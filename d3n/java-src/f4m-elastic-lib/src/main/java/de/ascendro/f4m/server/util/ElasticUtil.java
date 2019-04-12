package de.ascendro.f4m.server.util;
import java.time.ZonedDateTime;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import de.ascendro.f4m.service.util.DateTimeUtil;

public class ElasticUtil {

	private static final String PATH_SEPARATOR = ".";
	
	private Gson gson;

	public ElasticUtil() {
		gson = new Gson();
	}

	public <T> T fromJson(String jsonString, Class<T> jsonStringClass) {
		return gson.fromJson(jsonString, jsonStringClass);
	}

	public <T extends JsonElement> String toJson(T jsonElement, Class<T> type) {
		return gson.toJson(jsonElement, type);
	}
	
	public static String buildPath(String... properties) {
		return StringUtils.join(properties, PATH_SEPARATOR);
	}

	public static JsonElement convertToJsonElement(Object value) {
		if (value == null) {
			return null;
		} else if (value instanceof String) {
			return new JsonPrimitive((String) value);
		} else if (value instanceof Boolean) {
			return new JsonPrimitive((Boolean) value);
		} else if (value instanceof JsonElement) {
			return (JsonElement) value;
		} else if (value instanceof ZonedDateTime) {
			return new JsonPrimitive(DateTimeUtil.formatISODateTime((ZonedDateTime) value));
		} else if (value instanceof Number) {
			return new JsonPrimitive((Number) value);
		} else {
			throw new UnsupportedOperationException("Unknown value type: " + value.getClass());
		}
	}

	public <T extends JsonElement> T copyJson(T jsonElement, Class<T> type) {
		return fromJson(toJson(jsonElement, type), type);
	}
	
}
