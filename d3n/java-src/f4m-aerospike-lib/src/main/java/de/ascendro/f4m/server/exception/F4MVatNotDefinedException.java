package de.ascendro.f4m.server.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MVatNotDefinedException extends F4MClientException {
	private static final long serialVersionUID = -3894264627594023587L;

	public F4MVatNotDefinedException(String message) {
		super(AerospikeLibExceptionCodes.ERR_VAT_NOT_DEFINED, message);
	}

}
