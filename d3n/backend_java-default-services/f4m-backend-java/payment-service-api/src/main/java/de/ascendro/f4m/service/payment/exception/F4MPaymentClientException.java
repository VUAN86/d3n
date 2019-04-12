package de.ascendro.f4m.service.payment.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MPaymentClientException extends F4MClientException {

	private static final long serialVersionUID = 5636854564509582988L;

	public F4MPaymentClientException(String code, String message) {
		super(code, message);
	}
	
}
