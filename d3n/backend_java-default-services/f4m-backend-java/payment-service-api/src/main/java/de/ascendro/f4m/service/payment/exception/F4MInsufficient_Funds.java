package de.ascendro.f4m.service.payment.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MInsufficient_Funds extends F4MClientException {
    public F4MInsufficient_Funds(String message) {
        super(PaymentServiceExceptionCodes.ERR_INSUFFICIENT_FUNDS, message);
    }

}
