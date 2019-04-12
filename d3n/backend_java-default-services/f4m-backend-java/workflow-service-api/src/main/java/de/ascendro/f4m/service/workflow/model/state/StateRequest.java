package de.ascendro.f4m.service.workflow.model.state;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class StateRequest implements JsonMessageContent {

	private String taskId;

	public String getTaskId() {
		return taskId;
	}

}
