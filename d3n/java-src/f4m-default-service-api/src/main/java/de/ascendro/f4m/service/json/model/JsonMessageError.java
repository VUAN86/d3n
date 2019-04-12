package de.ascendro.f4m.service.json.model;

import com.google.gson.annotations.SerializedName;

import de.ascendro.f4m.service.exception.ExceptionType;
import de.ascendro.f4m.service.exception.F4MException;

public class JsonMessageError {
	private final String type;

	@SerializedName(value = "message")
	private final String code;

	public JsonMessageError(ExceptionType type, String code) {
		this.type = type.name().toLowerCase();
		this.code = code;
	}

	public JsonMessageError(F4MException f4mException) {
		this(f4mException.getType(), f4mException.getCode());
	}

	public String getCode() {
		return code;
	}

	public String getType() {
		return type;
	}

	@Override
	public String toString() {
		return "JsonMessageError[" + type + " : " + code + "]";
	}
}
