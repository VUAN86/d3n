package de.ascendro.f4m.service.game.engine.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MJokerNotAvailableException extends F4MClientException{

	private static final long serialVersionUID = -823720742451421931L;

	public F4MJokerNotAvailableException() {
		super(GameEngineExceptionCodes.ERR_JOKER_NOT_AVAILABLE, "Joker not available");
	}

}
