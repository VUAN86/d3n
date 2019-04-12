package de.ascendro.f4m.service.game.selection.model.game;

import java.util.HashMap;
import java.util.Map;

public enum JokerType {
	HINT("HINT", null), 
	FIFTY_FIFTY("FIFTY_FIFTY", null),
	SKIP("SKIP", 5),
	IMMEDIATE_ANSWER("IMMEDIATE_ANSWER", 1);
	
	private static Map<String, JokerType> values = new HashMap<>(values().length);
	static {
		for (JokerType value : values()) {
			values.put(value.getValue(), value);
		}
	}
	
	private String value;
	private Integer maxLimit;
	
	private JokerType(String value, Integer maxLimit) {
		this.value = value;
		this.maxLimit = maxLimit;
	}
	
	public String getValue() {
		return value;
	}
	
	public Integer getMaxLimit() {
		return maxLimit;
	}
	
	public static JokerType fromString(String value) {
		return values.get(value);
	}

}
