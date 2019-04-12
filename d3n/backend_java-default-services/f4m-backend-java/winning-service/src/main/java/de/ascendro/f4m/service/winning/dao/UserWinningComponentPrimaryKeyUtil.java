package de.ascendro.f4m.service.winning.dao;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;

/**
 * Winning service Aerospike key construct helper
 *
 */
public class UserWinningComponentPrimaryKeyUtil extends PrimaryKeyUtil<String> {

	private static final String APP_KEY_NAME = "app";
	private static final String USER_KEY_NAME = "user";
	private static final String USER_WINNING_COMPONENT_KEY_NAME = "userWinningComponent";

	@Inject
	public UserWinningComponentPrimaryKeyUtil(Config config) {
		super(config);
	}

	public String createUserWinningComponentMapPrimaryKey(String appId, String userId) {
		return getServiceName() + KEY_ITEM_SEPARATOR + APP_KEY_NAME + KEY_ITEM_SEPARATOR + appId + KEY_ITEM_SEPARATOR + USER_KEY_NAME + KEY_ITEM_SEPARATOR + userId;
	}

	public String createUserWinningComponentPrimaryKey(String userWinningComponentId) {
		return getServiceName() + KEY_ITEM_SEPARATOR + USER_WINNING_COMPONENT_KEY_NAME + KEY_ITEM_SEPARATOR + userWinningComponentId;
	}

}
