package de.ascendro.f4m.service.payment.exception;

import de.ascendro.f4m.service.exception.server.F4MServerException;

public class F4MCloseJackpotException  extends F4MServerException {
    public F4MCloseJackpotException(String message) {
        super(PaymentServiceExceptionCodes.ERR_CLOSE_JACKPOT, message);
    }
}
