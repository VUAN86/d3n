package de.ascendro.f4m.service.util.random;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class RandomUtilImplTest {
	private RandomUtil randomUtil;

	@Before
	public void setUp() throws Exception {
		randomUtil = new RandomUtilImpl();
	}

	@Test
	public void testNextInt() {
		int nextInt = randomUtil.nextInt(100);
		assertTrue(nextInt >= 0 && nextInt < 100);
	}
	
	@Test
	public void testNextLong() {
		long nextLong = randomUtil.nextLong(100L);
		assertTrue(nextLong >= 0 && nextLong < 100);
	}

	@Test
	public void testNextBlumBlumShub() {
		int nextBlumBlumShubInt = randomUtil.nextBlumBlumShubInt(0, 100);
		assertTrue(nextBlumBlumShubInt >= 0 && nextBlumBlumShubInt < 100);
	}

}
