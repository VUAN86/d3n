package de.ascendro.f4m.service.payment.rest.model;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TransactionRestInsertTest {
	private ObjectMapper objectMapper;
	
	@Before
	public void setUp() {
		objectMapper = new ObjectMapper();
	}

	@Test
	public void testThatEmptyAttributesAreNotSerialized() throws Exception {
		String jsonValue = "{\"DebitorAccountId\":\"asdf\"}";
		TransactionRestInsert transactionRestInsert = objectMapper.readValue(jsonValue, TransactionRestInsert.class);
		assertEquals("asdf", transactionRestInsert.getDebitorAccountId());
		
		String jsonOut = objectMapper.writeValueAsString(transactionRestInsert);
		assertEquals(jsonValue, jsonOut);
	}
}
