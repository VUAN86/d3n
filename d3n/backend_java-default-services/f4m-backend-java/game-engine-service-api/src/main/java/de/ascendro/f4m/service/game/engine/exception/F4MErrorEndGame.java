package de.ascendro.f4m.service.game.engine.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MErrorEndGame extends F4MClientException {

    private static final long serialVersionUID = -4066938658990889758L;

    public F4MErrorEndGame(String message) {
        super(GameEngineExceptionCodes.F4M_ERROR_AT_THE_END_OF_THE_GAME, message);
    }
}
