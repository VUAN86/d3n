package de.ascendro.f4m.service.promocode.rest.wrapper;

import java.util.HashMap;
import java.util.Map;


public enum PromoResponseType {
	// Overall items
	NO_KEY_GIVEN("NO_KEY_GIVEN"),
	INVALID("INVALID"),
	VALID("VALID");

	private static Map<String, PromoResponseType> values = new HashMap<>(values().length);
	static {
		for (PromoResponseType value : values()) {
			values.put(value.getValue(), value);
		}
	}

	private String value;

	private PromoResponseType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public static PromoResponseType fromString(String value) {
		return values.get(value);
	}
}