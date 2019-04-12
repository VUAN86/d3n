package de.ascendro.f4m.service.workflow.model.start;

import de.ascendro.f4m.service.workflow.model.WorkflowRequest;

public class StartRequest extends WorkflowRequest {

	private String taskType;
	private String description;

	public String getTaskType() {
		return taskType;
	}

	public String getDescription() {
		return description;
	}

}
