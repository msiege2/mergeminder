package com.mcs.mergeminder.slack;

import com.mcs.mergeminder.exception.ConversationException;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;

public interface Conversation {

	void start(SlackChannel channel, SlackUser messageSender, SlackSession session, SlackApi slackApi, String userInput) throws ConversationException;

	void receiveNewInput(SlackChannel channel, SlackUser messageSender, SlackSession session, SlackApi slackApi, String userInput) throws ConversationException;

	boolean isFinished();
}
