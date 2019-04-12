package de.ascendro.f4m.service.exception;

/**
 * Exception to be used only in tests, when catching unexpected checked exceptions.
 */
public class UnexpectedTestException extends RuntimeException {
	private static final long serialVersionUID = 5948248691586764208L;

	public UnexpectedTestException(String message) {
		super(message);
	}

	public UnexpectedTestException(String message, Throwable cause) {
		super(message, cause);
	}

}
