package de.ascendro.f4m.service.json;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.DateTypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import de.ascendro.f4m.service.util.DateTimeUtil;

/**
 * Basically a copy from com.google.gson.internal.bind.DateTypeAdapter which cannot be extended (because it is final).
 */
public class GsonSimpleDateAdapter extends TypeAdapter<ZonedDateTime> {
	public static final String SIMPLE_FORMAT = "yyyy-MM-dd";
	
	public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
		@SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
		@Override
		public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
			return typeToken.getRawType() == ZonedDateTime.class ? (TypeAdapter<T>) new DateTypeAdapter() : null;
		}
	};

	private static final DateTimeFormatter FORMAT = new DateTimeFormatterBuilder()
	        .append(DateTimeFormatter.ISO_LOCAL_DATE)
	        .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
	        .parseDefaulting(ChronoField.MINUTE_OF_DAY, 0)
	        .parseDefaulting(ChronoField.SECOND_OF_DAY, 0)
	        .parseDefaulting(ChronoField.NANO_OF_DAY, 0)
	        .toFormatter().withZone(DateTimeUtil.TIMEZONE);
	
	
	@Override
	public ZonedDateTime read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		return deserializeToDate(in.nextString());
	}

	private synchronized ZonedDateTime deserializeToDate(String json) {
		return ZonedDateTime.parse(json, FORMAT);
	}

	@Override
	public synchronized void write(JsonWriter out, ZonedDateTime value) throws IOException {
		if (value == null) {
			out.nullValue();
			return;
		}
		String dateFormatAsString = FORMAT.format(value);
		out.value(dateFormatAsString);
	}

}
