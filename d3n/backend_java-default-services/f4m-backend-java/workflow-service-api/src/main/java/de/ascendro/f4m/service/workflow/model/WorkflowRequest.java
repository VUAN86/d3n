package de.ascendro.f4m.service.workflow.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public abstract class WorkflowRequest implements JsonMessageContent {

    private String taskId;
	private String userId;
	private String tenantId;
    private JsonObject parameters;

	public String getTaskId() {
		return taskId;
	}

	public String getUserId() {
		return userId;
	}
	
	public String getTenantId() {
		return tenantId;
	}

	public Map<String, Object> getParameters() {
		if (parameters != null && ! parameters.isJsonNull() && ! parameters.entrySet().isEmpty()) {
			Map<String, Object> properties = new HashMap<>(parameters.entrySet().size());
			parameters.entrySet().forEach(entry -> {
				Object value = null;
				if (entry.getValue() != null && ! entry.getValue().isJsonNull()) {
					if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isBoolean()) {
						value = entry.getValue().getAsBoolean();
					} else {
						value = entry.getValue().getAsString();
					}
				}
				properties.put(entry.getKey(), value);
			});
			return properties;
		} else {
			return Collections.emptyMap();
		}
	}

}
