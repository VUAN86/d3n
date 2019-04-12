package de.ascendro.f4m.service.workflow.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class WorkflowResponse implements JsonMessageContent {

	private List<String> availableActionTypes;
	private boolean processFinished;
	
	protected WorkflowResponse() {
	}
	
	public WorkflowResponse(Collection<String> availableActionTypes, boolean processFinished) {
		this.availableActionTypes = new ArrayList<>(availableActionTypes);
		this.processFinished = processFinished;
	}

	public List<String> getAvailableActionTypes() {
		return availableActionTypes;
	}

	public boolean isProcessFinished() {
		return processFinished;
	}
	
}
