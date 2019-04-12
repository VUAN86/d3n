package de.ascendro.f4m.service.exception.server;

import de.ascendro.f4m.service.exception.ExceptionCodes;

public class F4MSessionStoreDestroyException extends F4MServerException {

	private static final long serialVersionUID = -6180985962680259707L;

	public F4MSessionStoreDestroyException(String message, Throwable cause) {
		super(ExceptionCodes.ERR_SESSION_STORE_DESTROY, message, cause);
	}

	public F4MSessionStoreDestroyException(String message) {
		super(ExceptionCodes.ERR_SESSION_STORE_DESTROY, message);
	}

}
