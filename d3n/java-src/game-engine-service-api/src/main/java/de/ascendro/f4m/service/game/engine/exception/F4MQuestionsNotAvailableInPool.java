package de.ascendro.f4m.service.game.engine.exception;

import de.ascendro.f4m.service.exception.server.F4MServerException;

public class F4MQuestionsNotAvailableInPool extends F4MServerException {

	private static final long serialVersionUID = 6343070450037462506L;

	public F4MQuestionsNotAvailableInPool(String message) {
		super(GameEngineExceptionCodes.ERR_QUESTIONS_NOT_AVAILABLE_IN_POOL, message);
	}

}
