package com.dst.mergeminder.slack;

import java.util.concurrent.ThreadLocalRandom;

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
		if (SlackChannel.SlackChannelType.INSTANT_MESSAGING == channelOnWhichMessageWasPosted.getType()) {
			handleDirectMessage(event, session);
		}
	}

	void handleDirectMessage(SlackMessagePosted event, SlackSession session) {
		SlackChannel channel = event.getChannel();
		String messageContent = event.getMessageContent();
		SlackUser messageSender = event.getSender();

		if (messageContent.toLowerCase().contains("help")) {
			simulateHumanStyleMessageSending(channel, "I'm alive!  Soon I'll be able to help you to understand some of the things I can do.", session);
			logger.info("Received 'HELP' request from user: {}", messageSender.getRealName());
			return;
		}
		if (messageContent.toLowerCase().contains("hi") || messageContent.toLowerCase().contains("hello") || messageContent.toLowerCase().contains("hey")) {
			simulateHumanStyleMessageSending(channel, "Hello, " + messageSender.getRealName() + ".", session);
			logger.info("Received 'HELLO' request from user: {}", messageSender.getRealName());
			return;
		}
		if (messageContent.toLowerCase().contains("shit") || messageContent.toLowerCase().contains("piss") || messageContent.toLowerCase().contains("fuck")
			|| messageContent.toLowerCase().contains("bitch") || messageContent.toLowerCase().contains("crap") || messageContent.toLowerCase().contains("hell")) {
			simulateHumanStyleMessageSending(channel, "Don't talk to me that way!  I'm sure you wouldn't speak to your mother that way!", session);
			logger.info("Received 'POTTY MOUTH' request from user: {}", messageSender.getRealName());
			return;
		}

		simulateHumanStyleMessageSending(channel, "Hmm, I don't know quite what you are saying.  Sorry.", session);
		logger.info("Received 'UNKNOWN' request from user: {}", messageSender.getRealName());
		return;

	}

	private void simulateHumanStyleMessageSending(SlackChannel channel, String message, SlackSession session) {
		// first generate a random "typing time between 1 and 3 seconds.
		int randomTypingTime = ThreadLocalRandom.current().nextInt(1000, 3000 + 1);
		// then take 0.025 seconds for each character in the message.  This will be added to the typing time.
		int lengthBasedSplay = message.length() * 25;

		// Tell the channel MergeMinder is typing.
		session.sendTyping(channel);
		try {
			Thread.sleep(randomTypingTime + lengthBasedSplay);
		} catch (InterruptedException e) {
			// ignored
		}
		session.sendMessage(channel, message);
	}
}
