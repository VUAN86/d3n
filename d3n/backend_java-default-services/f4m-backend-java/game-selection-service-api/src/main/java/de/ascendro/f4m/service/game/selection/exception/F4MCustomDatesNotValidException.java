package de.ascendro.f4m.service.game.selection.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MCustomDatesNotValidException extends F4MClientException {
	private static final long serialVersionUID = -5688273797573127952L;

	public F4MCustomDatesNotValidException(String message) {
		super(GameSelectionExceptionCodes.ERR_DATES_NOT_VALID, message);
	}

}
