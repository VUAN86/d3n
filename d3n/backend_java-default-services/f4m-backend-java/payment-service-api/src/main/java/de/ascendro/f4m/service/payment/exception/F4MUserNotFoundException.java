package de.ascendro.f4m.service.payment.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MUserNotFoundException extends F4MClientException {
    public F4MUserNotFoundException(String message) {
        super(PaymentServiceExceptionCodes.ERR_ACCOUNT_NOT_FOUND, message);
    }

}
