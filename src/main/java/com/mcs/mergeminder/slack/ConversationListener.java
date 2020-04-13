package com.mcs.mergeminder.slack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.mcs.mergeminder.dao.MergeMinderDb;
import com.mcs.mergeminder.dto.MinderProjectsModel;
import com.mcs.mergeminder.dto.UserMappingModel;
import com.mcs.mergeminder.exception.ConversationException;
import com.mcs.mergeminder.properties.MergeMinderProperties;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

/**
 * This bean handles the conversation aspects of MergeMinder.
 */
@Component
public class ConversationListener implements SlackMessageSender {

	private static final Logger logger = LoggerFactory.getLogger(ConversationListener.class);
	private List<String> adminEmails = null;
	private Map<String, Conversation> activeConversations = null;

	private final MergeMinderDb mergeMinderDb;

	public ConversationListener(MergeMinderProperties mergeMinderProperties, MergeMinderDb mergeMinderDb) {
		this.adminEmails = mergeMinderProperties.getAdminEmails();
		this.mergeMinderDb = mergeMinderDb;
		activeConversations = new HashMap<>();
	}

	public List<String> getAdminEmails() {
		return adminEmails;
	}

	public void setAdminEmails(List<String> adminEmails) {
		this.adminEmails = adminEmails;
	}

	/**
	 * Handler method for incoming events.
	 *
	 * @param event
	 * @param session
	 */
	void handleIncomingEvent(SlackMessagePosted event, SlackSession session) {
		SlackUser messageSender = event.getSender();
		if (messageSender.getUserName().equalsIgnoreCase("mergeminder")) {
			// ignore events from yourself
			return;
		}
		SlackChannel channelOnWhichMessageWasPosted = event.getChannel();
		if (SlackChannel.SlackChannelType.INSTANT_MESSAGING == channelOnWhichMessageWasPosted.getType()) {
			handleDirectMessage(event, session);
		}
	}

	/**
	 * Responds to DM events.
	 *
	 * @param event
	 * @param session
	 */
	void handleDirectMessage(SlackMessagePosted event, SlackSession session) {
		SlackChannel channel = event.getChannel();
		String messageContent = event.getMessageContent();
		SlackUser messageSender = event.getSender();

		// Handle ongoing conversation
		Conversation conversation = activeConversations.get(messageSender.getId());
		if (conversation != null) {
			try {
				conversation.receiveNewInput(channel, messageSender, session, messageContent);
				if (conversation.isFinished()) {
					activeConversations.remove(messageSender.getId());
				}
			} catch (ConversationException e) {
				sendOops(channel, messageSender, session);
			}
			return;
		}

		// Admin Functionality
		///////////////////////

		// Admin Help message
		if (messageContent.toLowerCase().startsWith("admin help") || messageContent.toLowerCase().startsWith("help admin")) {
			if (!isUserAdmin(messageSender)) {
				simulateHumanStyleMessageSending(channel, "Sorry!  You must be an admin to perform this function.", session);
			} else {
				simulateHumanStyleMessageSending(channel, "The following commands are supported for administrators:", session);
				session.sendMessage(channel, "User Mapping Commands:");
				session.sendMessage(channel, " - set unmapped");
				session.sendMessage(channel, " - view mappings");
				session.sendMessage(channel, "Project Administration Commands:");
				session.sendMessage(channel, " - view projects [namespace]");
				session.sendMessage(channel, " - add project");
				session.sendMessage(channel, " - remove project");
			}
			logger.info("Received 'ADMIN HELP' request from user: {}", messageSender.getRealName());
			return;
		}

		// Set unmapped users message
		if (messageContent.toLowerCase().startsWith("set unmapped")) {
			if (!isUserAdmin(messageSender)) {
				simulateHumanStyleMessageSending(channel, "Sorry!  You must be an admin to perform this function.", session);
			} else {
				startSetUnmappedUserConversation(channel, messageSender, session);
			}
			logger.info("Received 'SET UNMAPPED USER' request from user: {}", messageSender.getRealName());
			return;
		}

		// Add project message
		if (messageContent.toLowerCase().startsWith("add project")) {
			if (!isUserAdmin(messageSender)) {
				simulateHumanStyleMessageSending(channel, "Sorry!  You must be an admin to perform this function.", session);
			} else {
				startAddProjectConversation(channel, messageSender, session);
			}
			logger.info("Received 'ADD PROJECT' request from user: {}", messageSender.getRealName());
			return;
		}

		// View mappings
		if (messageContent.toLowerCase().startsWith("view mappings")) {
			if (!isUserAdmin(messageSender)) {
				simulateHumanStyleMessageSending(channel, "Sorry!  You must be an admin to perform this function.", session);
			} else {
				viewMappings(channel, messageSender, session);
			}
			logger.info("Received 'VIEW MAPPINGS' request from user: {}", messageSender.getRealName());
			return;
		}

		// Non-Admin Functionality
		///////////////////////////

		if (messageContent.toLowerCase().startsWith("help")) {
			simulateHumanStyleMessageSending(channel, "The following slack commands are supported:", session);
			session.sendMessage(channel, "Project Related Commands:");
			session.sendMessage(channel, " - view projects [namespace]");
			logger.info("Received 'HELP' request from user: {}", messageSender.getRealName());
			return;
		}

		// View Projects
		if (messageContent.toLowerCase().startsWith("view projects")) {
			String parameterContent = messageContent.toLowerCase().substring("view projects".length());
			viewProjects(channel, messageSender, session, parameterContent != null && !parameterContent.trim().isBlank() ? parameterContent.trim() : null);

			logger.info("Received 'VIEW PROJECTS' request from user: {}", messageSender.getRealName());
			return;
		}

		if (messageContent.toLowerCase().startsWith("thank you") || messageContent.toLowerCase().startsWith("thanks")) {
			simulateHumanStyleMessageSending(channel, "You're welcome.", session);
			logger.info("Received 'THANKS' request from user: {}", messageSender.getRealName());
			return;
		}
		if (messageContent.toLowerCase().startsWith("hi") || messageContent.toLowerCase().startsWith("hello") || messageContent.toLowerCase().startsWith("hey")) {
			simulateHumanStyleMessageSending(channel, "Hello, " + messageSender.getRealName() + ".", session);
			logger.info("Received 'HELLO' request from user: {}", messageSender.getRealName());
			return;
		}
		if (messageContent.toLowerCase().contains("shit") || messageContent.toLowerCase().contains("piss") || messageContent.toLowerCase().contains("fuck")
			|| messageContent.toLowerCase().contains("bitch") || messageContent.toLowerCase().contains("crap") || messageContent.toLowerCase().contains("hell")) {
			simulateHumanStyleMessageSending(channel, "Don't talk to me that way!  I'm sure you wouldn't speak to your mother that way!", session);
			logger.info("Received 'POTTY MOUTH' request from user: {}", messageSender.getRealName());
			return;
		}

		simulateHumanStyleMessageSending(channel, "Hmm, I don't know quite what you are saying.  Sorry.", session);
		logger.info("Received 'UNKNOWN' request from user: {}", messageSender.getRealName());
		return;

	}

