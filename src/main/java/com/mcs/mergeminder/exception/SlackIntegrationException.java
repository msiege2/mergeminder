package com.mcs.mergeminder.exception;

public class SlackIntegrationException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public SlackIntegrationException() {
		super();
	}

	public SlackIntegrationException(String message, Throwable rootCause) {
		super(message, rootCause);
	}

	public SlackIntegrationException(String message) {
		super(message);
	}

	public SlackIntegrationException(Throwable rootCause) {
		super(rootCause);
	}

}
