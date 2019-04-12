package de.ascendro.f4m.service.game.engine.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MGameRequireEntryFeeException extends F4MClientException {

	private static final long serialVersionUID = 8501835770000489934L;

	public F4MGameRequireEntryFeeException(String message) {
		super(GameEngineExceptionCodes.ERR_GAME_REQUIRE_ENTRY_FEE, message);
	}

}
