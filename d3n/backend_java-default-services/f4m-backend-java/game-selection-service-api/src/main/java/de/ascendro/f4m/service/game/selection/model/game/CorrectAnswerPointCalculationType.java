package de.ascendro.f4m.service.game.selection.model.game;

import java.util.HashMap;
import java.util.Map;

public enum CorrectAnswerPointCalculationType {
	BASED_ON_QUESTION_COMMPLEXITY("basedOnQuestionCommplexity"),
	BASED_ON_FIX_VALUE("basedOnFixValue"),
	DONT_USE("dontUse");
	
	private static Map<String, CorrectAnswerPointCalculationType> values = new HashMap<>(values().length);
	static {
		for (CorrectAnswerPointCalculationType value : values()) {
			values.put(value.getValue(), value);
		}
	}
	
	private String value;
	
	private CorrectAnswerPointCalculationType(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public static CorrectAnswerPointCalculationType fromString(String value) {
		return values.get(value);
	}
}
