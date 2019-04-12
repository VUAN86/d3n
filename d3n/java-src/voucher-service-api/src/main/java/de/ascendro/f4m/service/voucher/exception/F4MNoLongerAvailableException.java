package de.ascendro.f4m.service.voucher.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MNoLongerAvailableException extends F4MClientException {

	private static final long serialVersionUID = 5636854564509582988L;

	public F4MNoLongerAvailableException(String message) {
		super(VoucherServiceExceptionCodes.ERR_NO_LONGER_AVAILABLE, message);
	}

}
