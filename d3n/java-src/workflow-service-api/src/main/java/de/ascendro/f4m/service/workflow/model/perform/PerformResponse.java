package de.ascendro.f4m.service.workflow.model.perform;

import java.util.Collection;

import de.ascendro.f4m.service.workflow.model.WorkflowResponse;

public class PerformResponse extends WorkflowResponse {

	private String triggeredActionType;
	
	public PerformResponse() {
		// Initialize empty object
	}
	
	public PerformResponse(String triggeredActionType, Collection<String> availableActionTypes, boolean processFinished) {
		super(availableActionTypes, processFinished);
		this.triggeredActionType = triggeredActionType;
	}
	
	public String getTriggeredActionType() {
		return triggeredActionType;
	}

}
