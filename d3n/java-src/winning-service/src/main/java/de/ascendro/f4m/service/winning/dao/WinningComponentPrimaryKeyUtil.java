package de.ascendro.f4m.service.winning.dao;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.winning.config.WinningConfig;

/**
 * Winning service Aerospike key construct helper
 *
 */
public class WinningComponentPrimaryKeyUtil extends PrimaryKeyUtil<String> {

	@Inject
	public WinningComponentPrimaryKeyUtil(Config config) {
		super(config);
	}

	public String createWinningComponentKey(String id) {
		String winningComponentPrefix = config.getProperty(WinningConfig.AEROSPIKE_WINNING_COMPONENT_KEY_PREFIX);
		return getServiceName() + KEY_ITEM_SEPARATOR + winningComponentPrefix + KEY_ITEM_SEPARATOR + id;
	}

}
