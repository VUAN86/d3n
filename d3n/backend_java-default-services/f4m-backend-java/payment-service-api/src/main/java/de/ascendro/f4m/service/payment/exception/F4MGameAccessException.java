package de.ascendro.f4m.service.payment.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MGameAccessException extends F4MClientException {
    public F4MGameAccessException(String message) {
        super(PaymentServiceExceptionCodes.ERR_GAME_CLOSED, message);
    }
}
