package de.ascendro.f4m.service.exception.client;

import de.ascendro.f4m.service.exception.ExceptionCodes;

public class F4MEntryNotFoundException extends F4MClientException {

    private static final long serialVersionUID = 5636854564509582988L;

    public F4MEntryNotFoundException() {
        super(ExceptionCodes.ERR_ENTRY_NOT_FOUND, "Record not found");
    }

    public F4MEntryNotFoundException(String message) {
        super(ExceptionCodes.ERR_ENTRY_NOT_FOUND, message);
    }

    public F4MEntryNotFoundException(String message, Throwable cause) {
        super(ExceptionCodes.ERR_ENTRY_NOT_FOUND, message, cause);
    }
}
