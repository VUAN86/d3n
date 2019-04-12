package de.ascendro.f4m.service.winning.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MComponentAlreadyUsedException extends F4MClientException {

	private static final long serialVersionUID = 5636854564509582988L;

	public F4MComponentAlreadyUsedException(String message) {
		super(WinningServiceExceptionCodes.ERR_COMPONENT_ALREADY_USED, message);
	}

}
