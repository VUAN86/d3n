package de.ascendro.f4m.service.workflow.model.abort;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class AbortRequest implements JsonMessageContent {

	private String taskId;

	public String getTaskId() {
		return taskId;
	}

}
