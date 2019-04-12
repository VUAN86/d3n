package de.ascendro.f4m.service.exception.client;

import de.ascendro.f4m.service.exception.ExceptionType;
import de.ascendro.f4m.service.exception.F4MException;

public abstract class F4MClientException extends F4MException {

	private static final long serialVersionUID = 1967730998357391114L;

	protected F4MClientException(String code, String message) {
		super(ExceptionType.CLIENT, code, message);
	}

	public F4MClientException(String code, String message, Throwable cause) {
		super(ExceptionType.CLIENT, code, message, cause);
	}
}
