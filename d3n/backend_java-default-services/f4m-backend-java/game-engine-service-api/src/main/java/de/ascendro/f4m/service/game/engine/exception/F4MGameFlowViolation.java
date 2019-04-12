package de.ascendro.f4m.service.game.engine.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MGameFlowViolation extends F4MClientException{

	private static final long serialVersionUID = -8981370767834162979L;

	public F4MGameFlowViolation(String message) {
		super(GameEngineExceptionCodes.ERR_GAME_FLOW_VIOLATION, message);
	}

}
