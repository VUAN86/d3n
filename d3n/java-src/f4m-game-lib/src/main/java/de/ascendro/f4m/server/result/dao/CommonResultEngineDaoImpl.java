package de.ascendro.f4m.server.result.dao;

import javax.inject.Inject;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.result.MultiplayerResults;
import de.ascendro.f4m.server.result.Results;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;

public class CommonResultEngineDaoImpl extends AerospikeOperateDaoImpl<ResultEnginePrimaryKeyUtil>
		implements CommonResultEngineDao {

	protected static final String BIN_NAME_BLOB = "value";

	@Inject
	public CommonResultEngineDaoImpl(Config config, ResultEnginePrimaryKeyUtil primaryKeyUtil, JsonUtil jsonUtil,
			AerospikeClientProvider aerospikeClientProvider) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public Results getResults(String gameInstanceId) {
		final String resultPrimaryKey = primaryKeyUtil.createResultsPrimaryKey(gameInstanceId);
		final String resultsJson = readJson(getSet(), resultPrimaryKey, BIN_NAME_BLOB);
		return resultsJson == null ? null : new Results(jsonUtil.fromJson(resultsJson, JsonObject.class));
	}

	@Override
	public boolean exists(String gameInstanceId) {
		String key = primaryKeyUtil.createResultsPrimaryKey(gameInstanceId);
		return exists(getSet(), key);
	}

	@Override
	public MultiplayerResults getMultiplayerResults(String multiplayerGameInstanceId) {
		final String resultPrimaryKey = primaryKeyUtil.createMultiplayerResultsPrimaryKey(multiplayerGameInstanceId);
		final String resultsJson = readJson(getSet(), resultPrimaryKey, BIN_NAME_BLOB);
		return resultsJson == null ? null : new MultiplayerResults(jsonUtil.fromJson(resultsJson, JsonObject.class));
	}

	@Override
	public boolean hasMultiplayerResults(String mgiId) {
		String key = primaryKeyUtil.createMultiplayerResultsPrimaryKey(mgiId);
		return exists(getSet(), key);
	}

	protected String getSet() {
		return config.getProperty(GameConfigImpl.AEROSPIKE_RESULT_SET);
	}

}
