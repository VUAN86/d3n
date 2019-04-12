package de.ascendro.f4m.service.payment.json;

import java.io.IOException;
import java.time.ZonedDateTime;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import de.ascendro.f4m.service.util.DateTimeUtil;

public class JacksonDateDeserializer extends JsonDeserializer<ZonedDateTime> {

	@Override
	public ZonedDateTime deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
		ZonedDateTime value = null;
		if (parser != null && StringUtils.isNotEmpty(parser.getText())) {
			value = DateTimeUtil.parseISODateTimeString(parser.getText());
		}
		return value;
	}
}
