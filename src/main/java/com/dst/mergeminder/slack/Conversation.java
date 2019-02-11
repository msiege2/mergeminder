package com.dst.mergeminder.slack;

import org.springframework.stereotype.Component;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

@Component
public class Conversation {

	public Conversation() {
		// empty constructor
	}

	void handleIncomingEvent(SlackMessagePosted event, SlackSession session) {
		SlackChannel channelOnWhichMessageWasPosted = event.getChannel();
		event.getMessageSubType();
		String messageContent = event.getMessageContent();
		SlackUser messageSender = event.getSender();
	}
}
