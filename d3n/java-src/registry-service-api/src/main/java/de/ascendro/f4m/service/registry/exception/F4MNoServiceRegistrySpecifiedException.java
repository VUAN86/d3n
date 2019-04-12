package de.ascendro.f4m.service.registry.exception;

import de.ascendro.f4m.service.exception.server.F4MServerException;

/**
 * No Service Registry URI specified within service store.
 */
public class F4MNoServiceRegistrySpecifiedException extends F4MServerException {
	
	private static final long serialVersionUID = 8774780952766478172L;

	public F4MNoServiceRegistrySpecifiedException(String message, Throwable cause) {
		super(ServiceRegistryExceptionCodes.ERR_NO_SERVICE_REGISTRY_SPECIFIED, message, cause);
	}

	public F4MNoServiceRegistrySpecifiedException(String message) {
		this(message, null);
	}

}