	/**
	 * Starts a SetUnmappedUserConversation
	 *
	 * @param channel
	 * @param messageSender
	 * @param session
	 */
	private void startSetUnmappedUserConversation(SlackChannel channel, SlackUser messageSender, SlackSession session) {
		Conversation conversation = new SetUnmappedUserConversation(mergeMinderDb);
		activeConversations.put(messageSender.getId(), conversation);

		try {
			conversation.start(channel, messageSender, session, null);
		} catch (ConversationException e) {
			sendOops(channel, messageSender, session);
		}
	}

	private void startAddProjectConversation(SlackChannel channel, SlackUser messageSender, SlackSession session) {
		Conversation conversation = new AddProjectConversation(mergeMinderDb);
		activeConversations.put(messageSender.getId(), conversation);

		try {
			conversation.start(channel, messageSender, session, null);
		} catch (ConversationException e) {
			sendOops(channel, messageSender, session);
		}
	}

	private void viewMappings(SlackChannel channel, SlackUser messageSender, SlackSession session) {
		try {
			List<UserMappingModel> userMappings = mergeMinderDb.getAllUserMappings();
			if (userMappings == null) {
				throw new ConversationException("Could not load user mappings.");
			}
			simulateHumanStyleMessageSending(channel, "Here are the users from Gitlab that have user mappings for their Slack accounts [username|realname -> slackUID|slackEmail]:", session);
			for (int i = 0; i < userMappings.size(); i++) {
				StringBuilder message = new StringBuilder();
				UserMappingModel model = userMappings.get(i);
				message.append("   ");
				message.append(i + 1);
				message.append(".  [ *");
				message.append(model.getGitlabUsername());
				message.append("*  |  *");
				message.append(model.getGitlabName());
				message.append("*  ->  *");
				message.append(model.getSlackUID() == null ? "[UNKNOWN]" : model.getSlackUID());
				message.append("*  |  *");
				message.append(model.getSlackEmail() == null ? "[UNKNOWN]" : model.getSlackEmail());
				message.append("* ]");
				session.sendMessage(channel, message.toString());
			}
		} catch (ConversationException e) {
			sendOops(channel, messageSender, session);
		}
	}

	private void viewProjects(SlackChannel channel, SlackUser messageSender, SlackSession session, String namespace) {
		try {
			List<MinderProjectsModel> projectList = (namespace == null)
				? mergeMinderDb.getMinderProjects() : mergeMinderDb.getMinderProjectsForNamespace(namespace);
			if (projectList == null) {
				throw new ConversationException("Could not load project list.");
			}
			if (projectList.isEmpty()) {
				simulateHumanStyleMessageSending(channel, "I could not find any Gitlab projects that have MergeMinding enabled"
					+ (namespace != null ? " for namespace " + namespace : "") + ":", session);
			} else {
				simulateHumanStyleMessageSending(channel, "Here are the Gitlab projects that have MergeMinding enabled"
					+ (namespace != null ? " for namespace " + namespace : "") + ":", session);
				StringBuilder message = new StringBuilder();
				for (int i = 0; i < projectList.size(); i++) {
					MinderProjectsModel model = projectList.get(i);
					message.append(i + 1);

					message.append(".\t[");
					message.append(model.getNamespace());
					message.append("/");
					message.append(model.getProject());
					message.append("]\n");
				}
				session.sendMessage(channel, message.toString());
			}
		} catch (ConversationException e) {
			sendOops(channel, messageSender, session);
		}
	}

	/**
	 * Checks if the user is a MergeMinder admin.
	 *
	 * @param slackUser the user to check
	 * @return true if user is a MergeMinder admin.  false otherwise.
	 */
	private boolean isUserAdmin(SlackUser slackUser) {
		if (slackUser == null || StringUtils.isEmpty(slackUser.getUserMail()) || CollectionUtils.isEmpty(adminEmails)) {
			return false;
		}
		return adminEmails.contains(slackUser.getUserMail().trim());
	}

	/**
	 * Generic Method to send an error to the user and clear their conversation state.
	 */
	private void sendOops(SlackChannel channel, SlackUser messageSender, SlackSession session) {
		// clear the active conversation
		activeConversations.remove(messageSender.getId());
		simulateHumanStyleMessageSending(channel, "Oops!  Something weird happened.  Sorry about that.  Please try again.", session);
	}

}
