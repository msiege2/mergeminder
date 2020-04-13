package com.mcs.mergeminder.slack;

import com.mcs.mergeminder.dao.MergeMinderDb;
import com.mcs.mergeminder.dto.MinderProjectsModel;
import com.mcs.mergeminder.exception.ConversationException;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;

public class AddProjectConversation implements Conversation, SlackMessageSender {

	public enum ConversationState {
		INITIALIZED,
		SELECTING_NAMESPACE,
		SELECTING_PROJECTNAME;
	}

	private ConversationState conversationState;
	private boolean finished;

	private String namespace;
	private String projectName;

	private final MergeMinderDb mergeMinderDb;

	public AddProjectConversation(MergeMinderDb mergeMinderDb) {
		this.conversationState = ConversationState.INITIALIZED;
		this.finished = false;
		this.mergeMinderDb = mergeMinderDb;
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public void start(SlackChannel channel, SlackUser messageSender, SlackSession session, String userInput) throws ConversationException {
		try {
			simulateHumanStyleMessageSending(channel, "Ok, let's add a new project to mind!", session);
			session.sendMessage(channel, "What is the namespace of the project that you are adding? (Type \"exit\" to cancel)");
			conversationState = ConversationState.SELECTING_NAMESPACE;
		} catch (Exception e) {
			throw new ConversationException("Error in starting ADD PROJECT conversation.", e);
		}
	}

	@Override
	public void receiveNewInput(SlackChannel channel, SlackUser messageSender, SlackSession session, String userInput) throws ConversationException {
		try {
			if ("exit".equals(userInput.trim().toLowerCase())) {
				simulateHumanStyleMessageSending(channel, "Cancelling ADD PROJECT operation.", session);
				finished = true;
				return;
			}
			switch (conversationState) {
				case SELECTING_NAMESPACE:
					selectNamespace(channel, messageSender, session, userInput);
					break;
				case SELECTING_PROJECTNAME:
					addProject(channel, messageSender, session, userInput);
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

	private void selectNamespace(SlackChannel channel, SlackUser messageSender, SlackSession session, String userInput) {
		if (userInput == null || userInput.trim().isEmpty()) {
			simulateHumanStyleMessageSending(channel, "Please enter a valid namespace.  (Type \"exit\" to cancel)", session);
		} else {
			this.namespace = userInput.trim();
			conversationState = ConversationState.SELECTING_PROJECTNAME;
			simulateHumanStyleMessageSending(channel, "Ok.  Now, please tell me the name of the project to mind in this namespace. (Type \"exit\" to cancel)", session);
		}
	}

	private void addProject(SlackChannel channel, SlackUser messageSender, SlackSession session, String userInput) throws ConversationException {
		try {
			if (userInput == null || userInput.trim().isEmpty()) {
				simulateHumanStyleMessageSending(channel, "Please enter a valid project name.  (Type \"exit\" to cancel)", session);
			} else {
				this.projectName = userInput.trim();
				MinderProjectsModel model = new MinderProjectsModel(namespace, projectName);
				mergeMinderDb.saveMinderProject(model);
				StringBuilder message = new StringBuilder();
				message.append("Great!\n I have added a new project [");
				message.append(namespace);
				message.append("/");
				message.append(projectName);
				message.append("].");
				simulateHumanStyleMessageSending(channel, message.toString(), session);
				finished = true;
			}
		} catch (Exception e) {
			throw new ConversationException("Error in ADD PROJECT conversation.", e);
		}
	}
}
