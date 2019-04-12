package de.ascendro.f4m.service.winning.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MComponentNotAvailableException extends F4MClientException {
	private static final long serialVersionUID = -2427903133886061341L;

	public F4MComponentNotAvailableException(String message) {
		super(WinningServiceExceptionCodes.ERR_COMPONENT_NOT_AVAILABLE, message);
	}

}
