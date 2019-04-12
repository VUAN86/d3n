package de.ascendro.f4m.service.exception;

public abstract class F4MException extends RuntimeException {

	private static final long serialVersionUID = 6927146142092326791L;

	private final String code;
	private final ExceptionType type;

	protected F4MException(ExceptionType type, String code, String message) {
		super(message);
		this.code = code;
		this.type = type;
	}

	public F4MException(ExceptionType type, String code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
		this.type = type;
	}

	public String getCode() {
		return code;
	}

	public ExceptionType getType() {
		return type;
	}
}
