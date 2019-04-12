package de.ascendro.f4m.service.game.engine.exception;

import de.ascendro.f4m.service.exception.ExceptionCodes;

/**
 * Voucher service specific error codes.
 */
public class GameEngineExceptionCodes extends ExceptionCodes {

	// Client error codes
	public static final String ERR_GAME_NOT_AVAILABLE = "ERR_GAME_NOT_AVAILABLE";
	public static final String ERR_GAME_REQUIRE_ENTRY_FEE = "ERR_GAME_REQUIRE_ENTRY_FEE";
	public static final String ERR_GAME_CANCELLED = "ERR_GAME_CANCELLED";
	public static final String ERR_GAME_FLOW_VIOLATION = "ERR_GAME_FLOW_VIOLATION";
	public static final String ERR_GAME_QUESTION_ALREADY_ANSWERED = "ERR_GAME_QUESTION_ALREADY_ANSWERED";
	public static final String ERR_UNEXPECTED_GAME_QUESTION_ANSWERED = "ERR_UNEXPECTED_GAME_QUESTION_ANSWERED";
	public static final String ERR_JOKER_NOT_AVAILABLE = "ERR_JOKER_NOT_AVAILABLE";
	public static final String ERR_ALLOWED_GAME_PLAYS_EXHAUSTED = "ERR_ALLOWED_GAME_PLAYS_EXHAUSTED";
	
	//Server error codes
	public static final String ERR_QUESTION_CANNOT_BE_READ_FROM_POOL = "ERR_QUESTION_CANNOT_BE_READ_FROM_POOL";
	public static final String ERR_QUESTIONS_NOT_AVAILABLE_IN_POOL = "ERR_QUESTIONS_NOT_AVAILABLE_IN_POOL";
	
}
