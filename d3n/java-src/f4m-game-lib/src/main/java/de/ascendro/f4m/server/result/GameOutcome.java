package de.ascendro.f4m.server.result;

import java.util.HashMap;
import java.util.Map;

public enum GameOutcome {

	WINNER("win"),
	LOSER("lose"),
	TIE("tie"),
	NO_OPPONENT("noOpponent"),
	GAME_NOT_FINISHED("gameNotFinished"),
	UNSPECIFIED("unspecified");
	
	private static Map<String, GameOutcome> values = new HashMap<>(values().length);
	static {
		for (GameOutcome value : values()) {
			values.put(value.getValue(), value);
		}
	}
	
	private String value;
	
	private GameOutcome(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public static GameOutcome fromString(String value) {
		return values.get(value);
	}

}
