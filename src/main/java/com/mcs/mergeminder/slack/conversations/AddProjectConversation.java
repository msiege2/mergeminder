package com.mcs.mergeminder.slack.conversations;

import com.mcs.mergeminder.dao.MergeMinderDb;
import com.mcs.mergeminder.dto.MinderProjectsModel;
import com.mcs.mergeminder.exception.ConversationException;
import com.mcs.mergeminder.slack.ExtendedConversation;
import com.mcs.mergeminder.slack.SlackApi;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;

/**
 * Conversation to handle Adding new projects to MergeMinder.
 */
public class AddProjectConversation extends ExtendedConversation {

	public enum ConversationState {
		INITIALIZED,
		SELECTING_NAMESPACE,
		SELECTING_PROJECTNAME;
	}

	private ConversationState conversationState;

	private String namespace;
	private String projectName;

	public AddProjectConversation(MergeMinderDb mergeMinderDb) {
		super(mergeMinderDb);
		this.conversationState = ConversationState.INITIALIZED;
	}

	@Override
	public void start(SlackChannel channel, SlackUser messageSender, SlackSession session, SlackApi slackApi, String userInput) throws ConversationException {
		try {
			simulateHumanStyleMessageSending(channel, "Ok, let's add a new project to mind!", session, slackApi);
			slackApi.sendMessage(channel, "What is the namespace of the project that you are adding? (Type \"exit\" to cancel)");
			this.conversationState = ConversationState.SELECTING_NAMESPACE;
		} catch (Exception e) {
			throw new ConversationException("Error in starting ADD PROJECT conversation.", e);
		}
	}

	@Override
	public void receiveNewInput(SlackChannel channel, SlackUser messageSender, SlackSession session, SlackApi slackApi, String userInput) throws ConversationException {
		try {
			if ("exit".equals(userInput.trim().toLowerCase())) {
				simulateHumanStyleMessageSending(channel, "Cancelling ADD PROJECT operation.", session, slackApi);
				this.finished = true;
				return;
			}
			switch (this.conversationState) {
				case SELECTING_NAMESPACE:
					selectNamespace(channel, messageSender, session, slackApi, userInput);
					break;
				case SELECTING_PROJECTNAME:
					addProject(channel, messageSender, session, slackApi, userInput);
					break;
				default:
					throw new ConversationException("Unknown state in ADD PROJECT conversation!");
			}
		} catch (ConversationException e) {
			throw e;
		} catch (Exception e) {
			throw new ConversationException("Error in ADD PROJECT conversation.", e);
		}
	}

	private void selectNamespace(SlackChannel channel, SlackUser messageSender, SlackSession session, SlackApi slackApi, String userInput) {
		if (userInput == null || userInput.trim().isEmpty()) {
			simulateHumanStyleMessageSending(channel, "Please enter a valid namespace.  (Type \"exit\" to cancel)", session, slackApi);
		} else {
			this.namespace = userInput.trim();
			this.conversationState = ConversationState.SELECTING_PROJECTNAME;
			simulateHumanStyleMessageSending(channel, "Ok.  Now, please tell me the name of the project to mind in this namespace. (Type \"exit\" to cancel)", session, slackApi);
		}
	}

	private void addProject(SlackChannel channel, SlackUser messageSender, SlackSession session, SlackApi slackApi, String userInput) throws ConversationException {
		try {
			if (userInput == null || userInput.trim().isEmpty()) {
				simulateHumanStyleMessageSending(channel, "Please enter a valid project name.  (Type \"exit\" to cancel)", session, slackApi);
			} else {
				this.projectName = userInput.trim();
				MinderProjectsModel model = new MinderProjectsModel(this.namespace, this.projectName);
				this.mergeMinderDb.saveMinderProject(model);
				StringBuilder message = new StringBuilder();
				message.append("Great!\n I have added a new project [");
				message.append(this.namespace);
				message.append("/");
				message.append(this.projectName);
				message.append("].");
				simulateHumanStyleMessageSending(channel, message.toString(), session, slackApi);
				this.finished = true;
			}
		} catch (Exception e) {
			throw new ConversationException("Error in ADD PROJECT conversation.", e);
		}
	}
}
