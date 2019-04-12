package de.ascendro.f4m.service.exception.validation;

import de.ascendro.f4m.service.exception.ExceptionType;
import de.ascendro.f4m.service.exception.F4MException;

public abstract class F4MValidationException extends F4MException {

	private static final long serialVersionUID = 1578492822362917733L;

	protected F4MValidationException(String code, String message) {
		super(ExceptionType.VALIDATION, code, message);
	}

	protected F4MValidationException(String code, String message, Throwable cause) {
		super(ExceptionType.VALIDATION, code, message, cause);
	}

}
