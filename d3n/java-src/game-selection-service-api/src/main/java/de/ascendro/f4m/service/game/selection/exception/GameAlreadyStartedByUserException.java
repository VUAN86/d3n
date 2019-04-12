package de.ascendro.f4m.service.game.selection.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class GameAlreadyStartedByUserException  extends F4MClientException{

	private static final long serialVersionUID = -8185077908788392013L;

	public GameAlreadyStartedByUserException(String message) {
		super(GameSelectionExceptionCodes.ERR_GAME_ALREADY_STARTED_BY_USER, message);
	}

}
