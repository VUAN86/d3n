package de.ascendro.f4m.service.game.selection.model.game;

import java.util.HashMap;
import java.util.Map;

public enum DuelGroupWinnerPayoutType {
    FIXED_PAYOUT_VALUE("fixedPayoutValue"),
    CALCULATE_VALUE_OF_JACKPOT("calculateValueOfJackpot"),
    NOT_PAID_OUT("notPaidOut");
	
	private static Map<String, DuelGroupWinnerPayoutType> values = new HashMap<>(values().length);
	static {
		for (DuelGroupWinnerPayoutType value : values()) {
			values.put(value.getValue(), value);
		}
	}
	
	private String value;
	
	private DuelGroupWinnerPayoutType(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public static DuelGroupWinnerPayoutType fromString(String value) {
		return values.get(value);
	}
}
