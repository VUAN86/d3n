package de.ascendro.f4m.service.payment.exception;

import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;

public class F4MTentantNotFoundException extends F4MEntryNotFoundException {
	private static final long serialVersionUID = -1819295338174440346L;

	public F4MTentantNotFoundException(String message) {
		super(message);
	}

}
