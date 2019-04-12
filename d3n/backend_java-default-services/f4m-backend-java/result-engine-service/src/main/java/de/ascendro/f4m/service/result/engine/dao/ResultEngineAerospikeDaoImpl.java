package de.ascendro.f4m.service.result.engine.dao;

import com.google.gson.JsonObject;
import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.result.MultiplayerResults;
import de.ascendro.f4m.server.result.Results;
import de.ascendro.f4m.server.result.dao.CommonResultEngineDaoImpl;
import de.ascendro.f4m.server.result.dao.ResultEnginePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class ResultEngineAerospikeDaoImpl extends CommonResultEngineDaoImpl implements ResultEngineAerospikeDao {
	private static final Logger LOGGER = LoggerFactory.getLogger(ResultEngineAerospikeDaoImpl.class);

	@Inject
	public ResultEngineAerospikeDaoImpl(Config config, ResultEnginePrimaryKeyUtil primaryKeyUtil,
			AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public void saveResults(String gameInstanceId, Results results, boolean overwriteExisting) {
		String key = primaryKeyUtil.createResultsPrimaryKey(gameInstanceId);
		if (overwriteExisting) {
			createOrUpdateJson(getSet(), key, BIN_NAME_BLOB, (result, policy) -> results.getAsString());
		} else {
			createJson(getSet(), key, BIN_NAME_BLOB, results.getAsString());
		}
	}

	@Override
	public Results updateResults(String gameInstanceId, UpdateResultsAction resultChangeAction) {
		String key = primaryKeyUtil.createResultsPrimaryKey(gameInstanceId);
		LOGGER.debug("updateResults key {}  set {} ", key, getSet());
		String updatedResults = updateJson(getSet(), key, BIN_NAME_BLOB, resultChangeAction);
		return new Results(jsonUtil.fromJson(updatedResults, JsonObject.class));
	}

	@Override
	public void saveMultiplayerResults(MultiplayerResults results, boolean overwriteExisting) {
		String key = primaryKeyUtil.createMultiplayerResultsPrimaryKey(results.getMultiplayerGameInstanceId());
		if (overwriteExisting) {
			createOrUpdateJson(getSet(), key, BIN_NAME_BLOB, (result, policy) -> results.getAsString());
		} else {
			createJson(getSet(), key, BIN_NAME_BLOB, results.getAsString());
		}
	}

	public MultiplayerResults getResult(String multiplayerGameInstanceId) {
		try {
			String key = primaryKeyUtil.createMultiplayerResultsPrimaryKey(multiplayerGameInstanceId);
			String resultsJson = readJson(getSet(), key, BIN_NAME_BLOB);
			return new MultiplayerResults(jsonUtil.fromJson(resultsJson, JsonObject.class));
		} catch (Exception e) {
			LOGGER.debug("getResult try e{}", e.getMessage());
			return new MultiplayerResults();
		}

	}



}
