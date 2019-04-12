package de.ascendro.f4m.service.game.selection.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MPoolsNotValidException extends F4MClientException {
	private static final long serialVersionUID = -2891250649459446739L;

	public F4MPoolsNotValidException(String message) {
		super(GameSelectionExceptionCodes.ERR_POOLS_NOT_VALID, message);
	}

}
