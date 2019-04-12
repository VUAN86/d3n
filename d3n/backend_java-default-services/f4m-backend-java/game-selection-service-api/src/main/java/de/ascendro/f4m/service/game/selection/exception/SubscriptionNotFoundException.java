package de.ascendro.f4m.service.game.selection.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class SubscriptionNotFoundException extends F4MClientException{

	private static final long serialVersionUID = -3155345737079589498L;

	public SubscriptionNotFoundException(String message) {
		super(GameSelectionExceptionCodes.ERR_SUBSCRIPTION_NOT_FOUND, message);
	}

}
