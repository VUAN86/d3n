package de.ascendro.f4m.service.game.selection.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MGameInvitationNotValidException extends F4MClientException{

	private static final long serialVersionUID = 1L;

	public F4MGameInvitationNotValidException(String message) {
		super(GameSelectionExceptionCodes.ERR_GAME_INVITATION_NOT_VALID, message);
	}

}
