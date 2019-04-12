package de.ascendro.f4m.server.result;

import java.util.HashMap;
import java.util.Map;

public enum RefundReason {
	NO_OPPONENT("noOpponent"),
	GAME_NOT_FINISHED("gameNotFinished"),
	GAME_WAS_A_PAT("gameWasAPat"),
	GAME_NOT_PLAYED("notPlayed"), 
	RESULT_CALCULATION_FAILED("resultCalculationFailed"),
	BACKEND_FAILED("backendFailed"),
	OTHER("other");
	
	private static Map<String, RefundReason> values = new HashMap<>(values().length);
	static {
		for (RefundReason value : values()) {
			values.put(value.getValue(), value);
		}
	}
	
	private String value;
	
	private RefundReason(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public static RefundReason fromString(String value) {
		return values.get(value);
	}

}
