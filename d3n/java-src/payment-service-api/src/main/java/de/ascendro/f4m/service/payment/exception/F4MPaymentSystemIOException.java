package de.ascendro.f4m.service.payment.exception;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;

public class F4MPaymentSystemIOException extends F4MFatalErrorException {

	private static final long serialVersionUID = 5636854564509582988L;

	public F4MPaymentSystemIOException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
