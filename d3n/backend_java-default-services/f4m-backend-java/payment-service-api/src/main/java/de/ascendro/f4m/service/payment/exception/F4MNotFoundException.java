package de.ascendro.f4m.service.payment.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MNotFoundException extends F4MClientException {
    public F4MNotFoundException(String message) {
        super(PaymentServiceExceptionCodes.ERR_NOT_FOUND, message);
    }

}
