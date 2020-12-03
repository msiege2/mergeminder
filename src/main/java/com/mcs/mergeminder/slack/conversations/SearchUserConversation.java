package com.mcs.mergeminder.slack.conversations;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mcs.mergeminder.dao.MergeMinderDb;
import com.mcs.mergeminder.exception.ConversationException;
import com.mcs.mergeminder.slack.BasicConversation;
import com.mcs.mergeminder.slack.ConversationType;
import com.mcs.mergeminder.slack.SlackApi;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;

public class SearchUserConversation extends BasicConversation {

	private static final Logger logger = LoggerFactory.getLogger(SearchUserConversation.class);

	SlackSession slackSession;

	public SearchUserConversation(MergeMinderDb mergeMinderDb) {
		super(mergeMinderDb, ConversationType.SEARCH_USER.toString());
	}

	@Override
	protected void showInitialMessage(SlackChannel channel, SlackUser messageSender, SlackSession session, SlackApi slackApi, String userInput) throws ConversationException {
		simulateHumanStyleMessageSending(channel, "Ok, what Slack user are you looking for?  Enter a name and I'll find what I can! (Type \"exit\" to cancel)", session, slackApi);
	}

	@Override
	protected boolean handleResponse(SlackChannel channel, SlackUser messageSender, SlackSession session, SlackApi slackApi, String userInput) throws ConversationException {
		if (userInput == null || userInput.isBlank()) {
			throw new ConversationException("Invalid input to search user command.");
		}
		simulateHumanStyleMessageSending(channel, "Possible user matches:", session, slackApi);
		String[] namePieces = userInput.split("\\s");
		Collection<SlackUser> slackUsers = session.getUsers();
		StringBuilder sb = new StringBuilder();
		for (SlackUser slackUser : slackUsers) {
			if (slackUser.isDeleted()) {
				continue;
			}
			String slackUserRealName = slackUser.getRealName();
			boolean matches = false;
			if (slackUserRealName == null) {
				continue;
			}
			for (String namePiece : namePieces) {
				if (slackUserRealName.toLowerCase().contains(namePiece.toLowerCase())) {
					matches = true;
					continue;
				}
			}
			if (matches) {
				sb.append("  Slack UID: " + slackUser.getId() + ", Name: " + slackUserRealName + "\n");
			}
		}
		slackApi.sendMessage(channel, sb.toString());
		return true;
	}
}
