package de.ascendro.f4m.service.tombola.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class TombolaNoTicketsAvailableException extends F4MClientException {

    private static final long serialVersionUID = 5636854564509582987L;

    public TombolaNoTicketsAvailableException(String message) {
        super(TombolaServiceExceptionCodes.ERR_NO_TICKETS_AVAILABLE, message);
    }

}