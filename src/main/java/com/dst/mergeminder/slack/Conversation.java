package com.dst.mergeminder.slack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;

@Component
public class Conversation {

	private static final Logger logger = LoggerFactory.getLogger(Conversation.class);

	public Conversation() {
		// empty constructor
	}

	void handleIncomingEvent(SlackMessagePosted event, SlackSession session) {
		SlackUser messageSender = event.getSender();
		if (messageSender.getUserName().equalsIgnoreCase("mergeminder")) {
			// ignore events from yourself
			return;
		}
		SlackChannel channelOnWhichMessageWasPosted = event.getChannel();
		if (channelOnWhichMessageWasPosted.isDirect()) {
			handleDirectMessage(event, session);
		}
	}

	void handleDirectMessage(SlackMessagePosted event, SlackSession session) {
		SlackChannel channel = event.getChannel();
		String messageContent = event.getMessageContent();
		SlackUser messageSender = event.getSender();

		if (messageContent.toLowerCase().contains("help")) {
			session.sendMessage(channel, "I'm alive!  Soon I'll be able to help you to understand some of the things I can do.");
			logger.info("Received 'HELP' request from user: {}", messageSender.getRealName());
			return;
		}
		if (messageContent.toLowerCase().contains("hi") || messageContent.toLowerCase().contains("hello") || messageContent.toLowerCase().contains("hey")) {
			session.sendMessage(channel, "Hello, " + messageSender.getRealName() + ".");
			logger.info("Received 'HELLO' request from user: {}", messageSender.getRealName());
			return;
		}
		if (messageContent.toLowerCase().contains("shit") || messageContent.toLowerCase().contains("piss") || messageContent.toLowerCase().contains("fuck")
			|| messageContent.toLowerCase().contains("bitch") || messageContent.toLowerCase().contains("crap") || messageContent.toLowerCase().contains("hell")) {
			session.sendMessage(channel, "Don't talk to me that way!  I'm sure you wouldn't speak to your mother that way!");
			logger.info("Received 'POTTY MOUTH' request from user: {}", messageSender.getRealName());
			return;
		}

		session.sendMessage(channel, "Hmm, I don't know quite what you are saying.  Sorry.");
		logger.info("Received 'UNKNOWN' request from user: {}", messageSender.getRealName());
		return;

	}

}
