package de.ascendro.f4m.service.workflow.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserTaskTriggeredEvent implements JsonMessageContent {

	private String taskType;
	private String taskId;
	private String triggeredAction;
	
	public UserTaskTriggeredEvent() {
		// Initialize empty content
	}
	
	public UserTaskTriggeredEvent(String taskType, String taskId, String triggeredAction) {
		this.taskType = taskType;
		this.taskId = taskId;
		this.triggeredAction = triggeredAction;
	}
	
	public String getTaskType() {
		return taskType;
	}
	
	public String getTaskId() {
		return taskId;
	}
	
	public String getTriggeredAction() {
		return triggeredAction;
	}
	
}
