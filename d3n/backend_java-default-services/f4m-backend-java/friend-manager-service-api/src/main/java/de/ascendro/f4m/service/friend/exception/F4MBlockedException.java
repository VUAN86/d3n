package de.ascendro.f4m.service.friend.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

/**
 * Exception to be thrown if trying to manipulate relation with type BLOCKED_BY.
 */
public class F4MBlockedException extends F4MClientException{

	private static final long serialVersionUID = -8981370767834162979L;

	public F4MBlockedException(String message) {
		super(FriendManagerExceptionCodes.ERR_BLOCKED, message);
	}

}
