package de.ascendro.f4m.service.payment.exception;

import de.ascendro.f4m.service.exception.server.F4MServerException;

public class F4MLoadOfCoverageException extends F4MServerException {
    public F4MLoadOfCoverageException(String message) {
        super(PaymentServiceExceptionCodes.ERR_LOAD_OR_WITHDRAW_WITHOUT_COVERAGE, message);
    }
}
