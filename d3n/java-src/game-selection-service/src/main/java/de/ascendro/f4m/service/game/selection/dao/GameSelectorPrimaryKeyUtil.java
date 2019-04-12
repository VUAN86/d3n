package de.ascendro.f4m.service.game.selection.dao;

import javax.inject.Inject;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.game.util.GamePrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;

/**
 * Game Selection Service Aerospike key construct helper
 *
 */
public class GameSelectorPrimaryKeyUtil extends GamePrimaryKeyUtil {
	
	private static final String SEARCH_FIELD_TENANT = "tenant";
	private static final String SEARCH_FIELD_APP = "app";

	@Inject
	public GameSelectorPrimaryKeyUtil(Config config) {
		super(config);
	}

	/**
	 * Creates search key of game with required attributes
	 * 
	 * @param tenantId
	 *            {@link String}
	 * @param appId
	 *            {@link String}
	 * @return search key in form [game_prefix]:tenant:[tenant_id]:app:[app_id]
	 */
	public String createSearchKey(String tenantId, String appId) {
		StringBuilder builder = new StringBuilder(config.getProperty(AerospikeConfigImpl.AEROSPIKE_GAME_KEY_PREFIX));
		builder.append(KEY_ITEM_SEPARATOR).append(SEARCH_FIELD_TENANT).append(KEY_ITEM_SEPARATOR).append(tenantId);
		builder.append(KEY_ITEM_SEPARATOR).append(SEARCH_FIELD_APP).append(KEY_ITEM_SEPARATOR).append(appId);

		return builder.toString();
	}
	
	/**
	 * Creates search key of game including additional attribute
	 * 
	 * @param tenantId
	 *            {@link String}
	 * @param appId
	 *            {@link String}
	 * @param searchField
	 *            {@link String}
	 * @param searchValue
	 *            {@link String}
	 * @return search key in form
	 *         [game_prefix]:tenant:[tenant_id]:app:[app_id]:[field]:[value]
	 */
	public String createSearchKey(String tenantId, String appId, String searchField, String searchValue) {
		StringBuilder builder = new StringBuilder(createSearchKey(tenantId, appId));
		builder.append(KEY_ITEM_SEPARATOR).append(searchField).append(KEY_ITEM_SEPARATOR).append(searchValue);

		return builder.toString();
	}
}
