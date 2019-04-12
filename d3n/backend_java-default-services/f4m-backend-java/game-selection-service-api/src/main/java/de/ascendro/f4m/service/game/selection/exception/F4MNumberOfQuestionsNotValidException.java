package de.ascendro.f4m.service.game.selection.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MNumberOfQuestionsNotValidException extends F4MClientException {
	private static final long serialVersionUID = -6443871100724796402L;

	public F4MNumberOfQuestionsNotValidException(String message) {
		super(GameSelectionExceptionCodes.ERR_NUMBER_OF_QUESTIONS_NOT_VALID, message);
	}

}
