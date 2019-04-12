package de.ascendro.f4m.service.workflow.model.start;

import java.util.Collection;

import de.ascendro.f4m.service.workflow.model.WorkflowResponse;

public class StartResponse extends WorkflowResponse {

	public StartResponse() {
		// Initialize empty object
	}
	
	public StartResponse(Collection<String> availableActionTypes, boolean processFinished) {
		super(availableActionTypes, processFinished);
	}

}
