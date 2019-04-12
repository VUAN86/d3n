package de.ascendro.f4m.service.registry.exception;

import de.ascendro.f4m.service.exception.server.F4MServerException;

public class F4MServiceConnectionInformationNotFoundException extends F4MServerException {

	private static final long serialVersionUID = -1643267904438164214L;

	public F4MServiceConnectionInformationNotFoundException(String message) {
		super(ServiceRegistryExceptionCodes.ERR_SERVICE_CONNECTION_INFORMATION_NOT_FOUND, message);
	}

	public F4MServiceConnectionInformationNotFoundException(String message, Throwable cause) {
		super(ServiceRegistryExceptionCodes.ERR_SERVICE_CONNECTION_INFORMATION_NOT_FOUND, message, cause);
	}

}