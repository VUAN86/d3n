package de.ascendro.f4m.service.payment.exception;

import de.ascendro.f4m.service.exception.server.F4MServerException;

public class F4MTransferBetweenException   extends F4MServerException {
    public F4MTransferBetweenException(String message) {
        super(PaymentServiceExceptionCodes.ERR_TRANSFER_BETWEEN, message);
    }
}
