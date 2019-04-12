package de.ascendro.f4m.service.payment.json;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import de.ascendro.f4m.service.util.DateTimeUtil;

public class JacksonDateSerializer extends JsonSerializer<ZonedDateTime> implements ContextualSerializer {

	private static final String JSON_DATETIME_FORMAT_NO_ZONE = "yyyy-MM-dd'T'HH:mm:ss";

	private String format;

	public JacksonDateSerializer() {
		//
	}

	public JacksonDateSerializer(String format) {
		this.format = format;
	}

	public String getFormat() {
		return format != null ? format : JSON_DATETIME_FORMAT_NO_ZONE;
	}

	@Override
	public void serialize(final ZonedDateTime value, final JsonGenerator jgen, final SerializerProvider provider)
			throws IOException {
		if (getFormat().equalsIgnoreCase(DateTimeUtil.JSON_DATETIME_FORMAT)) {
			jgen.writeString(value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)));
		} else {
			jgen.writeString(value.format(DateTimeFormatter.ofPattern(getFormat())));
		}
	}

	@Override
	public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
			throws JsonMappingException {
		String datePattern = null;
		JsonSerializerParameter param = null;
		if (property != null) {
			param = property.getAnnotation(JsonSerializerParameter.class);
		}
		if (param != null) {
			datePattern = param.format();
		}
		return new JacksonDateSerializer(datePattern);
	}
}
