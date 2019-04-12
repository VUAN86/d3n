package de.ascendro.f4m.service.payment.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MNoCurrencyRateException extends F4MClientException {
	private static final long serialVersionUID = 7410085945771095461L;

	public F4MNoCurrencyRateException(String message) {
		super(PaymentServiceExceptionCodes.NO_EXCHANGE_RATE, message);
	}

}
