package de.ascendro.f4m.service.payment.rest.wrapper;

import de.ascendro.f4m.service.payment.exception.PaymentServiceExceptionCodes;

public enum PaymentExternalErrorCodes {

	UNDEFINED_ERROR("100"), //
	ACCESS_DENIED("101"), //
	NOT_IMPLEMENTED("102"), //
	INVALID_CALLBACKURL("103"), //
	INVALID_VALUE("104", PaymentServiceExceptionCodes.ERR_INVALID_VALUE), //
	REQUIRED_PROPERTY_MISSING("105", PaymentServiceExceptionCodes.ERR_REQUIRED_PROPERTY_MISSING), //
	ACCOUNT_NOT_FOUND("200", PaymentServiceExceptionCodes.ERR_ACCOUNT_NOT_FOUND), //
	ACCOUNT_NOT_COVERED("201", PaymentServiceExceptionCodes.ERR_INSUFFICIENT_FUNDS), //
	ACCOUNT_NOT_CLOSABLE("202"), //
	ACCOUNT_STATE_NOT_CHANGEABLE("203", PaymentServiceExceptionCodes.ERR_ACCOUNT_STATE_NOT_CHANGEABLE), //
	ACCOUNT_NOT_INTERNAL("204", PaymentServiceExceptionCodes.ERR_ACCOUNT_NOT_INTERNAL), //
	ACCOUNT_ONLY_FOR_TRANSFER("205", PaymentServiceExceptionCodes.ERR_ACCOUNT_ONLY_FOR_TRANSFER), //
	ACCOUNT_IS_CREDIT_ONLY("206", PaymentServiceExceptionCodes.ERR_ACCOUNT_IS_CREDIT_ONLY), //
	ACCOUNTTRANSACTION_NOT_FOUND("300", PaymentServiceExceptionCodes.ERR_ACCOUNTTRANSACTION_NOT_FOUND), //
	ACCOUNTTRANSACTION_FAILED("301", PaymentServiceExceptionCodes.ERR_ACCOUNTTRANSACTION_FAILED), //
	CURRENCY_NOT_FOUND("400"), //
	USER_NOT_FOUND("500", PaymentServiceExceptionCodes.ERR_USER_NOT_FOUND), //
	USER_DISABLED("501", PaymentServiceExceptionCodes.ERR_USER_DISABLED), //
	USER_NOT_IDENTIFIED("502", PaymentServiceExceptionCodes.ERR_USER_NOT_IDENTIFIED), //
	USER_IS_UNDERAGED("503", PaymentServiceExceptionCodes.ERR_USER_IS_UNDERAGED), //
	EXCHANGERATE_NOT_FOUND("600"), //
	EXCHANGERATE_ALREADY_EXISTS("601"), //
	EXCHANGERATE_NOT_POSITIVE("602"), //
	IDENTIFICATION_NOT_FOUND("700", PaymentServiceExceptionCodes.ERR_IDENTIFICATION_NOT_FOUND), //
	IDENTIFICATION_ALREADY_PROCESSED("701", PaymentServiceExceptionCodes.ERR_IDENTIFICATION_ALREADY_PROCESSED), //
	IDENTIFICATION_NOT_POSSIBLE("702", PaymentServiceExceptionCodes.ERR_IDENTIFICATION_NOT_POSSIBLE), //
	PAYMENTTRANSACTION_NOT_FOUND("800", PaymentServiceExceptionCodes.ERR_PAYMENTTRANSACTION_NOT_FOUND), //
	CASHOUT_LIMIT_REACHED_FOR_IDENTIFICATION_TYPE("801", PaymentServiceExceptionCodes.ERR_CASHOUT_LIMIT_REACHED_FOR_IDENTIFICATION_TYPE), //
	CASHOUT_LIMIT_NOT_SET("802", PaymentServiceExceptionCodes.ERR_CASHOUT_LIMIT_NOT_SET), //
	GAME_NOT_FOUND("900", PaymentServiceExceptionCodes.ERR_NOT_FOUND), //
	GAME_CLOSED("901", PaymentServiceExceptionCodes.ERR_GAME_CLOSED);

	private String code;
	private String f4mCode;

	private PaymentExternalErrorCodes(String code) {
		this.code = code;
	}

	private PaymentExternalErrorCodes(String code, String f4mCode) {
		this.code = code;
		this.f4mCode = f4mCode;
	}

	public String getCode() {
		return code;
	}

	public String getF4MCode() {
		return f4mCode;
	}

	public static PaymentExternalErrorCodes byCode(String code) {
		PaymentExternalErrorCodes externalErrorCode = null;
		for (PaymentExternalErrorCodes errCode : PaymentExternalErrorCodes.values()) {
			if (errCode.getCode().equals(code)) {
				externalErrorCode = errCode;
				break;
			}
		}
		return externalErrorCode;
	}

	public static boolean containsCode(String code) {
		return PaymentExternalErrorCodes.byCode(code) != null;
	}

}
