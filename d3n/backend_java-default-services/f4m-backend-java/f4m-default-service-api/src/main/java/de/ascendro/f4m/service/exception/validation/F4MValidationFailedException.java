package de.ascendro.f4m.service.exception.validation;

import de.ascendro.f4m.service.exception.ExceptionCodes;

public class F4MValidationFailedException extends F4MValidationException {

	private static final long serialVersionUID = 8537836803486601704L;

	public F4MValidationFailedException(String message) {
		super(ExceptionCodes.ERR_VALIDATION_FAILED, message);
	}

	public F4MValidationFailedException(String message, Throwable cause) {
		super(ExceptionCodes.ERR_VALIDATION_FAILED, message, cause);
	}

}
