package com.mcs.mergeminder;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
		logger.info("Current Eastern Time: {}", this.timeSchedule.currentEasternTime());
		if (!this.mergeMinderProperties.getScheduleBypass() && !this.timeSchedule.shouldAlertNow()) {
			logger.info("Skipping checks during off hours.");
			return;
		}
		doMinding();
		logger.info("MergeMinder checks complete.");
	}

	public void doMinding() {
		Instant start = Instant.now();

		List<MinderProjectsModel> projectList = this.mergeMinderDb.getMinderProjects();
		for (MinderProjectsModel project : projectList) {
			mindOneProject(project);
		}
		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toSeconds();
		logger.info("MergeMinding took {} seconds.", timeElapsed);
	}

	public void mindOneProject(MinderProjectsModel minderProject) {
		try {
			Collection<MergeRequestAssignmentInfo> assignmentInfoList = this.gitlabIntegration.getMergeRequestInfoForProject(minderProject.getNamespace(),
				minderProject.getProject());
			logger.info("Minding project [{}/{}].  Total of {} MRs to check.", minderProject.getNamespace(), minderProject.getProject(), assignmentInfoList.size());
			// 			for (MergeRequestAssignmentInfo mrInfo : assignmentInfoList) {
			AtomicInteger mrCheckCount = new AtomicInteger();
			assignmentInfoList.parallelStream().forEach((mrInfo) -> {
				if (mrInfo.getMr().getWorkInProgress()) {
					logger
						.info("[{}/{}] MR!{} assigned to {} is a WIP.  Ignoring.", minderProject.getNamespace(), minderProject.getProject(),
							mrInfo.getMr().getIid(), mrInfo.getAssignee().getUsername(),
							mrInfo.getAssignee().getName());
					mrCheckCount.getAndIncrement();
				} else {
					long hoursSinceLastAssignment = getHoursSinceAssignment(mrInfo.getAssignedAt());
					logger
						.info("[{}/{}] MR!{} has been assigned to {} ({}) for {} hours.", minderProject.getNamespace(), minderProject.getProject(),
							mrInfo.getMr().getIid(), mrInfo.getAssignee().getUsername(),
							mrInfo.getAssignee().getName(), hoursSinceLastAssignment);
					ReminderLength reminderLength = ReminderLength.getLastReminderPeriod(hoursSinceLastAssignment);
					long lastReminderAt = this.mergeMinderDb.getLastReminderSent(mrInfo.getMr().getId(), mrInfo.getLastAssignmentId());
					if (lastReminderAt >= reminderLength.getHours()) {
						logger.debug("[{}/{}] MR!{}: Already sent the most current reminder ({}).", minderProject.getNamespace(), minderProject.getProject(),
							mrInfo.getMr().getIid(), reminderLength);
					} else {
						this.slackIntegration.notifyMergeRequest(mrInfo, reminderLength, getEmail(mrInfo.getAssignee()));
					}
					this.mergeMinderDb.recordMergeRequest(mrInfo, hoursSinceLastAssignment);
					mrCheckCount.getAndIncrement();
				}
			});
			logger.info("Minding project [{}/{}] complete.  Total of {} MRs checked.", minderProject.getNamespace(), minderProject.getProject(), mrCheckCount.get());
		} catch (GitLabApiException e) {
			logger.error("Problem with GitLab integration.", e);
		}
	}

	/**
	 * Purge Process.  This removes stale MRs.
	 */
	@Scheduled(cron = "0 0 * * * *")
	public void mergePurge() {
		if (!this.mergeMinderProperties.getScheduleBypass() && this.timeSchedule.shouldPurgeNow()) {
			doPurge();
		}
	}

	public void doPurge() {
		logger.info("Running MergePurge.");
		int purgeCount = 0;
		List<MergeRequestModel> merges = this.mergeMinderDb.getAllMergeRequestModels();
		for (MergeRequestModel merge : merges) {
			try {
				if (merge.getLastUpdated().toInstant().isBefore(Instant.now().minus(2, ChronoUnit.DAYS)) &&
					this.gitlabIntegration.isMergeRequestMergedOrClosed(merge.getProject(), merge.getMrId())) {
					this.mergeMinderDb.removeMergeRequestModel(merge);
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
		if (!CollectionUtils.isEmpty(this.mergeMinderProperties.getEmailDomains())) {
			// email domains should be comma separated
			for (String emailDomain : this.mergeMinderProperties.getEmailDomains()) {
				if (user.getEmail() != null && user.getEmail().endsWith(emailDomain)) {
					return user.getEmail();
				}

			}
		}
		return null;
	}

}
