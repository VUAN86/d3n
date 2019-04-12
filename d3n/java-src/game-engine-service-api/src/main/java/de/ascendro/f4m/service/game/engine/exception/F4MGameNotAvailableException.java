package de.ascendro.f4m.service.game.engine.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MGameNotAvailableException extends F4MClientException {

	private static final long serialVersionUID = 1195973376799076219L;

	public F4MGameNotAvailableException(String message) {
		super(GameEngineExceptionCodes.ERR_GAME_NOT_AVAILABLE, message);
	}

}
