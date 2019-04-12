package de.ascendro.f4m.service.di;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.inject.Inject;
import javax.inject.Provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import de.ascendro.f4m.service.json.JsonMessageDeserializer;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class GsonProvider implements Provider<Gson> {
	private final JsonMessageDeserializer jsonMessageSerializer;

	@Inject
	public GsonProvider(JsonMessageDeserializer jsonMessageSerializer) {
		this.jsonMessageSerializer = jsonMessageSerializer;
	}
	
	public GsonProvider() {
		this(null);
	}

	@Override
	public Gson get() {
		final GsonBuilder gsonBuilder = new GsonBuilder();

		if (jsonMessageSerializer != null) {
			gsonBuilder.registerTypeHierarchyAdapter(JsonMessage.class, jsonMessageSerializer);
		}
		gsonBuilder.setDateFormat(DateTimeUtil.JSON_DATETIME_FORMAT);
		gsonBuilder.registerTypeAdapter(ZonedDateTime.class, (JsonDeserializer<ZonedDateTime>) (json, type, jsonDeserializationContext) ->
			DateTimeUtil.parseISODateTimeString(json.getAsJsonPrimitive().getAsString())
		);
		gsonBuilder.registerTypeAdapter(ZonedDateTime.class, (JsonSerializer<ZonedDateTime>) (zonedDateTime, type, jsonDeserializationContext) ->
			new JsonPrimitive(zonedDateTime.format(DateTimeFormatter.ofPattern(DateTimeUtil.JSON_DATETIME_FORMAT)))
		);

		return gsonBuilder.create();
	}
}
