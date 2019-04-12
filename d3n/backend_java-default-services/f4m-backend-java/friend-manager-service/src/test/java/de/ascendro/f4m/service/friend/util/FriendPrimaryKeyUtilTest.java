package de.ascendro.f4m.service.friend.util;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import de.ascendro.f4m.service.config.Config;

public class FriendPrimaryKeyUtilTest {

	private Config config;
	private FriendPrimaryKeyUtil friendPrimaryKeyUtil;

	@Before
	public void setUp() {
		friendPrimaryKeyUtil = new FriendPrimaryKeyUtil(config);
	}
	
	@Test
	public void primaryKeyTest(){
		assertEquals("friend:1", friendPrimaryKeyUtil.createPrimaryKey("1"));
	}
}
