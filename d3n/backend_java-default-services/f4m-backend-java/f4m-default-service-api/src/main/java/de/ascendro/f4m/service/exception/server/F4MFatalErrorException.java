package de.ascendro.f4m.service.exception.server;

import de.ascendro.f4m.service.exception.ExceptionCodes;

/**
 * Exception represents unexpected errors (from a business point of view).
 */
public class F4MFatalErrorException extends F4MServerException {

	private static final long serialVersionUID = -8646880787838288017L;

	public F4MFatalErrorException(String message) {
		super(ExceptionCodes.ERR_FATAL_ERROR, message);
	}

	public F4MFatalErrorException(String message, Throwable cause) {
		super(ExceptionCodes.ERR_FATAL_ERROR, message, cause);
	}
}
