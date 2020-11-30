package com.mcs.mergeminder.slack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.mcs.mergeminder.MergeMinder;
import com.mcs.mergeminder.ReminderLength;
import com.mcs.mergeminder.dao.MergeMinderDb;
import com.mcs.mergeminder.dto.MergeRequestAssignmentInfo;
import com.mcs.mergeminder.dto.SlackUserModel;
import com.mcs.mergeminder.dto.SlackUserSearchCriteria;
import com.mcs.mergeminder.dto.UserMappingModel;
import com.mcs.mergeminder.properties.MergeMinderProperties;
import com.mcs.mergeminder.properties.SlackProperties;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackPreparedMessage;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;

@Component
public class SlackIntegration {

	private static final Logger logger = LoggerFactory.getLogger(SlackIntegration.class);

	private final MergeMinderDb mergeMinderDb;
	private final ConversationListener conversationListener;

	/**
	 * Object to store the properties related to slack behavior
	 **/
	private final MergeMinderProperties mergeMinderProperties;
	private final SlackProperties slackProperties;
	private SlackSession slackSession;
	private SlackApi slackApi;

	public SlackIntegration(MergeMinderDb mergeMinderDb, ConversationListener conversationListener, MergeMinderProperties mergeMinderProperties, SlackProperties slackProperties) {
		this.mergeMinderDb = mergeMinderDb;
		this.conversationListener = conversationListener;
		this.mergeMinderProperties = mergeMinderProperties;
		this.slackProperties = slackProperties;
	}

	@PostConstruct
	public void init() throws Exception {
		SlackSession session = SlackSessionFactory
			.getSlackSessionBuilder(this.slackProperties.getBotToken())
			.withAutoreconnectOnDisconnection(true)
			.withConnectionHeartbeat(15, TimeUnit.SECONDS)
			.build();
		session.connect();
		this.slackSession = session;
		this.registerListener();
		logger.info("Slack connection created.  Notification channel is {}.  User notification is {}",
			this.slackProperties.getNotificationChannel() != null ? "ENABLED on channel #" + this.slackProperties.getNotificationChannel() : "DISABLED",
			this.slackProperties.getNotifyUsers() ? "ENABLED" : "DISABLED");
		logger.info("Admin users: {}", StringUtils.collectionToCommaDelimitedString(this.mergeMinderProperties.getAdminEmails()));

		SlackApi slackApi = new SlackApi(this.slackProperties.getBotToken());
		this.slackApi = slackApi;
	}

	public void registerListener() {
		logger.info("Registering Slack message listener for conversational functionality.");
		// first define the listener
		SlackMessagePostedListener messagePostedListener = (event, session) -> this.conversationListener.handleIncomingEvent(event, session, this.slackApi);
		//add it to the session
		this.slackSession.addMessagePostedListener(messagePostedListener);
		logger.info("Message listener registration complete.");
	}

	public SlackSession getSlackSession() {
		return this.slackSession;
	}

	public SlackApi getSlackApi() {
		return this.slackApi;
	}

	/**
	 * Create notification(s) for this MR.
	 *
	 * @param mrInfo
	 * @param reminderLength
	 * @param userEmail
	 */
	public void notifyMergeRequest(MergeRequestAssignmentInfo mrInfo, ReminderLength reminderLength, String userEmail) {
		// Always notify the channel
		if (this.slackProperties.getNotificationChannel() != null) {
			notifyChannelOfMergeInformation(mrInfo);
		}
		if (this.slackProperties.getNotifyUsers() && reminderLength.shouldSendAlert()) {
			notifyUser(mrInfo, reminderLength, userEmail);
		}
	}

