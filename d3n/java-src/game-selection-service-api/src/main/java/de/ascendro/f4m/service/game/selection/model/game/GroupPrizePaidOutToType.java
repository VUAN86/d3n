package de.ascendro.f4m.service.game.selection.model.game;

import java.util.HashMap;
import java.util.Map;

public enum GroupPrizePaidOutToType {
	ALL_WINNERS("allWinners"),
	PARTS_OF_THE_WINNERS("partsOfTheWinners");
	
	private static Map<String, GroupPrizePaidOutToType> values = new HashMap<>(values().length);
	static {
		for (GroupPrizePaidOutToType value : values()) {
			values.put(value.getValue(), value);
		}
	}
	
	private String value;
	
	private GroupPrizePaidOutToType(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public static GroupPrizePaidOutToType fromString(String value) {
		return values.get(value);
	}
}
