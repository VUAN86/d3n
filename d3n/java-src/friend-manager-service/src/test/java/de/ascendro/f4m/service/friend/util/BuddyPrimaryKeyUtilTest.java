package de.ascendro.f4m.service.friend.util;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class BuddyPrimaryKeyUtilTest {
	
	private BuddyPrimaryKeyUtil buddyPrimaryKeyUtil;

	@Before
	public void setUp() throws Exception {
		buddyPrimaryKeyUtil = new BuddyPrimaryKeyUtil(null);
	}

	@Test
	public void testCreateBuddyPrimaryKey() {
		assertEquals("friend:user:456:buddy:789", buddyPrimaryKeyUtil.createBuddyPrimaryKey("456", "789"));
	}

}
