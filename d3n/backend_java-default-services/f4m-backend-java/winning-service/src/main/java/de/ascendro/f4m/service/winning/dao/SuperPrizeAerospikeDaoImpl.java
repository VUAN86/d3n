package de.ascendro.f4m.service.winning.dao;

import javax.inject.Inject;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.winning.config.WinningConfig;
import de.ascendro.f4m.service.winning.model.SuperPrize;

public class SuperPrizeAerospikeDaoImpl extends AerospikeDaoImpl<PrimaryKeyUtil<String>> implements SuperPrizeAerospikeDao {

	@Inject
	public SuperPrizeAerospikeDaoImpl(Config config, JsonUtil jsonUtil, AerospikeClientProvider aerospikeClientProvider) {
		super(config, new PrimaryKeyUtil<String>(config), jsonUtil, aerospikeClientProvider);
	}

	@Override
	public SuperPrize getSuperPrize(String superPrizeId) {
		final String key = primaryKeyUtil.createPrimaryKey(superPrizeId);
		final String superPrizeAsString = readJson(getSet(), key, BLOB_BIN_NAME);
		return superPrizeAsString == null ? null : new SuperPrize(jsonUtil.fromJson(superPrizeAsString, JsonObject.class));
	}

	protected String getSet() {
		return config.getProperty(WinningConfig.AEROSPIKE_SUPER_PRIZE_SET);
	}

}
