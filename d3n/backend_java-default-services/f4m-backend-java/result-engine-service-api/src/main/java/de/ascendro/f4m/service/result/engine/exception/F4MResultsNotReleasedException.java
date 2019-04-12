package de.ascendro.f4m.service.result.engine.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MResultsNotReleasedException extends F4MClientException {

	private static final long serialVersionUID = 1195973376799076219L;

	public F4MResultsNotReleasedException(String message) {
		super(ResultEngineExceptionCodes.ERR_RESULTS_NOT_RELEASED, message);
	}

}
