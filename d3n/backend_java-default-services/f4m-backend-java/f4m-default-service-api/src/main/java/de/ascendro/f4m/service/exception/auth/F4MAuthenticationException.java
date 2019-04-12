package de.ascendro.f4m.service.exception.auth;

import de.ascendro.f4m.service.exception.ExceptionType;
import de.ascendro.f4m.service.exception.F4MException;

public abstract class F4MAuthenticationException extends F4MException {

	private static final long serialVersionUID = -4729716415621976222L;

	public F4MAuthenticationException(String code, String message, Throwable cause) {
		super(ExceptionType.AUTH, code, message, cause);
	}

	public F4MAuthenticationException(String code, String message) {
		super(ExceptionType.AUTH, code, message);
	}

}
