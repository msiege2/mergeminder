package com.mcs.mergeminder.slack.conversations;

import java.util.List;

import com.mcs.mergeminder.dao.MergeMinderDb;
import com.mcs.mergeminder.dto.UserMappingModel;
import com.mcs.mergeminder.exception.ConversationException;
import com.mcs.mergeminder.slack.ConversationType;
import com.mcs.mergeminder.slack.SingleResponseConversation;
import com.mcs.mergeminder.slack.SlackApi;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;

public class ViewMappingsConversation extends SingleResponseConversation {

	public ViewMappingsConversation(MergeMinderDb mergeMinderDb) {
		super(mergeMinderDb, ConversationType.VIEW_MAPPINGS.toString());
	}

	@Override
	protected void handleResponse(SlackChannel channel, SlackUser messageSender, SlackSession session, SlackApi slackApi, String userInput) throws ConversationException {
		List<UserMappingModel> userMappings = this.mergeMinderDb.getAllUserMappings();
		if (userMappings == null) {
			throw new ConversationException("Could not load user mappings.");
		}
		simulateHumanStyleMessageSending(channel, "Here are the users from Gitlab that have user mappings for their Slack accounts [username|realname -> slackUID|slackEmail]:", session, slackApi);
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
			slackApi.sendMessage(channel, message.toString());
		}
	}
}
