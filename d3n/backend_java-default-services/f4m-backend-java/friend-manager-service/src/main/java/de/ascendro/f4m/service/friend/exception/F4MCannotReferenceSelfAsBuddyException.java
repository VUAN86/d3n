package de.ascendro.f4m.service.friend.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MCannotReferenceSelfAsBuddyException extends F4MClientException {

	private static final long serialVersionUID = 5636854564509582988L;

	public F4MCannotReferenceSelfAsBuddyException(String message) {
		super(FriendServiceExceptionCodes.ERR_CANNOT_REFERENCE_SELF_AS_BUDDY, message);
	}

}
