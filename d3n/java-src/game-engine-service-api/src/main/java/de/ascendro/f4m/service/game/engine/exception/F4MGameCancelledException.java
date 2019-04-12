package de.ascendro.f4m.service.game.engine.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MGameCancelledException extends F4MClientException{

	private static final long serialVersionUID = -4830149170516770548L;

	public F4MGameCancelledException(String message) {
		super(GameEngineExceptionCodes.ERR_GAME_CANCELLED, message);
	}

}
