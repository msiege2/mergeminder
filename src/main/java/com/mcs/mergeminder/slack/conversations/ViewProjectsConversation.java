package com.mcs.mergeminder.slack.conversations;

import java.util.List;

import com.mcs.mergeminder.dao.MergeMinderDb;
import com.mcs.mergeminder.dto.MinderProjectsModel;
import com.mcs.mergeminder.exception.ConversationException;
import com.mcs.mergeminder.slack.ConversationType;
import com.mcs.mergeminder.slack.SingleResponseConversation;
import com.mcs.mergeminder.slack.SlackApi;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;

public class ViewProjectsConversation extends SingleResponseConversation {

	public ViewProjectsConversation(MergeMinderDb mergeMinderDb) {
		super(mergeMinderDb, ConversationType.VIEW_PROJECTS.toString());
	}

	@Override
	protected void handleResponse(SlackChannel channel, SlackUser messageSender, SlackSession session, SlackApi slackApi, String namespace) throws ConversationException {
		List<MinderProjectsModel> projectList = (namespace == null)
			? this.mergeMinderDb.getMinderProjects() : this.mergeMinderDb.getMinderProjectsForNamespace(namespace);
		if (projectList == null) {
			throw new ConversationException("Could not load project list.");
		}
		if (projectList.isEmpty()) {
			simulateHumanStyleMessageSending(channel, "I could not find any Gitlab projects that have MergeMinding enabled"
				+ (namespace != null ? " for namespace " + namespace : "") + ":", session, slackApi);
		} else {
			simulateHumanStyleMessageSending(channel, "Here are the Gitlab projects that have MergeMinding enabled"
				+ (namespace != null ? " for namespace " + namespace : "") + ":", session, slackApi);
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
			slackApi.sendMessage(channel, message.toString());
		}

	}
}
