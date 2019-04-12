package de.ascendro.f4m.service.exception;

public class F4MShuttingDownException extends F4MException {

	private static final long serialVersionUID = 6927146142092326731L;

	public F4MShuttingDownException(Throwable cause) {
		super(ExceptionType.SERVER, ExceptionCodes.ERR_SHUTTING_DOWN, "Shutting down", cause);
	}

}
