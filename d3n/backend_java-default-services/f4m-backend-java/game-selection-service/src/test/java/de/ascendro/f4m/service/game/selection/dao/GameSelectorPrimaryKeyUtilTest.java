package de.ascendro.f4m.service.game.selection.dao;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.selection.config.GameSelectionConfig;

public class GameSelectorPrimaryKeyUtilTest {

	private final static char KEY_ITEM_SEPARATOR = GameSelectorPrimaryKeyUtil.KEY_ITEM_SEPARATOR;

	private Config config;
	private GameSelectorPrimaryKeyUtil gameSelectorPrimaryKeyUtil;

	@Before
	public void setUp() {
		config = new GameSelectionConfig();
		gameSelectorPrimaryKeyUtil = new GameSelectorPrimaryKeyUtil(config);
	}

	@Test
	public void testCreateSearchKey() {
		String tenantId = "tenant_id_100";
		String appId = "app_id_200";

		String key = getGameAerospikeKeyPrefix() + KEY_ITEM_SEPARATOR + "tenant" + KEY_ITEM_SEPARATOR + tenantId
				+ KEY_ITEM_SEPARATOR + "app" + KEY_ITEM_SEPARATOR + appId;
		assertEquals(key, gameSelectorPrimaryKeyUtil.createSearchKey(tenantId, appId));
	}

	@Test
	public void testCreateSearchKeyWithAdditionalAttribute() {
		String tenantId = "tenant_id_100";
		String appId = "app_id_200";
		String primaryKey = getGameAerospikeKeyPrefix() + KEY_ITEM_SEPARATOR + "tenant" + KEY_ITEM_SEPARATOR + tenantId
				+ KEY_ITEM_SEPARATOR + "app" + KEY_ITEM_SEPARATOR + appId;

		String searchField = "pool";
		String searchValue = "puzzle";

		String key = primaryKey + KEY_ITEM_SEPARATOR + searchField + KEY_ITEM_SEPARATOR + searchValue;
		assertEquals(key, gameSelectorPrimaryKeyUtil.createSearchKey(tenantId, appId, searchField, searchValue));
	}

	private String getGameAerospikeKeyPrefix() {
		return config.getProperty(AerospikeConfigImpl.AEROSPIKE_GAME_KEY_PREFIX);
	}
}
