package de.ascendro.f4m.service.game.selection.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MGameHasntStartedYetOrIsOverException extends F4MClientException {

    public F4MGameHasntStartedYetOrIsOverException(String code, String message) {
        super(code, message);
    }

}
