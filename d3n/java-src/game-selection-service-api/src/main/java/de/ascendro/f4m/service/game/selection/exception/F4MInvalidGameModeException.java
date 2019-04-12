package de.ascendro.f4m.service.game.selection.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MInvalidGameModeException extends F4MClientException {
	private static final long serialVersionUID = 1L;

	public F4MInvalidGameModeException(String message) {
		super(GameSelectionExceptionCodes.ERR_MODE_NOT_VALID, message);
	}

}
