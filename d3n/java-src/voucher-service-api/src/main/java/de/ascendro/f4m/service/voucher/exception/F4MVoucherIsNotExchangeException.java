package de.ascendro.f4m.service.voucher.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MVoucherIsNotExchangeException extends F4MClientException {

	private static final long serialVersionUID = 5636854564509582988L;

	public F4MVoucherIsNotExchangeException(String message) {
		super(VoucherServiceExceptionCodes.ERR_VOUCHER_IS_NOT_EXCHANGE, message);
	}

}
