package de.ascendro.f4m.service.game.selection.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MMaxParticipantCountNotValidException extends F4MClientException {
	private static final long serialVersionUID = -3406529548006283142L;

	public F4MMaxParticipantCountNotValidException(String message) {
		super(GameSelectionExceptionCodes.ERR_MAX_PARTICIPANT_COUNT_NOT_VALID, message);
	}

}
