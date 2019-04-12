package de.ascendro.f4m.service.json.model.type;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public interface JsonMessageTypeMap {

	Type register(MessageType key, TypeToken<? extends JsonMessageContent> value);
	
	Type register(String key, Type value);
	
	Type register(MessageType key, Type value);

	Type get(String key);

	Map<String, Type> getEntries();

}
