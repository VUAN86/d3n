package de.ascendro.f4m.service.payment;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.payment.rest.model.ErrorInfoRest;

public class F4MPaymentException extends F4MFatalErrorException {
	private static final long serialVersionUID = 2005743446832678028L;
	private ErrorInfoRest errorInfo;

	public F4MPaymentException(ErrorInfoRest errorInfo) {
		super(errorInfo.getMessage() + " - " + errorInfo.getAdditionalMessage());
		this.errorInfo = errorInfo;
	}

	public F4MPaymentException(String message) {
		super(message);
	}

	public F4MPaymentException(String message, Throwable cause) {
		super(message, cause);
	}

	public ErrorInfoRest getErrorInfo() {
		return errorInfo;
	}
}
