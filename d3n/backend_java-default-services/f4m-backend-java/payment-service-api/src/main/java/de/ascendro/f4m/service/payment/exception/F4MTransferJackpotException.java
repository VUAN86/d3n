package de.ascendro.f4m.service.payment.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;
import de.ascendro.f4m.service.exception.server.F4MServerException;

public class F4MTransferJackpotException extends F4MServerException {
    public F4MTransferJackpotException(String message) {
        super(PaymentServiceExceptionCodes.ERR_TRANSFER_JACKPOT, message);
    }
}
