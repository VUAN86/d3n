package de.ascendro.f4m.service.exception.auth;

import de.ascendro.f4m.service.exception.ExceptionCodes;

/**
 * Unable to identify and authorize user access
 */
public class F4MInsufficientRightsException extends F4MAuthenticationException {//Rename

	private static final long serialVersionUID = -3317758124262453508L;

	public F4MInsufficientRightsException(String message) {
		super(ExceptionCodes.ERR_INSUFFICIENT_RIGHTS, message);
	}

	public F4MInsufficientRightsException(String message, Throwable cause) {
		super(ExceptionCodes.ERR_INSUFFICIENT_RIGHTS, message, cause);
	}

}
