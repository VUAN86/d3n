package de.ascendro.f4m.service.payment.rest.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum AccountState {
	@JsonProperty("1")
	ENABLED("1"),
	@JsonProperty("2")
	DISABLED("2"),
	@JsonProperty("4")
	CONVERTED("4"),
	@JsonProperty("8")
	CLOSED("8"),
	@JsonProperty("16")
	MERGED("16");
	
	private static Map<String, AccountState> values = new HashMap<>(values().length);
	static {
		for (AccountState item : values()) {
			values.put(item.getValue(), item);
		}
	}

	private String value;
	
	private AccountState(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	//JsonProperty is ignored when parsing from int (used only to serialize), see com.fasterxml.jackson.databind.deser.std.EnumDeserializer.deserialize
	@JsonCreator
	public static AccountState fromString(String value) {
		return values.get(value);
	}
}
