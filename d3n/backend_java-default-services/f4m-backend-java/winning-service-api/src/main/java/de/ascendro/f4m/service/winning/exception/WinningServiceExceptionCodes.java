package de.ascendro.f4m.service.winning.exception;

import de.ascendro.f4m.service.exception.ExceptionCodes;

/**
 * Winning service specific error codes.
 */
public class WinningServiceExceptionCodes extends ExceptionCodes {
	
	// Client error codes
	public static final String ERR_COMPONENT_ALREADY_USED = "ERR_COMPONENT_ALREADY_USED";
	public static final String ERR_COMPONENT_NOT_AVAILABLE = "ERR_COMPONENT_NOT_AVAILABLE";
	public static final String ERR_THE_REQUESTED_WINNING_COMPONENT_NOT_AVAILABLE = " ERR_THE_REQUESTED_WINNING_COMPONENT_IS_NOT_AVAILABLE_FOR_THE_CURRENT_RESULT.";
	
}
