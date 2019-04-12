package de.ascendro.f4m.server.multiplayer.dao;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.service.config.Config;

public class MultiplayerGameInstancePrimaryKeyUtilTest {
	
	@Mock
	private Config config;
	
	private MultiplayerGameInstancePrimaryKeyUtil pkUtil;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		when(config.getProperty(GameConfigImpl.AEROSPIKE_MULTIPLAYER_KEY_PREFIX)).thenReturn("mgi");
		pkUtil = new MultiplayerGameInstancePrimaryKeyUtil(config);
	}

	@Test
	public void testCreateInstanceRecordKey() {
		assertEquals("mgi:123:7", pkUtil.createInstanceRecordKey("123", 7L));
	}
	
	@Test
	public void testCreateIndexKeyToInstanceRecord() {
		assertEquals("mgi:123:user:456", pkUtil.createIndexKeyToInstanceRecord("123", "456"));
	}
	
	@Test
	public void testCreateMetaKey() {
		assertEquals("mgi:123:meta", pkUtil.createMetaKey("123"));
	}
	
	@Test
	public void testCreateInvitationListKey() {
		assertEquals("mgi:tenant:123:app:789:user:456", pkUtil.createInvitationListKey("123", "789", "456"));
	}
	
	@Test
	public void testCreateGameInstanceIdKey() throws Exception {
		assertEquals("mgi:123:user:456:gameInstanceId", pkUtil.createGameInstanceIdKey("123", "456"));
	}
	
	@Test
	public void testCreateGameInstanceCounterKey() {
		assertEquals("mgi:123:gameInstances", pkUtil.createGameInstanceCounterKey("123"));
	}
	
	@Test
	public void testCreateTournamentMappingKey() {
		assertEquals("mgi:tournament:123", pkUtil.createTournamentMappingKey("123"));
	}

}
