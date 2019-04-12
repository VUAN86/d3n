package de.ascendro.f4m.service.game.selection.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MGameParticipantCountExceededException extends F4MClientException {

	private static final long serialVersionUID = 3359481625688838274L;

	public F4MGameParticipantCountExceededException(String message) {
		super(GameSelectionExceptionCodes.ERR_GAME_PARTICIPANT_COUNT_EXCEEDED, message);
	}

}
