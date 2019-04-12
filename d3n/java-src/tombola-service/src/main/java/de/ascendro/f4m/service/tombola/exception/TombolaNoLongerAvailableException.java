package de.ascendro.f4m.service.tombola.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class TombolaNoLongerAvailableException extends F4MClientException {

    private static final long serialVersionUID = 5636854564509582989L;

    public TombolaNoLongerAvailableException(String message) {
        super(TombolaServiceExceptionCodes.ERR_NO_LONGER_AVAILABLE, message);
    }

}