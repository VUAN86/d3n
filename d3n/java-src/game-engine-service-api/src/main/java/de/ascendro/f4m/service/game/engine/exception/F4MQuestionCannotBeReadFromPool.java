package de.ascendro.f4m.service.game.engine.exception;

import de.ascendro.f4m.service.exception.server.F4MServerException;

public class F4MQuestionCannotBeReadFromPool extends F4MServerException {

	private static final long serialVersionUID = 8458042808611031241L;

	public F4MQuestionCannotBeReadFromPool(String message) {
		super(GameEngineExceptionCodes.ERR_QUESTION_CANNOT_BE_READ_FROM_POOL, message);
	}

}
