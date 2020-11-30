package com.mcs.mergeminder.slack;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.conversations.ConversationsOpenRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.conversations.ConversationsOpenResponse;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackPreparedMessage;
import com.ullink.slack.simpleslackapi.SlackUser;

public class SlackApi {

	private Slack slack;

	private MethodsClient methods;
	//	private RTMClient rtm;

	Map<String, String> directChannelIdCache;

	public SlackApi(String botToken) {
		try {
			this.directChannelIdCache = new HashMap<>();
			this.slack = Slack.getInstance();

			// Initialize an API Methods client with the given token
			this.methods = this.slack.methods(botToken);
			//this.rtm = this.slack.rtmConnect(botToken);
		} catch (Exception e) {

		}
	}

	/**
	 * Sends a message to a channel.  This method mixes the use of the legacy ullink SlackApi and the
	 * Slack provided Java API.  It is temporary to avoid the use of deprecated API methods in the ullink product.
	 *
	 * @param channel
	 * @param messageContent
	 */
	public ChatPostMessageResponse sendMessage(SlackChannel channel, SlackPreparedMessage messageContent) {
		ChatPostMessageRequest request = ChatPostMessageRequest.builder()
			.channel(channel.getId())
			.text(messageContent.getMessage())
			.unfurlLinks(messageContent.isUnfurl())
			.linkNames(messageContent.isLinkNames())
			.build();
		// Get a response as a Java object
		try {
			return this.methods.chatPostMessage(request);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SlackApiException e) {
			e.printStackTrace();
		}
		return null;
	}

	public ChatPostMessageResponse sendMessage(SlackChannel channel, String message) {
		SlackPreparedMessage preparedMessage = new SlackPreparedMessage.Builder()
			.withMessage(message)
			.withUnfurl(Boolean.FALSE)
			.build();

		return sendMessage(channel, preparedMessage);
	}

	/**
	 * Sends a Direct IM to a user.  This method mixes the use of the legacy ullink SlackApi and the
	 * Slack provided Java API.  It is temporary to avoid the use of deprecated API methods in the ullink product.
	 *
	 * @param user
	 * @param messageContent
	 */
	public void sendMessageToUser(SlackUser user, SlackPreparedMessage messageContent) {
		// In order to send the message, we need to do a few things.  We need a channel ID for the direct message,
		// which should be in the format "D#######", and we need the message to send.  Then we'll send the message over the
		// websocket RTM connection.

		// Step 1: Get the channel ID.
		String directChannelId = getDirectMessageChannelId(user.getId());

		// Send messages over a WebSocket connection
		//		String channelId = "C1234567";
		//		String message = Message.builder().id(1234567L).channel(channelId).text(":wave: Hi there!").build().toJSONString();
		//		this.rtm.sendMessage(message);

		ChatPostMessageRequest request = ChatPostMessageRequest.builder()
			.channel(directChannelId) // Use a channel ID `C1234567` is preferrable
			.text(messageContent.getMessage())
			.unfurlLinks(messageContent.isUnfurl())
			.linkNames(messageContent.isLinkNames())
			.build();
		// Get a response as a Java object
		try {
			ChatPostMessageResponse response = this.methods.chatPostMessage(request);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SlackApiException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Gets a direct channel ID between MergeMinder and the given Slack user (U#######)
	 *
	 * @param userId User ID in format U#######
	 * @return channel ID in format D#######
	 */
	private String getDirectMessageChannelId(String userId) {
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
			// TODO : improve exception handling
			e.printStackTrace();
		}
		return null;
	}

}
