package de.ascendro.f4m.service.game.selection.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MEntryFeeNotValidException extends F4MClientException {
	private static final long serialVersionUID = 6235847365263491827L;

	public F4MEntryFeeNotValidException(String message) {
		super(GameSelectionExceptionCodes.ERR_ENTRY_FEE_NOT_VALID, message);
	}

}
