package de.ascendro.f4m.service.payment.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MPayoutItemException  extends F4MClientException {
    public F4MPayoutItemException(String message) {
        super(PaymentServiceExceptionCodes.ERR_INVALID_VALUE, message);

    }
}
