package de.ascendro.f4m.service.game.engine.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MAllowedGamePlaysExhaustedException extends F4MClientException {

	private static final long serialVersionUID = 5693317903618215898L;

	public F4MAllowedGamePlaysExhaustedException() {
		super(GameEngineExceptionCodes.ERR_ALLOWED_GAME_PLAYS_EXHAUSTED, "Game has been already played maximum allowed amount of times");
	}

}
