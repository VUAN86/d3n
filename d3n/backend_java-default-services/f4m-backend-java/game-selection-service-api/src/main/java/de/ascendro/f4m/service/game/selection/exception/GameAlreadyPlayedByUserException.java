package de.ascendro.f4m.service.game.selection.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class GameAlreadyPlayedByUserException  extends F4MClientException
{
    public GameAlreadyPlayedByUserException(String message) {
        super(GameSelectionExceptionCodes.ERR_GAME_ALREADY_PLAYED_BY_USER, message);
    }
}
