package com.mcs.mergeminder.slack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mcs.mergeminder.exception.SlackIntegrationException;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.channels.ChannelsListRequest;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.conversations.ConversationsOpenRequest;
import com.slack.api.methods.response.channels.ChannelsListResponse;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.conversations.ConversationsOpenResponse;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackPreparedMessage;
import com.ullink.slack.simpleslackapi.SlackUser;

public class SlackApi {

	private static final Logger logger = LoggerFactory.getLogger(SlackApi.class);

	private Slack slack;

	private MethodsClient methods;
	/* Uncomment when rtm is used -- ie, typing event */
	//	private RTMClient rtm;

	/* Cache for IM channel IDs. */
	Map<String, String> directChannelIdCache;

	public SlackApi(String botToken) throws SlackIntegrationException {
		try {
			this.directChannelIdCache = new HashMap<>();
			this.slack = Slack.getInstance();

			// Initialize an API Methods client with the given token
			this.methods = this.slack.methods(botToken);

			/* Uncomment when RTM is to be used */
			//this.rtm = this.slack.rtmConnect(botToken);
		} catch (Exception e) {
			throw new SlackIntegrationException("Could not initialize SlackApi object.", e);
		}
	}

	/**
	 * Sends a message to a channel.  This method mixes the use of the legacy ullink SlackApi and the
	 * Slack provided Java API.  It is temporary to avoid the use of deprecated API methods in the ullink product.
	 *
	 * @param channelId
	 * @param message
	 */
	public ChatPostMessageResponse sendMessage(String channelId, SlackPreparedMessage message) {
		try {
			ChatPostMessageRequest request = ChatPostMessageRequest.builder()
				.channel(channelId)
				.text(message.getMessage())
				.unfurlLinks(message.isUnfurl())
				.linkNames(message.isLinkNames())
				.build();

			// Get a response as a Java object
			return this.methods.chatPostMessage(request);
		} catch (Exception e) {
			logger.error("Could not send message through slack!", e);
			return null;
		}
	}

	/**
	 * Sends a message to a channel.  This method mixes the use of the legacy ullink SlackApi and the
	 * Slack provided Java API.  It is temporary to avoid the use of deprecated API methods in the ullink product.
	 *
	 * @param channel
	 * @param message
	 * @return
	 */
	public ChatPostMessageResponse sendMessage(SlackChannel channel, String message) {
		SlackPreparedMessage preparedMessage = SlackPreparedMessage.builder()
			.message(message)
			.unfurl(Boolean.FALSE)
			.build();

		return sendMessage(channel.getId(), preparedMessage);
	}

	public ChatPostMessageResponse sendMessage(String channelId, String message) {
		SlackPreparedMessage preparedMessage = SlackPreparedMessage.builder()
			.message(message)
			.unfurl(Boolean.FALSE)
			.build();

		return sendMessage(channelId, preparedMessage);
	}

	public ChatPostMessageResponse sendMessage(SlackChannel channel, SlackPreparedMessage message) {
		return sendMessage(channel.getId(), message);
	}

	/**
	 * Sends a Direct IM to a user.  This method mixes the use of the legacy ullink SlackApi and the
	 * Slack provided Java API.  It is temporary to avoid the use of deprecated API methods in the ullink product.
	 *
	 * @param user
	 * @param messageContent
	 */
	public ChatPostMessageResponse sendMessageToUser(SlackUser user, SlackPreparedMessage messageContent) {
		try {
			// In order to send the message, we need to do a few things.  We need a channel ID for the direct message,
			// which should be in the format "D#######", and we need the message to send.

			// Step 1: Get the channel ID.
			String directChannelId = getDirectMessageChannelId(user.getId());

			// Step 2: Create a chat.postMessage request
			ChatPostMessageRequest request = ChatPostMessageRequest.builder()
				.channel(directChannelId) // Use a channel ID `C1234567` is preferrable
				.text(messageContent.getMessage())
				.unfurlLinks(messageContent.isUnfurl())
				.linkNames(messageContent.isLinkNames())
				.build();

			// Step 3: Send the message, get the response.
			ChatPostMessageResponse response = this.methods.chatPostMessage(request);

			//TODO: Handle the response -- look for a failure?

			return response;
		} catch (Exception e) {
			logger.error("Could not send direct message through slack!", e);
			return null;
		}
	}

	public void listChannels() {
		try {
			ChannelsListRequest request = ChannelsListRequest.builder().build();

			// Step 3: Send the message, get the response.
			ChannelsListResponse response = this.methods.channelsList(request);

			//TODO: Handle the response -- look for a failure?

			System.out.println(response.getResponseMetadata());
		} catch (Exception e) {
			logger.error("Could not send direct message through slack!", e);
		}
	}
	// Private Methods
	///////////////////

	/**
	 * Gets a direct channel ID between MergeMinder and the given Slack user (U#######)
	 *
	 * @param userId User ID in format U#######
	 * @return channel ID in format D#######
	 */
	private String getDirectMessageChannelId(String userId) throws SlackIntegrationException {
		try {
			String directChannelId = this.directChannelIdCache.get(userId);
			if (directChannelId == null) {
				ConversationsOpenRequest request = ConversationsOpenRequest.builder()
					.users(Collections.singletonList(userId))
					.build();

				// Get a response as a Java object
				ConversationsOpenResponse response = this.methods.conversationsOpen(request);

				directChannelId = response.getChannel().getId();
				this.directChannelIdCache.put(userId, directChannelId);
			}
			return directChannelId;
		} catch (Exception e) {
			throw new SlackIntegrationException("Could not find direct channel ID for message.", e);
		}
	}

}
