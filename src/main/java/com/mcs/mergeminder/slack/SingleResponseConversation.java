package com.mcs.mergeminder.slack;

import com.mcs.mergeminder.dao.MergeMinderDb;
import com.mcs.mergeminder.exception.ConversationException;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;

/**
 * <tt>SingleResponseConversation</tt> handles conversations that do not require user
 * input after starting.  Essentially, they begin and finish within a single roundtrip.<br/>
 * For conversations that do not require further user input use
 * For more robust conversations, use  {@link BasicConversation} or {@link ExtendedConversation}.
 */
public abstract class SingleResponseConversation implements Conversation, SlackMessageSender {

	/**
	 * Determines if conversation is finished
	 **/
	protected boolean finished;
	/**
	 * DAO Object
	 **/
	protected final MergeMinderDb mergeMinderDb;

	protected String conversationType;

	public SingleResponseConversation(MergeMinderDb mergeMinderDb, String conversationType) {
		this.finished = false;
		this.mergeMinderDb = mergeMinderDb;
		this.conversationType = conversationType;
	}

	@Override
	public boolean isFinished() {
		return finished;
	}

	@Override
	public void start(SlackChannel channel, SlackUser messageSender, SlackSession session, String userInput) throws ConversationException {
		try {
			handleResponse(channel, messageSender, session, userInput);
			finished = true;
		} catch (ConversationException e) {
			throw e;
		} catch (Exception e) {
			throw new ConversationException("Error in starting " + conversationType + " conversation.", e);
		}
	}

	@Override
	public void receiveNewInput(SlackChannel channel, SlackUser messageSender, SlackSession session, String userInput) throws ConversationException {
		throw new ConversationException("SingleResponseConversation does not support new input.");
	}

	protected abstract void handleResponse(SlackChannel channel, SlackUser messageSender, SlackSession session, String userInput) throws ConversationException;
}
