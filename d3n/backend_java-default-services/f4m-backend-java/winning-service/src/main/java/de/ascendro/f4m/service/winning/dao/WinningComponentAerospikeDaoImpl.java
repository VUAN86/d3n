package de.ascendro.f4m.service.winning.dao;

import javax.inject.Inject;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.winning.config.WinningConfig;
import de.ascendro.f4m.service.winning.model.WinningComponent;

public class WinningComponentAerospikeDaoImpl extends AerospikeDaoImpl<WinningComponentPrimaryKeyUtil>
		implements WinningComponentAerospikeDao {

	@Inject
	public WinningComponentAerospikeDaoImpl(Config config, WinningComponentPrimaryKeyUtil winningComponentPrimaryKeyUtil,
			JsonUtil jsonUtil, AerospikeClientProvider aerospikeClientProvider) {
		super(config, winningComponentPrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public WinningComponent getWinningComponent(String id) {
		final String key = primaryKeyUtil.createWinningComponentKey(id);
		final String componentAsString = readJson(getSet(), key, BLOB_BIN_NAME);
		return componentAsString == null ? null : new WinningComponent(jsonUtil.fromJson(componentAsString, JsonObject.class));
	}

	protected String getSet() {
		return config.getProperty(WinningConfig.AEROSPIKE_WINNING_COMPONENT_SET);
	}

}
