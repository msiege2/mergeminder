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
import com.mcs.mergeminder.exception.ConversationException;
import com.mcs.mergeminder.properties.MergeMinderProperties;
import com.mcs.mergeminder.slack.conversations.AddProjectConversation;
import com.mcs.mergeminder.slack.conversations.SearchUserConversation;
import com.mcs.mergeminder.slack.conversations.SetUnmappedUserConversation;
import com.mcs.mergeminder.slack.conversations.ViewMappingsConversation;
import com.mcs.mergeminder.slack.conversations.ViewProjectsConversation;
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
				session.sendMessage(channel, " - search users");
				session.sendMessage(channel, "Project Administration Commands:");
				session.sendMessage(channel, " - view projects [namespace]");
				session.sendMessage(channel, " - add project");
				session.sendMessage(channel, " - remove project");
			}
			logger.info("Received 'ADMIN HELP' request from user: {}", messageSender.getRealName());
			return;
		}

		// Set unmapped users
		if (messageContent.toLowerCase().startsWith("set unmapped")) {
			converse(ConversationType.SET_UNMAPPED_USER, true, channel, messageSender, session, null);
			return;
		}
		// Add project
		if (messageContent.toLowerCase().startsWith("add project")) {
			converse(ConversationType.ADD_PROJECT, true, channel, messageSender, session, null);
			return;
		}
		// Search users
		if (messageContent.toLowerCase().startsWith("search user")) {
			converse(ConversationType.SEARCH_USER, true, channel, messageSender, session, null);
			return;
		}
		// View mappings
		if (messageContent.toLowerCase().startsWith("view mappings")) {
			converse(ConversationType.VIEW_MAPPINGS, true, channel, messageSender, session, null);
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
			// any parameter content should represent namespace
			String parameterContent = messageContent.toLowerCase().substring("view projects".length());
			if (org.apache.commons.lang3.StringUtils.isBlank(parameterContent)) {
				parameterContent = null;
			}
			converse(ConversationType.VIEW_PROJECTS, false, channel, messageSender, session, parameterContent);
			return;
		}

		// Other wacky responses
		////////////////////////
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

		//if we get this far, we don't know what to do with the input
		sendUnknownInputResponse(channel, messageSender, session);
	}

	private void converse(ConversationType conversationType, boolean requiresAdmin, SlackChannel channel,
		SlackUser messageSender, SlackSession session, String userInput) {
		if (conversationType == null) {
			return;
		}

		logger.info("Received {} request from user: {}", conversationType, messageSender.getRealName());

		if (requiresAdmin && !isUserAdmin(messageSender)) {
			simulateHumanStyleMessageSending(channel, "Sorry!  You must be an admin to perform this function.", session);
			logger.warn("{} request denied because user is not an admin: {}", conversationType, messageSender.getRealName());
		} else {
			// Start a conversation!
			try {
				Conversation c = null;
				switch (conversationType) {
					case SET_UNMAPPED_USER:
						c = new SetUnmappedUserConversation(mergeMinderDb);
						break;
					case ADD_PROJECT:
						c = new AddProjectConversation(mergeMinderDb);
						break;
					case SEARCH_USER:
						c = new SearchUserConversation(mergeMinderDb);
						break;
					case VIEW_PROJECTS:
						c = new ViewProjectsConversation(mergeMinderDb);
						break;
					case VIEW_MAPPINGS:
						c = new ViewMappingsConversation(mergeMinderDb);
						break;
					default:
						sendUnknownInputResponse(channel, messageSender, session);
				}
				if (c != null) {
					initializeConversation(c, channel, messageSender, session, userInput);
				}
			} catch (ConversationException e) {
				sendOops(channel, messageSender, session);
			}
		}
	}

	private void sendUnknownInputResponse(SlackChannel channel, SlackUser messageSender, SlackSession session) {
		simulateHumanStyleMessageSending(channel, "Hmm, I don't know quite what you are saying.  Sorry.", session);
		logger.info("Received 'UNKNOWN' request from user: {}", messageSender.getRealName());
	}

	private void initializeConversation(Conversation c, SlackChannel channel, SlackUser messageSender, SlackSession session) throws ConversationException {
		initializeConversation(c, channel, messageSender, session, null);
	}

	private void initializeConversation(Conversation c, SlackChannel channel, SlackUser messageSender, SlackSession session, String initialInput) throws ConversationException {
		if (c == null) {
			return;
		}
		activeConversations.put(messageSender.getId(), c);
		c.start(channel, messageSender, session, initialInput);
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
