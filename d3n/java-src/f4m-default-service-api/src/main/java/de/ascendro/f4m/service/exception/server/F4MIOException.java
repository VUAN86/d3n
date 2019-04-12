package de.ascendro.f4m.service.exception.server;

public class F4MIOException extends F4MFatalErrorException {
	
	private static final long serialVersionUID = 2005743446832678028L;

	public F4MIOException(String message, Throwable cause) {
		super(message, cause);
	}

	public F4MIOException(String message) {
		super(message);
	}
	
}
