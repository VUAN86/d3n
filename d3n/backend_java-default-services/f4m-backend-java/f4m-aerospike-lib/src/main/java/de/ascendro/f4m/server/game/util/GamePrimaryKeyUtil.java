package de.ascendro.f4m.server.game.util;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.service.config.Config;

public class GamePrimaryKeyUtil extends PrimaryKeyUtil<String> {

	@Inject
	public GamePrimaryKeyUtil(Config config) {
		super(config);
	}

	@Override
	protected String getServiceName() {
		return config.getProperty(AerospikeConfigImpl.AEROSPIKE_GAME_KEY_PREFIX);
	}

}
