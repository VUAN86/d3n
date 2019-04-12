package de.ascendro.f4m.service.tombola.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class TombolaAlreadyDrawnException extends F4MClientException {

    private static final long serialVersionUID = 5636854564509582989L;

    public TombolaAlreadyDrawnException(String message) {
        super(TombolaServiceExceptionCodes.ERR_TOMBOLA_ALREADY_DRAWN, message);
    }

}