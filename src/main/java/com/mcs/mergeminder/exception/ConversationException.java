package com.mcs.mergeminder.exception;

public class ConversationException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public ConversationException() {
		super();
	}

	public ConversationException(String message, Throwable rootCause) {
		super(message, rootCause);
	}

	public ConversationException(String message) {
		super(message);
	}

	public ConversationException(Throwable rootCause) {
		super(rootCause);
	}

}
