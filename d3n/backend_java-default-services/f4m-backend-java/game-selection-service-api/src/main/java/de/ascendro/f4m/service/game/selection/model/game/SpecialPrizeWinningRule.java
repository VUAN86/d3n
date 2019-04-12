package de.ascendro.f4m.service.game.selection.model.game;

import java.util.HashMap;
import java.util.Map;

public enum SpecialPrizeWinningRule {

    EVERY_PLAYER("everyPlayer"),
    EVERY_X_PLAYER("everyXPlayer"),
    FIRST_X_PLAYERS("firstXPlayers");
	
	private static Map<String, SpecialPrizeWinningRule> values = new HashMap<>(values().length);
	static {
		for (SpecialPrizeWinningRule value : values()) {
			values.put(value.getValue(), value);
		}
	}
	
	private String value;
	
	private SpecialPrizeWinningRule(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public static SpecialPrizeWinningRule fromString(String value) {
		return values.get(value);
	}
	
}
