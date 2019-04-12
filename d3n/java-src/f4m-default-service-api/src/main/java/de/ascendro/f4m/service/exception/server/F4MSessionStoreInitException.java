package de.ascendro.f4m.service.exception.server;

import de.ascendro.f4m.service.exception.ExceptionCodes;

public class F4MSessionStoreInitException extends F4MServerException {

	private static final long serialVersionUID = 3432548710474098319L;

	public F4MSessionStoreInitException(String message, Throwable cause) {
		super(ExceptionCodes.ERR_SESSION_STORE_INIT, message, cause);
	}

	public F4MSessionStoreInitException(String message) {
		super(ExceptionCodes.ERR_SESSION_STORE_INIT, message);
	}
}