	/**
	 * Send a message to the user about the merge request.
	 *
	 * @param mrInfo
	 * @param reminderLength
	 * @param userEmail
	 */
	private void notifyUser(MergeRequestAssignmentInfo mrInfo, ReminderLength reminderLength, String userEmail) {
		// first check the mapping table
		UserMappingModel userMapping = this.mergeMinderDb.getUserMappingByGitlabUsername(mrInfo.getAssignee().getUsername());
		SlackUser user = findUserFromPredefinedMapping(userMapping);
		// then try email lookup
		if (user == null) {
			if (userEmail != null) {
				// if we found the user's email in gitlab, use that...
				user = this.slackSession.findUserByEmail(userEmail);
			} else {
				// otherwise guess their slack email from realname + emaildomains
				List<String> potentialEmails = guessEmails(mrInfo.getAssignee());
				for (String potentialUserEmail : potentialEmails) {
					user = this.slackSession.findUserByEmail(potentialUserEmail);
					if (user != null) {
						break;
					}
				}
			}
		}
		// at this point, we are either fuzzy matching, or not matching at all.  either way, record this in the mapping table for resolution later.
		if (user == null) {
			if (userMapping == null) {
				recordUserMapping(mrInfo.getAssignee());
			}
			// now try a fuzzy match
			user = findUserTheHardWay(mrInfo.getAssignee());
		}
		// if we found the user, notify.  if not, just give up
		if (user != null) {
			String messageForUser = null;
			if (!mrInfo.getAssignee().getId().equals(mrInfo.getAuthor().getId())) {
				// The assignee is not the author.  Send a regular note.
				messageForUser = reminderLength.getSlackPrivateNotificationMessage(getFirstName(mrInfo.getAssignee()),
					buildMRNameSection(mrInfo.getMr()),
					buildMRTitleSection(mrInfo.getMr()),
					mrInfo.getFullyQualifiedProjectName(),
					mrInfo.getAuthor().getName());
				logger.info("Notifying user {} for [{}]{} at reminder time {}.", mrInfo.getAssignee().getName(),
					mrInfo.getFullyQualifiedProjectName(),
					buildMRNameSection(mrInfo.getMr(), Boolean.TRUE),
					reminderLength);
			} else {
				// The MR is assigned to the author.  Remind them.  Skip the first reminder if it is brand new.
				if (mrInfo.getLastAssignmentId() <= 0 && reminderLength == ReminderLength.INITIAL_REMINDER) {
					logger.info("Skipping author assignment message at reminder time {}.", reminderLength);
				} else {
					messageForUser = reminderLength.getReminderForAuthor(getFirstName(mrInfo.getAssignee()),
						buildMRNameSection(mrInfo.getMr()),
						buildMRTitleSection(mrInfo.getMr()),
						mrInfo.getFullyQualifiedProjectName());
					logger.info("Notifying user {} for [{}]{} with author assignment message at reminder time {}.", mrInfo.getAssignee().getName(),
						mrInfo.getFullyQualifiedProjectName(),
						buildMRNameSection(mrInfo.getMr(), Boolean.TRUE),
						reminderLength);
				}

			}
			logger.debug("Notification message: {}", messageForUser);
			if (messageForUser != null) {
				SlackPreparedMessage slackPreparedMessage = new SlackPreparedMessage.Builder()
					.withMessage(messageForUser)
					.withUnfurl(false)
					.build();
				this.slackApi.sendMessageToUser(user, slackPreparedMessage);
			}
		} else {
			logger.warn("Could not send user notification because user with email {} couldn't be located.", userEmail);
		}
	}

	/**
	 * Save a user mapping
	 *
	 * @param user
	 */
	private void recordUserMapping(User user) {
		logger.info("Recording user in UserMapping lookup table.");
		UserMappingModel userMapping = new UserMappingModel(user.getUsername());
		userMapping.setGitlabName(user.getName());
		this.mergeMinderDb.saveUserMapping(userMapping);
	}

	/**
	 * Finds a slack user based on the data in the UserMapping table.
	 *
	 * @param userMapping
	 * @return
	 */
	private SlackUser findUserFromPredefinedMapping(UserMappingModel userMapping) {
		if (userMapping == null) {
			return null;
		}
		SlackUser slackUser = null;
		if (userMapping.getSlackUID() != null) {
			slackUser = this.slackSession.findUserById(userMapping.getSlackUID());
		}
		if (slackUser == null && userMapping.getSlackEmail() != null) {
			slackUser = this.slackSession.findUserByEmail(userMapping.getSlackEmail());
		}
		return slackUser;
	}

	/**
	 * Sends a message to the configured notification channel about the merge status.
	 *
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

		SlackPreparedMessage preparedMessage = new SlackPreparedMessage.Builder()
			.withMessage(sb.toString())
			.withUnfurl(false)
			.build();
		SlackChannel channel = this.slackSession.findChannelByName(this.slackProperties.getNotificationChannel());
		if (channel != null) {
			this.slackApi.sendMessage(channel, preparedMessage);
		} else {
			logger.warn("Could not send notifications to slack channel #{}", this.slackProperties.getNotificationChannel());
		}
	}

	/**
	 * Builds a section refering to the MR as "MR!xxxx" without a link.
	 *
	 * @param mr
	 * @return
	 */
	private String buildMRNameSection(MergeRequest mr) {
		return buildMRNameSection(mr, Boolean.FALSE);
	}

