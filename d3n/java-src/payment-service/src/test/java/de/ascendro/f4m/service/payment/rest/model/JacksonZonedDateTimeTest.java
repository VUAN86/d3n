package de.ascendro.f4m.service.payment.rest.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.JsonTestUtil;

public class JacksonZonedDateTimeTest {

	private ObjectMapper objectMapper;

	@Before
	public void setUp() {
		objectMapper = new ObjectMapper();
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	@Test
	public void UserRestInsertDateSerializationTest() throws Exception {
		String jsonString = "{\"DateOfBirth\":\"2017-01-01\"}";
		ZonedDateTime expectedDate = ZonedDateTime.of(2017, 01, 01, 00, 00, 00, 00, ZoneOffset.UTC);
		processJacksonSerializationDeserialization(jsonString, "DateOfBirth", expectedDate, jsonString,
				UserRestInsert.class);
	}

	@Test
	public void TransactionRestDateSerializationTest() throws Exception {
		String jsonString = "{\"Created\":\"2017-01-01T10:10:10Z\"}";
		ZonedDateTime expectedDate = ZonedDateTime.of(2017, 01, 01, 10, 10, 10, 0, DateTimeUtil.TIMEZONE);
		processJacksonSerializationDeserialization(jsonString, "Created", expectedDate, jsonString,
				TransactionRest.class);
	}

	@Test
	public void TransactionRestDateWithZoneSerializationTest() throws Exception {
		String jsonString = "{\"Created\":\"2017-01-01T10:10:10+01:00[Europe/Paris]\"}";
		String expectedJson = "{\"Created\":\"2017-01-01T09:10:10Z\"}";
		ZonedDateTime expectedDate = ZonedDateTime.of(2017, 01, 01, 9, 10, 10, 0, DateTimeUtil.TIMEZONE);
		processJacksonSerializationDeserialization(jsonString, "Created", expectedDate, expectedJson,
				TransactionRest.class);
	}

	@Test
	public void TransactionRestDateWithFullZoneSerializationTest() throws Exception {
		String jsonString = "{\"Created\":\"2017-01-01T10:10:10+05\"}";
		String expectedJson = "{\"Created\":\"2017-01-01T05:10:10Z\"}";
		ZonedDateTime expectedDate = ZonedDateTime.of(2017, 01, 01, 5, 10, 10, 0, DateTimeUtil.TIMEZONE);
		processJacksonSerializationDeserialization(jsonString, "Created", expectedDate, expectedJson,
				TransactionRest.class);
	}

	@Test
	public void PaymentTransactionRestDateSerializationTest() throws Exception {
		String jsonString = "{\"Created\":\"2017-01-01T10:10:10\"}";
		ZonedDateTime expectedDate = ZonedDateTime.of(2017, 01, 01, 10, 10, 10, 0, DateTimeUtil.TIMEZONE);
		processJacksonSerializationDeserialization(jsonString, "Created", expectedDate, jsonString,
				PaymentTransactionRest.class);
	}

	protected void processJacksonSerializationDeserialization(String jsonString, String fieldName,
			ZonedDateTime expectedDate, String expectedJson, Class<?> objectClass) throws Exception {
		Object jsonObject = null;
		if (UserRestInsert.class == objectClass) {
			UserRestInsert dateObject = getObjectMapper().readValue(jsonString, UserRestInsert.class);
			assertTrue(expectedDate.isEqual(dateObject.getDateOfBirth()));
			jsonObject = dateObject;
		} else if (TransactionRest.class == objectClass) {
			TransactionRest dateObject = getObjectMapper().readValue(jsonString, TransactionRest.class);
			assertTrue(expectedDate.isEqual(dateObject.getCreated()));
			jsonObject = dateObject;
		} else if (PaymentTransactionRest.class == objectClass) {
			PaymentTransactionRest dateObject = getObjectMapper().readValue(jsonString, PaymentTransactionRest.class);
			assertTrue(expectedDate.isEqual(dateObject.getCreated()));
			jsonObject = dateObject;
		}
		String jsonOut = getObjectMapper().writeValueAsString(jsonObject);
		assertEquals(JsonTestUtil.extractFieldValue(expectedJson, fieldName),
				JsonTestUtil.extractFieldValue(jsonOut, fieldName));
	}

}