package de.ascendro.f4m.service.voucher.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MNoVoucherAvailableException extends F4MClientException {

	private static final long serialVersionUID = 5636854564509582988L;

	public F4MNoVoucherAvailableException(String message) {
		super(VoucherServiceExceptionCodes.ERR_NO_VOUCHER_AVAILABLE, message);
	}

}