	/**
	 * Builds a section refering to the MR as "MR!xxxx" with optional link support.
	 *
	 * @param mr
	 * @param suppressLink
	 * @return
	 */
	private String buildMRNameSection(MergeRequest mr, boolean suppressLink) {
		StringBuilder sb = new StringBuilder();
		if (!suppressLink) {
			sb.append("<").append(mr.getWebUrl()).append("|")
				.append("MR!").append(mr.getIid()).append(">");
		} else {
			sb.append("MR!").append(mr.getIid());
		}
		return sb.toString();
	}

	private String buildMRTitleSection(MergeRequest mr) {
		if (!StringUtils.hasLength(mr.getTitle())) {
			return "{NO TITLE}";
		}
		String[] lines = mr.getTitle().split("\\n");
		return lines[0];
	}

	/**
	 * Ugh.  Brute force match parts of the user's gitlab name to their slack name.
	 *
	 * @param u
	 * @return
	 */
	private SlackUser findUserTheHardWay(User u) {
		Collection<SlackUser> slackUsers = this.slackSession.getUsers();
		for (SlackUser slackUser : slackUsers) {
			String[] namePieces = u.getName().split("\\s");
			String slackUserRealName = slackUser.getRealName();
			boolean matches = true;
			for (String namePiece : namePieces) {
				if (slackUserRealName == null || !slackUserRealName.toLowerCase().contains(namePiece.toLowerCase())) {
					matches = false;
					break;
				}
			}
			if (matches) {
				logger.info("I have 'fuzzy matched' Gitlab user @{} ({}) to Slack user \"{}\".", u.getUsername(), u.getName(), slackUserRealName);
				return slackUser;
			}
		}
		logger.warn("Could not figure out who the GitLab user @{} ({}) is.", u.getUsername(), u.getName());
		return null;
	}

	public List<SlackUserModel> searchSlackUsers(SlackUserSearchCriteria searchCriteria) {
		Collection<SlackUser> slackUsers = this.slackSession.getUsers();
		List<SlackUserModel> matchingUsers = new LinkedList<>();
		for (SlackUser slackUser : slackUsers) {
			if (StringUtils.hasLength(searchCriteria.getId())) {
				if (!StringUtils.hasLength(slackUser.getId()) || !slackUser.getId().toLowerCase().contains(searchCriteria.getId().toLowerCase())) {
					continue;
				}
			}
			if (StringUtils.hasLength(searchCriteria.getRealName())) {
				if (!StringUtils.hasLength(slackUser.getRealName()) || !slackUser.getRealName().toLowerCase().contains(searchCriteria.getRealName().toLowerCase())) {
					continue;
				}
			}
			if (StringUtils.hasLength(searchCriteria.getUsername())) {
				if (!StringUtils.hasLength(slackUser.getUserName()) || !slackUser.getUserName().toLowerCase().contains(searchCriteria.getUsername().toLowerCase())) {
					continue;
				}
			}
			if (StringUtils.hasLength(searchCriteria.getEmail())) {
				if (!StringUtils.hasLength(slackUser.getUserMail()) || !slackUser.getUserMail().toLowerCase().contains(searchCriteria.getEmail().toLowerCase())) {
					continue;
				}
			}
			matchingUsers.add(new SlackUserModel(slackUser));
		}
		return matchingUsers;
	}

	/**
	 * Guesses the first name of the user.
	 *
	 * @param u
	 * @return
	 */
	private String getFirstName(User u) {
		return (u == null || u.getName() == null) ? "Anonymous" : (u.getName() + " ").substring(0, (u.getName() + " ").indexOf(" ")).trim();
	}

	private List<String> guessEmails(User user) {
		if (user == null || CollectionUtils.isEmpty(this.mergeMinderProperties.getEmailDomains())) {
			return Collections.emptyList();
		}
		List<String> emailGuesses = new ArrayList<>();
		// email domains should be comma separated
		for (String emailDomain : this.mergeMinderProperties.getEmailDomains()) {
			emailGuesses.add(user.getName().toLowerCase().replaceAll("\\s", ".") + "@" + emailDomain);
		}
		return emailGuesses;
	}

}
