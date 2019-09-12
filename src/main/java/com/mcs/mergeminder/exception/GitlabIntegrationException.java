package com.mcs.mergeminder.exception;

public class GitlabIntegrationException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public GitlabIntegrationException() {
		super();
	}

	public GitlabIntegrationException(String message, Throwable rootCause) {
		super(message, rootCause);
	}

	public GitlabIntegrationException(String message) {
		super(message);
	}

	public GitlabIntegrationException(Throwable rootCause) {
		super(rootCause);
	}

}
