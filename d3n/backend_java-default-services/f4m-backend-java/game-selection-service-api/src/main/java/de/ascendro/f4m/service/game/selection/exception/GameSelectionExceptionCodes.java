package de.ascendro.f4m.service.game.selection.exception;

import de.ascendro.f4m.service.exception.ExceptionCodes;

public class GameSelectionExceptionCodes extends ExceptionCodes {
	
	public static final String ERR_GAME_PARTICIPANT_COUNT_EXCEEDED = "ERR_GAME_PARTICIPANT_COUNT_EXCEEDED";
	public static final String ERR_GAME_ALREADY_STARTED_BY_USER = "ERR_GAME_ALREADY_STARTED_BY_USER";
	public static final String ERR_GAME_ALREADY_PLAYED_BY_USER = "ERR_GAME_ALREADY_PLAYED_BY_USER";
	public static final String ERR_SUBSCRIPTION_NOT_FOUND = "ERR_SUBSCRIPTION_NOT_FOUND";
	public static final String ERR_GAME_INVITATION_NOT_VALID = "ERR_GAME_INVITATION_NOT_VALID";
	public static final String ERR_GAME_TYPE_NOT_VALID = "ERR_GAME_TYPE_NOT_VALID";
	
	// validation errors for custom game parameters
	public static final String ERR_POOLS_NOT_VALID = "ERR_POOLS_NOT_VALID";
	public static final String ERR_NUMBER_OF_QUESTIONS_NOT_VALID = "ERR_NUMBER_OF_QUESTIONS_NOT_VALID";
	public static final String ERR_MAX_PARTICIPANT_COUNT_NOT_VALID = "ERR_MAX_PARTICIPANT_COUNT_NOT_VALID";
	public static final String ERR_ENTRY_FEE_NOT_VALID = "ERR_ENTRY_FEE_NOT_VALID";
	public static final String ERR_DATES_NOT_VALID = "ERR_DATES_NOT_VALID";
	public static final String ERR_MODE_NOT_VALID = "ERR_MODE_NOT_VALID";
	public static final String GAME_HASNT_STARTED_YET = "GAME_HASNT_STARTED_YET";
	public static final String GAME_IS_OVER = "GAME_IS_OVER";

}
