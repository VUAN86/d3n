package de.ascendro.f4m.server.analytics.exception;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;

public class F4MAnalyticsFatalErrorException extends F4MFatalErrorException {

    private static final long serialVersionUID = -896413555144248947L;

    public F4MAnalyticsFatalErrorException(String message) {
        super(message);
    }

    public F4MAnalyticsFatalErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
