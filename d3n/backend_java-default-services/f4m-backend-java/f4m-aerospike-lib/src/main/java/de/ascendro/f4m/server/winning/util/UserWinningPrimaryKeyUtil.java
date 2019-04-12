package de.ascendro.f4m.server.winning.util;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.winning.WinningMessageTypes;

public class UserWinningPrimaryKeyUtil extends PrimaryKeyUtil<String> {
	private static final String USER_KEY_PREFIX = "user";
	private static final String APP_KEY_PREFIX = "app";
	private static final String WINNING_KEY_PREFIX = "winning";
	private static final String USER_WINNING_COMPONENT_KEY_NAME = "userWinningComponent";
	private static final String AEROSPIKE_WINNING_COMPONENT_KEY_PREFIX = "winningComponent";

	@Inject
	public UserWinningPrimaryKeyUtil(Config config) {
		super(config);
	}

	public String createPrimaryKey(String appId, String userWinningId) {
		return getServiceName() + KEY_ITEM_SEPARATOR + APP_KEY_PREFIX + appId + KEY_ITEM_SEPARATOR + WINNING_KEY_PREFIX + userWinningId;
	}

	public String createUserWinningsKey(String appId, String userId) {
		return getServiceName() + KEY_ITEM_SEPARATOR + APP_KEY_PREFIX + appId + KEY_ITEM_SEPARATOR + USER_KEY_PREFIX + KEY_ITEM_SEPARATOR + userId;
	}

	public String createWinningComponentKey(String id) {
		String winningComponentPrefix = AEROSPIKE_WINNING_COMPONENT_KEY_PREFIX;
		return getServiceName() + KEY_ITEM_SEPARATOR + winningComponentPrefix + KEY_ITEM_SEPARATOR + id;
	}

	@Override
	protected String getServiceName() {
		return WinningMessageTypes.SERVICE_NAME;
	}

	public String createUserWinningComponentPrimaryKey(String userWinningComponentId) {
		return getServiceName() + KEY_ITEM_SEPARATOR + USER_WINNING_COMPONENT_KEY_NAME + KEY_ITEM_SEPARATOR + userWinningComponentId;
	}
}
