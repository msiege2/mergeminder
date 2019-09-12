package com.mcs.mergeminder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

import com.mcs.mergeminder.dao.MergeMinderDb;
import com.mcs.mergeminder.dto.MergeRequestAssignmentInfo;
import com.mcs.mergeminder.dto.MergeRequestModel;
import com.mcs.mergeminder.dto.MinderProjectsModel;
import com.mcs.mergeminder.exception.GitlabIntegrationException;
import com.mcs.mergeminder.gitlab.GitlabIntegration;
import com.mcs.mergeminder.properties.MergeMinderProperties;
import com.mcs.mergeminder.slack.SlackIntegration;
import com.mcs.mergeminder.util.TimeSchedule;

/**
 * MergeMinderBot!
 *
 * @author Matt Siegel
 * @version 1.0.0, 02/03/2019
 */
@Configuration
@EnableScheduling
public class MergeMinder {

	private static final Logger logger = LoggerFactory.getLogger(MergeMinder.class);

	private final TimeSchedule timeSchedule;
	private final MergeMinderDb mergeMinderDb;
	private final SlackIntegration slackIntegration;
	private final GitlabIntegration gitlabIntegration;
	private final MergeMinderProperties mergeMinderProperties;

	public MergeMinder(TimeSchedule timeSchedule, MergeMinderDb mergeMinderDb, SlackIntegration slackIntegration,
		GitlabIntegration gitlabIntegration, MergeMinderProperties mergeMinderProperties) {
		this.timeSchedule = timeSchedule;
		this.mergeMinderDb = mergeMinderDb;
		this.slackIntegration = slackIntegration;
		this.gitlabIntegration = gitlabIntegration;
		this.mergeMinderProperties = mergeMinderProperties;
	}

	@PostConstruct
	public void init() {
		logger.info("Starting MergeMinder");
	}

	/**
	 * Main application.  This task runs every 5 minutes.
	 */
	@Scheduled(cron = "0 0/5 * * * *")
	public void mindMerges() {
		logger.info("Running MergeMinder checks.");
		logger.info("Current Eastern Time: {}", timeSchedule.currentEasternTime());
		if (!mergeMinderProperties.getScheduleBypass() && !timeSchedule.shouldAlertNow()) {
			logger.info("Skipping checks during off hours.");
			return;
		}
		doMinding();
	}

	public void doMinding() {
		List<MinderProjectsModel> projectList = mergeMinderDb.getMinderProjects();
		for (MinderProjectsModel minderProject : projectList) {
			try {
				Collection<MergeRequestAssignmentInfo> assignmentInfoList = gitlabIntegration.getMergeRequestInfoForProject(minderProject.getNamespace(),
					minderProject.getProject());
				for (MergeRequestAssignmentInfo mrInfo : assignmentInfoList) {
					if (mrInfo.getMr().getWorkInProgress()) {
						// skip WIP for now.
						continue;
					}
					long hoursSinceLastAssignment = getHoursSinceAssignment(mrInfo.getAssignedAt());
					logger
						.info("[{}/{}] MR!{} has been assigned to {} ({}) for {} hours.", minderProject.getNamespace(), minderProject.getProject(),
							mrInfo.getMr().getIid(), mrInfo.getAssignee().getUsername(),
							mrInfo.getAssignee().getName(), hoursSinceLastAssignment);
					ReminderLength reminderLength = ReminderLength.getLastReminderPeriod(hoursSinceLastAssignment);
					long lastReminderAt = mergeMinderDb.getLastReminderSent(mrInfo.getMr().getId(), mrInfo.getLastAssignmentId());
					if (lastReminderAt >= reminderLength.getHours()) {
						logger.debug("[{}/{}] MR!{}: Already sent the most current reminder ({}).", minderProject.getNamespace(), minderProject.getProject(),
							mrInfo.getMr().getIid(), reminderLength);
					} else {
						slackIntegration.notifyMergeRequest(mrInfo, reminderLength, getEmail(mrInfo.getAssignee()));
					}
					mergeMinderDb.recordMergeRequest(mrInfo, hoursSinceLastAssignment);
				}

			} catch (GitLabApiException e) {
				logger.error("Problem with GitLab integration.", e);
			}
		}
	}

	/**
	 * Purge Process.  This removes stale MRs.
	 */
	@Scheduled(cron = "0 0 * * * *")
	public void mergePurge() {
		if (!mergeMinderProperties.getScheduleBypass() && timeSchedule.shouldPurgeNow()) {
			doPurge();
		}
	}

	public void doPurge() {
		logger.info("Running MergePurge.");
		int purgeCount = 0;
		List<MergeRequestModel> merges = mergeMinderDb.getAllMergeRequestModels();
		for (MergeRequestModel merge : merges) {
			try {
				if (merge.getLastUpdated().toInstant().isBefore(Instant.now().minus(2, ChronoUnit.DAYS)) &&
					gitlabIntegration.isMergeRequestMergedOrClosed(merge.getProject(), merge.getMrId())) {
					mergeMinderDb.removeMergeRequestModel(merge);
					purgeCount++;
				}
			} catch (GitlabIntegrationException e) {
				logger.error("Problem with GitLab integration.", e);
			}
		}
		logger.info("MergePurge complete.  Removed {} entries.", purgeCount);
	}

	/**
	 * Returns a long of hours since the last assignment occurred.
	 *
	 * @param mrAssignedAt the date at which the MR was last assigned.
	 * @return
	 */
	public static long getHoursSinceAssignment(Date mrAssignedAt) {
		if (mrAssignedAt == null) {
			return -1;
		}
		return getHoursSinceAssignment(mrAssignedAt.toInstant());
	}

	/**
	 * Returns a long of hours since the last assignment occurred.
	 *
	 * @param mrAssignedAt Instant at whihc the MR was last assigned.
	 * @return
	 */
	public static long getHoursSinceAssignment(Instant mrAssignedAt) {
		return ChronoUnit.HOURS.between(mrAssignedAt, Instant.now());
	}

	/**
	 * Converts a user into the best guess email.  First checks the user object for a
	 * specified email, then guesses it based upon <tt>fname.lname@emaildomain</tt>
	 *
	 * @param user
	 * @return
	 */
	String getEmail(User user) {
		if (user == null) {
			return null;
		}
		if (!CollectionUtils.isEmpty(mergeMinderProperties.getEmailDomains())) {
			// email domains should be comma separated
			for (String emailDomain : mergeMinderProperties.getEmailDomains()) {
				if (user.getEmail() != null && user.getEmail().endsWith(emailDomain)) {
					return user.getEmail();
				}

			}
		}
		return null;
	}

}
