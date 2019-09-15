package com.mcs.mergeminder.slack;

import java.util.concurrent.ThreadLocalRandom;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackPreparedMessage;
import com.ullink.slack.simpleslackapi.SlackSession;

public interface SlackMessageSender {

	/**
	 * Send the given message to the channel, but send a typing event to the channel first, add a delay, and then send the message.
	 *
	 * @param channel
	 * @param message
	 * @param session
	 */
	default void simulateHumanStyleMessageSending(SlackChannel channel, String message, SlackSession session) {
		SlackPreparedMessage slackPreparedMessage = new SlackPreparedMessage.Builder()
			.withMessage(message)
			.withUnfurl(false)
			.build();
		simulateHumanStyleMessageSending(channel, slackPreparedMessage, session);
	}

	/**
	 * Send the given message to the channel, but send a typing event to the channel first, add a delay, and then send the message.
	 *
	 * @param channel
	 * @param message
	 * @param session
	 */
	default void simulateHumanStyleMessageSending(SlackChannel channel, SlackPreparedMessage message, SlackSession session) {
		// first generate a random "typing time between 600 and 1200 milliseconds.
		int randomTypingTime = ThreadLocalRandom.current().nextInt(500, 900 + 1);
		// then take a pseudo-random amount of milliseconds for each character in the message.  This will be added to the typing time.
		int lengthBasedSplay = message.getMessage().length() * 20 * ThreadLocalRandom.current().nextInt(35, 81) / 100;

		// Tell the channel MergeMinder is typing.
		session.sendTyping(channel);
		try {
			Thread.sleep((long) randomTypingTime + lengthBasedSplay);
		} catch (
			InterruptedException e) {
			// ignored
		}
		session.sendMessage(channel, message);
	}

}
