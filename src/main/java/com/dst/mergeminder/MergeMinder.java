package com.dst.mergeminder;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.dst.mergeminder.dao.MergeMinderDb;
import com.dst.mergeminder.dto.MergeRequestAssignmentInfo;
import com.dst.mergeminder.dto.MergeRequestModel;
import com.dst.mergeminder.dto.MinderProjectsModel;
import com.dst.mergeminder.gitlab.GitlabIntegration;
import com.dst.mergeminder.slack.SlackIntegration;
import com.dst.mergeminder.util.TimeSchedule;

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

	@Autowired
	TimeSchedule timeSchedule;
	@Autowired
	MergeMinderDb mergeMinderDb;
	@Autowired
	SlackIntegration slackIntegration;
	@Autowired
	GitlabIntegration gitlabIntegration;

	@Value("${mergeminder.schedule.bypass:false}")
	private boolean bypassSchedule;

	@Value("${mergeminder.emailDomains}")
	private String emailDomains;

	public MergeMinder() {
		// empty constructor
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
		if (!bypassSchedule && !timeSchedule.shouldAlertNow()) {
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
		if (!bypassSchedule && timeSchedule.shouldPurgeNow()) {
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
			} catch (GitLabApiException e) {
				logger.error("Problem with GitLab integration.", e);
			}
		}
		logger.info("MergePurge complete.  Removed {} entries.", purgeCount);
	}

	/**
	 * Returns a long of hours since the last assignment occurred.
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
	 * @param mrAssignedAt Instant at whihc the MR was last assigned.
	 * @return
	 */
	public static long getHoursSinceAssignment(Instant mrAssignedAt) {
		return ChronoUnit.HOURS.between(mrAssignedAt, Instant.now());
	}

	/**
	 * Converts a user into the best guess email.  First checks the user object for a
	 * specified email, then guesses it based upon <tt>fname.lname@emaildomain</tt>
	 * @param user
	 * @return
	 */
	String getEmail(User user) {
		if (user == null) {
			return null;
		}
		if (emailDomains != null) {
			// email domains should be comma separated
			String[] splitEmailDomains = emailDomains.split(",");
			for (String emailDomain : splitEmailDomains) {
				if (user.getEmail() != null && user.getEmail().endsWith(emailDomain)) {
					return user.getEmail();
				}

			}
		}
		return null;
	}

}
