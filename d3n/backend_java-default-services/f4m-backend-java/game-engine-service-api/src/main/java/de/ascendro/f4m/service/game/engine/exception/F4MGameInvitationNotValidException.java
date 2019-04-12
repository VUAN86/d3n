package de.ascendro.f4m.service.game.engine.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MGameInvitationNotValidException extends F4MClientException {

    private static final long serialVersionUID = -5454296000796897922L;

    public F4MGameInvitationNotValidException(String message) {
        super(GameEngineExceptionCodes.ERR_GAME_INVITATION_NOT_VALID, message);
    }

}
