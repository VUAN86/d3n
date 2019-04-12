package de.ascendro.f4m.service.result.engine.dao;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.policy.WritePolicy;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.UpdateCall;
import de.ascendro.f4m.server.result.MultiplayerResults;
import de.ascendro.f4m.server.result.Results;
import de.ascendro.f4m.server.result.dao.CommonResultEngineDao;
import de.ascendro.f4m.service.json.JsonMessageUtil;

public interface ResultEngineAerospikeDao extends CommonResultEngineDao {
    
    /**
     * Store/update the results.
     * @throws AerospikeException Thrown if results already exist
     */
	void saveResults(String gameInstanceId, Results results, boolean overwriteExisting);

    /**
     * Update the results.
     * @throws AerospikeException Thrown if results don't exist or modified by another process
     */
	Results updateResults(String gameInstanceId, UpdateResultsAction updateResultsAction);

    /**
     * Store/update the results.
     * @throws AerospikeException Thrown if results already exist
     */
	void saveMultiplayerResults(MultiplayerResults results, boolean overwriteExisting);

    public abstract class UpdateResultsAction implements UpdateCall<String> {
		private final JsonMessageUtil jsonUtil;

		public UpdateResultsAction(JsonMessageUtil jsonUtil) {
			this.jsonUtil = jsonUtil;
		}

		public abstract Results updateResults(Results results);

		@Override
		public String update(String readResult, WritePolicy writePolicy) {
			final Results results = new Results(jsonUtil.fromJson(readResult, JsonObject.class));
			return updateResults(results).getAsString();
		}
	}

	MultiplayerResults getResult(String multiplayerGameInstanceId);

}
