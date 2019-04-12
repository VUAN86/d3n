package de.ascendro.f4m.service.exception;

/**
 * Defines common error codes that may be used in every service. Each service may subclass this to define service
 * specific error codes.
 * 
 * Business specific codes should be negative to avoid clashes with HTTP status codes used for standard error messages.
 */
public class ExceptionCodes {

	// Server error codes
	public static final String ERR_FATAL_ERROR = "ERR_FATAL_ERROR";
	public static final String ERR_SESSION_STORE_INIT = "ERR_SESSION_STORE_INIT";
	public static final String ERR_SESSION_STORE_DESTROY = "ERR_SESSION_STORE_DESTROY";
	public static final String ERR_CONNECTION_ERROR = "ERR_CONNECTION_ERROR";
	public static final String ERR_SHUTTING_DOWN = "ERR_SHUTTING_DOWN";
	
	// Validation error codes
	public static final String ERR_VALIDATION_FAILED = "ERR_VALIDATION_FAILED";

	// Client error codes
	public static final String ERR_ENTRY_ALREADY_EXISTS = "ERR_ENTRY_ALREADY_EXISTS";
	public static final String ERR_ENTRY_NOT_FOUND = "ERR_ENTRY_NOT_FOUND";
	
	//Auth
	public static final String ERR_INSUFFICIENT_RIGHTS = "ERR_INSUFFICIENT_RIGHTS";
	
	protected ExceptionCodes() {
	}
}
