package de.ascendro.f4m.service.game.engine.dao.instance;

import com.aerospike.client.policy.WritePolicy;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.UpdateCall;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.result.RefundReason;
import de.ascendro.f4m.service.game.engine.model.CloseUpReason;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.json.JsonMessageUtil;

public interface GameInstanceAerospikeDao extends CommonGameInstanceAerospikeDao {

	/**
	 * Store game instance
	 * 
	 * @param gameInstance
	 *            - instance of game
	 * @return game instance id
	 */
	String create(GameInstance gameInstance);

	String updateQuestions(String gameInstanceId, JsonObject newQuestionMap);
	
	/**
	 * Update game instance end status to CALCULATED
	 * @param gameInstanceId - if of game instance object
	 * @return new state of Game Instance
	 */
	GameInstance updateEndStatusToResultsCalculated(String gameInstanceId);

	GameInstance update(String gameInstanceId, UpdateGameInstanceAction updateGameInstanceAction);

	GameInstance cancelGameInstance(String gameInstanceId);
	
	GameInstance markShowAdvertisementWasSent(String gameInstanceId);
	
	void updateEndStatusToResultsCalculationFailed(String gameInstanceId);

	/**
	 * Close up game by setting it to terminated
	 * - specify close up reason and optional error message.
	 * - specify optional refund reason
	 * @param gameInstanceId - game instance id
	 * @param closeUpReason - game forced close up reason
	 * @param errorMessage - optional error message for close up description
	 * @param refundReason - refund reason or null if refund is not applicable
	 * @return
	 */
	GameInstance terminateIfNotYet(String gameInstanceId, CloseUpReason closeUpReason, String errorMessage, RefundReason refundReason);

	GameInstance calculateIfUnfinished(String gameInstanceId, CloseUpReason closeUpReason, String errorMessage, RefundReason refundReason, boolean isProcessExpiredDuel);

	abstract class UpdateGameInstanceAction implements UpdateCall<String> {
		private final JsonMessageUtil jsonUtil;

		public UpdateGameInstanceAction(JsonMessageUtil jsonUtil) {
			this.jsonUtil = jsonUtil;
		}

		public abstract String updateGameInstance(GameInstance gameInstance);

		@Override
		public String update(String readResult, WritePolicy writePolicy) {
			final GameInstance gameInstance = new GameInstance(jsonUtil.fromJson(readResult, JsonObject.class));
			return updateGameInstance(gameInstance);
		}
	}

}
