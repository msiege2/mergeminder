package com.mcs.mergeminder.slack;

import com.mcs.mergeminder.dao.MergeMinderDb;
import com.mcs.mergeminder.exception.ConversationException;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;

public abstract class ExtendedConversation implements Conversation, SlackMessageSender {

	/**
	 * Determines if conversation is finished
	 **/
	protected boolean finished;
	/**
	 * DAO Object
	 **/
	protected final MergeMinderDb mergeMinderDb;

	public ExtendedConversation(MergeMinderDb mergeMinderDb) {
		this.finished = false;
		this.mergeMinderDb = mergeMinderDb;
	}

	@Override
	public boolean isFinished() {
		return this.finished;
	}

	@Override
	public abstract void start(SlackChannel channel, SlackUser messageSender, SlackSession session, SlackApi slackApi, String userInput) throws ConversationException;

	@Override
	public abstract void receiveNewInput(SlackChannel channel, SlackUser messageSender, SlackSession session, SlackApi slackApi, String userInput) throws ConversationException;
}
