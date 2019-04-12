package de.ascendro.f4m.service.payment.exception;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;

public class F4MNoCurrencyException extends F4MFatalErrorException {

	private static final long serialVersionUID = 7410085945771095461L;

	public F4MNoCurrencyException(String message) {
		super(message);
	}

	public F4MNoCurrencyException(String message, Throwable cause) {
		super(message, cause);
	}

}
