package de.ascendro.f4m.service.exception.client;

import de.ascendro.f4m.service.exception.ExceptionCodes;

public class F4MEntryAlreadyExistsException extends F4MClientException {

	private static final long serialVersionUID = -7462395484053811819L;

	public F4MEntryAlreadyExistsException(String message) {
		super(ExceptionCodes.ERR_ENTRY_ALREADY_EXISTS, message);
	}

}
