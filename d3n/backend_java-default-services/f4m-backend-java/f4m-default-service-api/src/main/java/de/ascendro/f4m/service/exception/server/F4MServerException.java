package de.ascendro.f4m.service.exception.server;

import de.ascendro.f4m.service.exception.ExceptionType;
import de.ascendro.f4m.service.exception.F4MException;

public abstract class F4MServerException extends F4MException {

	private static final long serialVersionUID = -3410967980694068559L;

	protected F4MServerException(String code, String message) {
		super(ExceptionType.SERVER, code, message);
	}

	public F4MServerException(String code, String message, Throwable cause) {
		super(ExceptionType.SERVER, code, message, cause);
	}

}
