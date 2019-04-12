package de.ascendro.f4m.service.result.engine.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MWinningComponentPurchasePending extends F4MClientException {

	private static final long serialVersionUID = 1195973376799076219L;

	public F4MWinningComponentPurchasePending(String message) {
		super(ResultEngineExceptionCodes.ERR_WINNING_COMPONENT_PURCHASE_PENDING, message);
	}

}
