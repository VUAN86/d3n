package de.ascendro.f4m.service.payment.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MMoneyAccountNotInitializedException extends F4MClientException {
	private static final long serialVersionUID = -3913414397765023321L;

	public F4MMoneyAccountNotInitializedException(String message) {
		super(PaymentServiceExceptionCodes.ERR_MONEY_ACCOUNT_NOT_INITIALIZED, message);
	}
	
}
