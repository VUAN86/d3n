package de.ascendro.f4m.service.json.model;

import java.util.HashMap;
import java.util.Map;

public enum EventLogType {
	ERROR("error"), 
	WARNING("warning"), 
	INFO("info");
	
	private static Map<String, EventLogType> values = new HashMap<>(values().length);
	static {
		for (EventLogType value : values()) {
			values.put(value.getValue(), value);
		}
	}
	
	private String value;
	
	private EventLogType(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public static EventLogType fromString(String value) {
		return values.get(value);
	}
	
}
