package de.ascendro.f4m.server.transaction.log;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class TransactionLogAerospikeDaoImplTest {
	@InjectMocks
	private TransactionLogAerospikeDaoImpl dao;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testBigDecimalToString() {
		assertEquals("12.34", dao.bigDecimalToString(new BigDecimal("12.34")));
		assertEquals("120000001.34", dao.bigDecimalToString(new BigDecimal("120000001.34")));
		assertEquals("12.34", dao.bigDecimalToString(new BigDecimal("1.234E+1")));
		assertEquals("123400", dao.bigDecimalToString(new BigDecimal("1.234E+5")));
	}

}
