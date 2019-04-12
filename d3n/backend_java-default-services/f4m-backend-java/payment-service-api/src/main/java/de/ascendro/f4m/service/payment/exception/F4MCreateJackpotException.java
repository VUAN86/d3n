package de.ascendro.f4m.service.payment.exception;

import de.ascendro.f4m.service.exception.server.F4MServerException;

public class F4MCreateJackpotException extends F4MServerException {
    public F4MCreateJackpotException(String message) {
        super(PaymentServiceExceptionCodes.ERR_CREATE_JACKPOT, message);
    }
}
