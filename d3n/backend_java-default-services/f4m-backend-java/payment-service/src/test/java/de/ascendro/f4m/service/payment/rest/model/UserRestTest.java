package de.ascendro.f4m.service.payment.rest.model;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class UserRestTest {
	private ObjectMapper objectMapper;
	
	@Before
	public void setUp() {
		objectMapper = new ObjectMapper();
	}

	@Test
	public void testThatEmptyAttributesAreNotSerialized() throws Exception {
		String jsonValue = "{\"EMail\":\"xxx\"}";
		UserRest user = objectMapper.readValue(jsonValue, UserRest.class);
		assertEquals("xxx", user.getEmail());
		
		String jsonOut = objectMapper.writeValueAsString(user);
		assertEquals(jsonValue, jsonOut);
	}
}