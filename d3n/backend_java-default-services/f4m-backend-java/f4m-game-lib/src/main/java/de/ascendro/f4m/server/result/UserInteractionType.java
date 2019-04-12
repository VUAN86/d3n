package de.ascendro.f4m.server.result;

import java.util.HashMap;
import java.util.Map;

public enum UserInteractionType {

	BONUS_POINT_TRANSFER("bonusPointTransfer"),
	BONUS_POINT_ENQUIRY("bonusPointEnquiry"),
	HANDICAP_ADJUSTMENT("handicapAdjustment"),
	SPECIAL_PRIZE("specialPrize"),
	BUY_WINNING_COMPONENT("buyWinningComponent");

	private static Map<String, UserInteractionType> values = new HashMap<>(values().length);
	static {
		for (UserInteractionType value : values()) {
			values.put(value.getValue(), value);
		}
	}
	
	private String value;
	
	private UserInteractionType(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public static UserInteractionType fromString(String value) {
		return values.get(value);
	}

}
