package de.ascendro.f4m.service.payment.exception;

import de.ascendro.f4m.service.exception.ExceptionCodes;

public class PaymentServiceExceptionCodes extends ExceptionCodes {
	public static final String ERR_INSUFFICIENT_FUNDS = "ERR_INSUFFICIENT_FUNDS";
	public static final String ERR_MONEY_ACCOUNT_NOT_INITIALIZED = "ERR_MONEY_ACCOUNT_NOT_INITIALIZED";
	public static final String NO_EXCHANGE_RATE = "ERR_NO_EXCHANGE_RATE";
	public static final String ERR_INVALID_VALUE = "ERR_INVALID_VALUE";
	public static final String ERR_REQUIRED_PROPERTY_MISSING = "ERR_REQUIRED_PROPERTY_MISSING";
	public static final String ERR_ACCOUNT_NOT_FOUND = "ERR_ACCOUNT_NOT_FOUND";
	public static final String ERR_ACCOUNT_STATE_NOT_CHANGEABLE = "ERR_ACCOUNT_STATE_NOT_CHANGEABLE";
	public static final String ERR_ACCOUNT_NOT_INTERNAL = "ERR_ACCOUNT_NOT_INTERNAL";
	public static final String ERR_ACCOUNT_ONLY_FOR_TRANSFER = "ERR_ACCOUNT_ONLY_FOR_TRANSFER";
	public static final String ERR_ACCOUNT_IS_CREDIT_ONLY = "ERR_ACCOUNT_IS_CREDIT_ONLY";
	public static final String ERR_ACCOUNTTRANSACTION_NOT_FOUND = "ERR_ACCOUNTTRANSACTION_NOT_FOUND";
	public static final String ERR_ACCOUNTTRANSACTION_FAILED = "ERR_ACCOUNTTRANSACTION_FAILED";
	public static final String ERR_USER_NOT_FOUND = "ERR_USER_NOT_FOUND";
	public static final String ERR_USER_DISABLED = "ERR_USER_DISABLED";
	public static final String ERR_USER_NOT_IDENTIFIED = "ERR_USER_NOT_IDENTIFIED";
	public static final String ERR_USER_IS_UNDERAGED = "ERR_USER_IS_UNDERAGED";
	public static final String ERR_IDENTIFICATION_NOT_FOUND = "ERR_IDENTIFICATION_NOT_FOUND";
	public static final String ERR_IDENTIFICATION_ALREADY_PROCESSED = "ERR_IDENTIFICATION_ALREADY_PROCESSED";
	public static final String ERR_IDENTIFICATION_NOT_POSSIBLE = "ERR_IDENTIFICATION_NOT_POSSIBLE";
	public static final String ERR_PAYMENTTRANSACTION_NOT_FOUND = "ERR_PAYMENTTRANSACTION_NOT_FOUND";
	public static final String ERR_CASHOUT_LIMIT_REACHED_FOR_IDENTIFICATION_TYPE = "ERR_CASHOUT_LIMIT_REACHED_FOR_IDENTIFICATION_TYPE";
	public static final String ERR_CASHOUT_LIMIT_NOT_SET = "ERR_CASHOUT_LIMIT_NOT_SET";
	public static final String ERR_NOT_FOUND = "ERR_NOT_FOUND";
	public static final String ERR_GAME_CLOSED = "ERR_GAME_CLOSED";
	public static final String ERR_TRANSFER_JACKPOT = "ERR_TRANSFER_JACKPOT";
	public static final String ERR_CREATE_JACKPOT = "ERR_CREATE_JACKPOT";
	public static final String ERR_CLOSE_JACKPOT = "ERR_CLOSE_JACKPOT";
	public static final String ERR_GET_JACKPOT = "ERR_GET_JACKPOT";
	public static final String ERR_TRANSFER_BETWEEN = "ERR_TRANSFER_BETWEEN";
	public static final String ERR_LOAD_OR_WITHDRAW_WITHOUT_COVERAGE = "ERR_LOAD_OR_WITHDRAW_WITHOUT_COVERAGE";
}
