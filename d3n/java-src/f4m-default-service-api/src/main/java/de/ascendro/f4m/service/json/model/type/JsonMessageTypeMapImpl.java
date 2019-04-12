package de.ascendro.f4m.service.json.model.type;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class JsonMessageTypeMapImpl extends HashMap<String, Type> implements JsonMessageTypeMap {
	private static final long serialVersionUID = -5328805275763892363L;

	private static final Logger LOGGER = LoggerFactory.getLogger(JsonMessageTypeMapImpl.class);

	@Override
	public Type register(String key, Type value) {
		final Type result;

		if (key != null) {
			result = super.put(key, value);
		} else {
			result = null;
		}

		return result;
	}

	@Override
	public Type register(MessageType key, TypeToken<? extends JsonMessageContent> value) {
		return register(key.getMessageName(), value.getType());
	}

	@Override
	public Type register(MessageType key, Type value) {
		return register(key.getMessageName(), value);
	}
	
	@Override
	public Type get(String key) {
		final Type result;

		if (key != null) {
			result = super.get(key);
		} else {
			result = null;
		}

		return result;
	}

	protected void init(JsonMessageTypeMap inheritedMapper) {
		if (inheritedMapper != null) {
			inheritedMapper.getEntries().forEach((k, v) -> register(k, v));
		} else {
			LOGGER.warn("Tried to initialize inherited JSON type mapper, but null was provided");
		}
	}

	@Override
	public Map<String, Type> getEntries() {
		final Map<String, Type> entries = new HashMap<>();

		entrySet().stream().forEach(e -> entries.put(e.getKey(), e.getValue()));

		return entries;
	}
}
