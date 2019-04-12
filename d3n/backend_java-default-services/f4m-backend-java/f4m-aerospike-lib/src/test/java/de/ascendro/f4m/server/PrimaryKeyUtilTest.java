package de.ascendro.f4m.server;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfig;

public class PrimaryKeyUtilTest {
	private static final String SERVICE_NAME = "TEST_SERVICE";

	@Mock
	private Config config;

	private PrimaryKeyUtil<String> primaryKeyUtil;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		when(config.getProperty(F4MConfig.SERVICE_NAME)).thenReturn(SERVICE_NAME);

		this.primaryKeyUtil = new PrimaryKeyUtil<>(config);
	}

	@Test
	public void testParseId() {
		final String key = UUID.randomUUID().toString();
		assertEquals(key, primaryKeyUtil.parseId(SERVICE_NAME + PrimaryKeyUtil.KEY_ITEM_SEPARATOR + key));
	}

	@Test
	public void testCreatePrimaryKey() {
		String id = "id_100";
		assertEquals(SERVICE_NAME + PrimaryKeyUtil.KEY_ITEM_SEPARATOR + id, primaryKeyUtil.createPrimaryKey(id));
	}

}
