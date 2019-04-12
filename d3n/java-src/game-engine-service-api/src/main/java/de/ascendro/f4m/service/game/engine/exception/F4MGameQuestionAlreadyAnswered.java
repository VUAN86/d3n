package de.ascendro.f4m.service.game.engine.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MGameQuestionAlreadyAnswered extends F4MClientException{

	private static final long serialVersionUID = -6083683517692904806L;

	public F4MGameQuestionAlreadyAnswered(String message) {
		super(GameEngineExceptionCodes.ERR_GAME_QUESTION_ALREADY_ANSWERED, message);
	}

}
