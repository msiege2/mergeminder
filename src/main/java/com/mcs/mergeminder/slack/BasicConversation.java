package com.mcs.mergeminder.slack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mcs.mergeminder.dao.MergeMinderDb;
import com.mcs.mergeminder.exception.ConversationException;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;

/**
 * <tt>BasicConversation</tt> handles conversations which follow a pattern of:<br>
 *     <ol>
 *         <li>MergeMinder sends a message</li>
 *         <li>User responds</li>
 *         <li>MergeMinder sends a response</li>
 *     </ol>
 * No futher conversation is allowed beyond that.  For conversations that do not require further user input use {@link SingleResponseConversation}.
 * For more robust conversations, use  {@link ExtendedConversation}.
 */
public abstract class BasicConversation implements Conversation, SlackMessageSender {

	private static final Logger logger = LoggerFactory.getLogger(BasicConversation.class);

	/**
	 * Basic Conversations only have a single state
	 **/
	enum ConversationState {
		INITIALIZED,
		INPUT_ADDED
	}

	protected ConversationState conversationState;
	/**
	 * Determines if conversation is finished
	 **/
	protected boolean finished;
	/**
	 * DAO Object
	 **/
	protected final MergeMinderDb mergeMinderDb;

	protected String conversationType;

	public BasicConversation(MergeMinderDb mergeMinderDb, String conversationType) {
		this.conversationState = ConversationState.INITIALIZED;
		this.finished = false;
		this.mergeMinderDb = mergeMinderDb;
		this.conversationType = conversationType;
	}

	@Override
	public boolean isFinished() {
		return this.finished;
	}

	@Override
	public void start(SlackChannel channel, SlackUser messageSender, SlackSession session, SlackApi slackApi, String userInput) throws ConversationException {
		try {
			showInitialMessage(channel, messageSender, session, slackApi, userInput);
			this.conversationState = ConversationState.INPUT_ADDED;
		} catch (ConversationException e) {
			throw e;
		} catch (Exception e) {
			throw new ConversationException("Error in starting " + this.conversationType + " conversation.", e);
		}
	}

	@Override
	public void receiveNewInput(SlackChannel channel, SlackUser messageSender, SlackSession session, SlackApi slackApi, String userInput) throws ConversationException {
		try {
			if ("exit".equalsIgnoreCase(userInput.trim())) {
				handleExit(channel, messageSender, session, slackApi, userInput);
				this.finished = true;
				return;
			}
			switch (this.conversationState) {
				case INPUT_ADDED:
					this.finished = handleResponse(channel, messageSender, session, slackApi, userInput);
					break;
				default:
					throw new ConversationException("Unknown state in " + this.conversationType + " conversation!");
			}
		} catch (ConversationException e) {
			throw e;
		} catch (Exception e) {
			throw new ConversationException("Error in " + this.conversationType + " conversation.", e);
		}
		if (this.finished) {
			logger.info(this.conversationType + " conversation is now finished.");
		}
	}

	protected abstract void showInitialMessage(SlackChannel channel, SlackUser messageSender, SlackSession session, SlackApi slackApi, String userInput) throws ConversationException;

	/**
	 * Takes the input to the initial message from the user and processes it.
	 *
	 * @param channel
	 * @param messageSender
	 * @param session
	 * @param userInput
	 * @return <tt>true</tt> if the conversation is finished, <tt>false</tt> if the conversation should be kept alive in its current state.
	 * @throws ConversationException
	 */
	protected abstract boolean handleResponse(SlackChannel channel, SlackUser messageSender, SlackSession session, SlackApi slackApi, String userInput) throws ConversationException;

	protected void handleExit(SlackChannel channel, SlackUser messageSender, SlackSession session, SlackApi slackApi, String userInput) throws ConversationException {
		simulateHumanStyleMessageSending(channel, "Cancelling " + this.conversationType + " operation.", session, slackApi);
	}

}
