package de.ascendro.f4m.service.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.util.JsonTestUtil;

public class GsonZonedDateTimeTest {

	private Gson gson;

	@Before
	public void setUp() {
		GsonProvider gsonProvider = new GsonProvider();
		gson = gsonProvider.get();
	}

	private Gson getGson() {
		return gson;
	}

	private class ZonedDateObject implements JsonMessageContent {
		private ZonedDateTime zonedDate;

		public ZonedDateTime getZonedDate() {
			return zonedDate;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("DateObject [zonedDate=");
			builder.append(zonedDate);
			builder.append("]");
			return builder.toString();
		}

	}

	@Test
	public void readDateTest() throws IOException {
		String jsonString = "{\"zonedDate\":\"2017-01-11T14:37:59Z\"}";
		ZonedDateTime expectedDateTime = ZonedDateTime.of(2017, 1, 11, 14, 37, 59, 0, ZoneOffset.UTC);
		processGsonDeSerializationDeserialization(jsonString, "creationDate", expectedDateTime);
	}

	@Test
	public void readDateWithZoneTest() throws IOException {
		String jsonString = "{\"zonedDate\":\"2017-01-11T14:37:59+01\"}";
		ZonedDateTime expectedDateTime = ZonedDateTime.of(2017, 1, 11, 13, 37, 59, 0, ZoneOffset.UTC);
		processGsonDeSerializationDeserialization(jsonString, "creationDate", expectedDateTime);
	}

	@Test
	public void readDateWithFullZoneTest() throws IOException {
		String jsonString = "{\"zonedDate\":\"2017-01-11T14:37:59-01:22:01\"}";
		ZonedDateTime expectedDateTime = ZonedDateTime.of(2017, 1, 11, 16, 0, 0, 0, ZoneOffset.UTC);
		processGsonDeSerializationDeserialization(jsonString, "creationDate", expectedDateTime);
	}

	private void processGsonDeSerializationDeserialization(String jsonString, String fieldName,
			ZonedDateTime expectedDateTime) throws IOException {
		ZonedDateObject object = getGson().fromJson(jsonString, ZonedDateObject.class);
		assertTrue(object.getZonedDate().withZoneSameInstant(ZoneOffset.UTC).isEqual(expectedDateTime));
		String resultJson = getGson().toJson(object);
		assertEquals(expectedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
				JsonTestUtil.extractFieldValue(resultJson, fieldName));
	}

}