package de.ascendro.f4m.service.workflow.model.state;

import java.util.Collection;

import de.ascendro.f4m.service.workflow.model.WorkflowResponse;

public class StateResponse extends WorkflowResponse {

	public StateResponse() {
		// Initialize empty object
	}
	
	public StateResponse(Collection<String> availableActionTypes, boolean processFinished) {
		super(availableActionTypes, processFinished);
	}

}
