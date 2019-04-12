package de.ascendro.f4m.service.exception.server;

import de.ascendro.f4m.service.exception.ExceptionCodes;

public class F4MConnectionErrorException extends F4MServerException {

	private static final long serialVersionUID = -9008077524626858132L;

	public F4MConnectionErrorException(String message, Throwable cause) {
		super(ExceptionCodes.ERR_CONNECTION_ERROR, message, cause);
	}

	public F4MConnectionErrorException(String message) {
		super(ExceptionCodes.ERR_CONNECTION_ERROR, message);
	}

}
