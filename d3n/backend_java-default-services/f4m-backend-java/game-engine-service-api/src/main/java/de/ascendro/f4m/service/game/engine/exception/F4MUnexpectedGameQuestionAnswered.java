package de.ascendro.f4m.service.game.engine.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MUnexpectedGameQuestionAnswered extends F4MClientException{

	private static final long serialVersionUID = 4313959201722251942L;

	public F4MUnexpectedGameQuestionAnswered(String message) {
		super(GameEngineExceptionCodes.ERR_UNEXPECTED_GAME_QUESTION_ANSWERED, message);
	}

}
