package de.ascendro.f4m.service.voucher.exception;

import de.ascendro.f4m.service.exception.ExceptionCodes;

/**
 * Voucher service specific error codes.
 */
public class VoucherServiceExceptionCodes extends ExceptionCodes {

	// Client error codes
	public static final String ERR_VOUCHER_ALREADY_USED = "ERR_VOUCHER_ALREADY_USED";
	public static final String ERR_NO_LONGER_AVAILABLE = "ERR_NO_LONGER_AVAILABLE";
	public static final String ERR_VOUCHER_IS_NOT_EXCHANGE = "ERR_VOUCHER_IS_NOT_EXCHANGE";
	public static final String ERR_NO_VOUCHER_AVAILABLE = "ERR_NO_VOUCHER_AVAILABLE";

}
