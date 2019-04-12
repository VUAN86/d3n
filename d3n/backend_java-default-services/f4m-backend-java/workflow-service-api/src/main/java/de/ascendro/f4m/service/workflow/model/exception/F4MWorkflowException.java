package de.ascendro.f4m.service.workflow.model.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MWorkflowException extends F4MClientException {

	private static final long serialVersionUID = 2110166457721783447L;

	public F4MWorkflowException(String code, String message) {
		super(code, message);
	}

}
