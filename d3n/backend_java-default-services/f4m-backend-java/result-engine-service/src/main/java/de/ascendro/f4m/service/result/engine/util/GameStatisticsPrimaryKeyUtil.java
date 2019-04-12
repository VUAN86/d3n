package de.ascendro.f4m.service.result.engine.util;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;

public class GameStatisticsPrimaryKeyUtil extends PrimaryKeyUtil<String> {

	public static final String AEROSPIKE_KEY_PREFIX_GAME_STATISTICS = "gameStatistics";

	@Inject
    public GameStatisticsPrimaryKeyUtil(Config config) {
        super(config);
    }

	@Override
	protected String getServiceName() {
		return AEROSPIKE_KEY_PREFIX_GAME_STATISTICS;
	}

}
