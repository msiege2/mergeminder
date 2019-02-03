package com.dst.mergeminder;

import javax.annotation.PostConstruct;

import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dst.mergeminder.dto.MergeRequestAssignmentInfo;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;

@Component
public class SlackIntegration {

	private static final Logger logger = LoggerFactory.getLogger(SlackIntegration.class);

	@Value("${slack.botToken}")
	private String slackToken;

	@Value("${slack.notificationChannel}")
	private String slackNotificationChannel;
	@Value("${slack.notifyUsers}")
	private boolean notifyUsers;
	private SlackSession slackSession;

	public SlackSession getSlackSession() {
		return slackSession;
	}

	@PostConstruct
	public void init() throws Exception {
		SlackSession session = SlackSessionFactory.createWebSocketSlackSession(slackToken);
		session.connect();
		this.slackSession = session;
	}

	public void notifyMergeRequest(MergeRequestAssignmentInfo mrInfo, ReminderLength reminderLength, String userEmail) {
		// Always notify the channel
		notifyChannelOfMergeInformation(mrInfo);
		if (notifyUsers && reminderLength.shouldSendAlert()) {
			notifyUser(mrInfo, reminderLength, userEmail);
		}
	}

	/**
	 * Send a message to the user about the merge request.
	 * @param mrInfo
	 * @param reminderLength
	 * @param userEmail
	 */
	private void notifyUser(MergeRequestAssignmentInfo mrInfo, ReminderLength reminderLength, String userEmail) {
		SlackUser user = slackSession.findUserByEmail(userEmail);
		if (user != null) {
			String messageForUser = null;
			if (!mrInfo.getAssignee().equals(mrInfo.getAuthor())) {
				// The assignee is not the author.  Send a regular note.
				messageForUser = reminderLength.getSlackPrivateNotificationMessage(getFirstName(mrInfo.getAssignee()),
					buildMRNameSection(mrInfo.getMr(), true),
					mrInfo.getFullyQualifiedProjectName(),
					mrInfo.getAuthor().getName());
			} else {
				if (reminderLength != ReminderLength.INITIAL_REMINDER) {
					// The MR is assigned to the author.  Remind them.
					messageForUser = reminderLength.getReminderForAuthor(getFirstName(mrInfo.getAssignee()),
						buildMRNameSection(mrInfo.getMr(), true),
						mrInfo.getFullyQualifiedProjectName());
				}
			}

			logger.info("Notifying user {} for [{}]{} at reminder time {}.", userEmail,
				mrInfo.getFullyQualifiedProjectName(),
				buildMRNameSection(mrInfo.getMr()),
				reminderLength);
			logger.debug("Notification message: {}", messageForUser);
			if (messageForUser != null) {
				slackSession.sendMessageToUser(user, messageForUser, null);
			}
		} else {
			logger.warn("Could not send user notification because user with email {} couldn't be located.", userEmail);
		}
	}

	/**
	 * Sends a message to the configured notification channel about the merge status.
	 * @param mrInfo
	 */
	private void notifyChannelOfMergeInformation(MergeRequestAssignmentInfo mrInfo) {
		StringBuilder sb = new StringBuilder();
		sb.append("[*").append(mrInfo.getFullyQualifiedProjectName()).append("*] ")
			.append(buildMRNameSection(mrInfo.getMr()))
			.append(" has been assigned to ")
			.append(mrInfo.getAssignee().getName()).append(" (@")
			.append(mrInfo.getAssignee().getUsername()).append(") for ")
			.append(MergeMinder.getHoursSinceAssignment(mrInfo.getAssignedAt()))
			.append(" hours.");

		SlackChannel channel = slackSession.findChannelByName(slackNotificationChannel);
		slackSession.sendMessage(channel, sb.toString());
	}

	/**
	 * Builds a section refering to the MR as "MR!xxxx" without a link.
	 * @param mr
	 * @return
	 */
	private String buildMRNameSection(MergeRequest mr) {
		return buildMRNameSection(mr, Boolean.FALSE);
	}

	/**
	 * Builds a section refering to the MR as "MR!xxxx" with optional link support.
	 * @param mr
	 * @param includeLink
	 * @return
	 */
	private String buildMRNameSection(MergeRequest mr, boolean includeLink) {
		StringBuilder sb = new StringBuilder();
		if (includeLink) {
			sb.append("<").append(mr.getWebUrl()).append("|")
				.append("MR!").append(mr.getIid()).append(">");
		} else {
			sb.append("MR!").append(mr.getIid());
		}
		return sb.toString();
	}

	/**
	 * Guesses the first name of the user.
	 * @param u
	 * @return
	 */
	private String getFirstName(User u) {
		return (u == null || u.getName() == null) ? "Anonymous" : u.getName().substring(0, u.getName().indexOf(" ")).trim();
	}
}
