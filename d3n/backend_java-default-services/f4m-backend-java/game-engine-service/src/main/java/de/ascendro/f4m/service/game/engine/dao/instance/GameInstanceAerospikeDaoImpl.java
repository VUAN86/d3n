package de.ascendro.f4m.service.game.engine.dao.instance;

import com.google.gson.JsonObject;
import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDaoImpl;
import de.ascendro.f4m.server.game.util.GameEnginePrimaryKeyUtil;
import de.ascendro.f4m.server.result.RefundReason;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.engine.model.CloseUpReason;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameState;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.util.DateTimeUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class GameInstanceAerospikeDaoImpl extends CommonGameInstanceAerospikeDaoImpl implements GameInstanceAerospikeDao {
	private static final Logger LOGGER = LoggerFactory.getLogger(GameInstanceAerospikeDaoImpl.class);

	@Inject
	public GameInstanceAerospikeDaoImpl(Config config, GameEnginePrimaryKeyUtil primaryKeyUtil,
			AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public String create(GameInstance gameInstance) {
		final String gameInstanceId = primaryKeyUtil.generateId();
		gameInstance.setId(gameInstanceId);

		final String gameInstanceKey = primaryKeyUtil.createPrimaryKey(gameInstanceId);
		createJson(getSet(), gameInstanceKey, BLOB_BIN_NAME, gameInstance.getAsString());

		List<String> gameInstanceList = getGameInstancesList(gameInstance.getGame().getGameId());
		final String gameIdKey = GAME_KEY_PREFIX +KEY_ITEM_SEPARATOR+ gameInstance.getGame().getGameId();
		gameInstanceList.add(gameInstanceId);
		createOrUpdateJson(getSet(), gameIdKey, BLOB_BIN_NAME,
				(existing, wp) -> jsonUtil.toJson(gameInstanceList));
		return gameInstanceId;
	}

	@SuppressWarnings("unchecked")
	public List<String> getGameInstancesList(String gameid){
		final String gameIdKey = GAME_KEY_PREFIX + KEY_ITEM_SEPARATOR + gameid;
		final String resultsJson = readJson(getSet(), gameIdKey, BLOB_BIN_NAME);
		List<String> gameInstanceList;
		if (StringUtils.isNotBlank(resultsJson)) {
			gameInstanceList = jsonUtil.fromJson(resultsJson, List.class);
		} else {
			gameInstanceList = new LinkedList();
		}
		return gameInstanceList;
	}


	@Override
	public String updateQuestions(String gameInstanceId, JsonObject newQuestionMap) {
		final String gameInstanceIdKey = primaryKeyUtil.createPrimaryKey(gameInstanceId);
		return updateJson(getSet(), gameInstanceIdKey, BLOB_BIN_NAME, (gameInstanceAsString, writePolicy) -> {
            final JsonObject gameInstanceElement = jsonUtil.fromJson(gameInstanceAsString, JsonObject.class);
            final GameInstance gameInstance = new GameInstance(gameInstanceElement);
            gameInstance.setQuestionMap(newQuestionMap);
            return gameInstance.getAsString();
        });
	}
	
	@Override
	public GameInstance update(String gameInstanceId, UpdateGameInstanceAction updateGameInstanceAction){
		final String gameInstanceKey = primaryKeyUtil.createPrimaryKey(gameInstanceId);
		final String gameInstanceJson = updateJson(getSet(), gameInstanceKey, BLOB_BIN_NAME, updateGameInstanceAction);
		return jsonUtil.toJsonObjectWrapper(gameInstanceJson, GameInstance::new);
	}

	@Override
	public GameInstance getGameInstance(String gameInstanceId) {
		final String gameInstanceKey = primaryKeyUtil.createPrimaryKey(gameInstanceId);
		final String gameInstanceJson = readJson(getSet(), gameInstanceKey, BLOB_BIN_NAME);
		return jsonUtil.toJsonObjectWrapper(gameInstanceJson, GameInstance::new);
	}

	@Override
	public GameInstance updateEndStatusToResultsCalculated(String gameInstanceId) {
		final String gameInstanceKey = primaryKeyUtil.createPrimaryKey(gameInstanceId);
		final String updateGameInstanceJson = super.updateJson(getSet(), gameInstanceKey, BLOB_BIN_NAME, (gameInstanceAsJsonString, wp) -> {
			final GameInstance gameInstance = new GameInstance(jsonUtil.fromJson(gameInstanceAsJsonString, JsonObject.class));

			final GameState gameState = gameInstance.getGameState();
			if (gameState.getGameEndStatus() != GameEndStatus.TERMINATED) {
				gameState.setGameEndStatus(GameEndStatus.CALCULATED_RESULT);
			}
			
			return gameInstance.getAsString();
		});
		return jsonUtil.toJsonObjectWrapper(updateGameInstanceJson, GameInstance::new);
	}
	
	@Override
	public void updateEndStatusToResultsCalculationFailed(String gameInstanceId) {
		final String gameInstanceKey = primaryKeyUtil.createPrimaryKey(gameInstanceId);
		super.updateJson(getSet(), gameInstanceKey, BLOB_BIN_NAME, (gameInstanceAsJsonString, wp) -> {
			final GameInstance gameInstance = new GameInstance(jsonUtil.fromJson(gameInstanceAsJsonString, JsonObject.class));

			final GameState gameState = gameInstance.getGameState();
			if (gameState.getGameEndStatus() == GameEndStatus.CALCULATING_RESULT) {
				gameState.setGameEndStatus(GameEndStatus.CALCULATING_RESULTS_FAILED);
			}
			
			if (!gameInstance.isFree() ) {		
				gameState.refund(RefundReason.RESULT_CALCULATION_FAILED.name());
				gameState.setCloseUpExplanation(CloseUpReason.BACKEND_FAILS, null);
			}
			
			return gameInstance.getAsString();
		});
	}
	
	@Override
	public GameInstance cancelGameInstance(String gameInstanceId) {
		final String gameInstanceKey = primaryKeyUtil.createPrimaryKey(gameInstanceId);
		final String gameInstanceAsString = super.updateJson(getSet(), gameInstanceKey, BLOB_BIN_NAME, (j, wp) -> {
			final GameInstance gameInstance = new GameInstance(jsonUtil.fromJson(j, JsonObject.class));
			
			final GameState gameState = gameInstance.getGameState();
			
			final GameStatus status = gameState.getGameStatus();
			gameState.noRefund();
			if (status == GameStatus.IN_PROGRESS && gameState.hasAnyQuestionAnswered()) {
				gameState.setGameEndStatus(GameEndStatus.CALCULATING_RESULT);
				gameState.fillInMissingAnswersAsEmpty(gameInstance,false);
			} else if (status == GameStatus.REGISTERED || status == GameStatus.PREPARED
					|| status == GameStatus.READY_TO_PLAY) {
				gameState.refund(RefundReason.GAME_NOT_PLAYED.name());
			}
			gameState.cancelGame();
			
			return gameInstance.getAsString();
		});
		return jsonUtil.toJsonObjectWrapper(gameInstanceAsString, GameInstance::new);
	}
	
	@Override
	public GameInstance markShowAdvertisementWasSent(String gameInstanceId) {
		final String gameInstanceKey = primaryKeyUtil.createPrimaryKey(gameInstanceId);
		final String updateGameInstanceJson = super.updateJson(getSet(), gameInstanceKey, BLOB_BIN_NAME, (j, wp) -> {
			final GameInstance gameInstance = new GameInstance(jsonUtil.fromJson(j, JsonObject.class));
			
			final GameState gameState = gameInstance.getGameState();
			// Set the flag to be used at the following nextStep call
			gameState.setAdvertisementSentBeforeNextStep(true);

			final Integer currentAdvertisementIndex = gameState.getCurrentAdvertisementIndex();
			if(currentAdvertisementIndex != null){
				final String[] advertisementBlobKeys = gameInstance.getAdvertisementBlobKeys();
				if(currentAdvertisementIndex + 1 < advertisementBlobKeys.length){
					gameState.setCurrentAdvertisementIndex(currentAdvertisementIndex + 1);
				}
			}
			return gameInstance.getAsString();
		});
		return jsonUtil.toJsonObjectWrapper(updateGameInstanceJson, GameInstance::new);
	}
	
	@Override
	public GameInstance terminateIfNotYet(String gameInstanceId, CloseUpReason closeUpReason, String errorMessage, RefundReason refundReason) {
		final String gameInstanceKey = primaryKeyUtil.createPrimaryKey(gameInstanceId);
		final String gameInstanceAsString = super.updateJson(getSet(), gameInstanceKey, BLOB_BIN_NAME, (gameInstanceAsJsonString, wp) -> {
			final GameInstance gameInstance = jsonUtil.toJsonObjectWrapper(gameInstanceAsJsonString, GameInstance::new);

			final GameState gameState = Optional.ofNullable(gameInstance.getGameState()).orElse(new GameState(GameStatus.CANCELLED));
			if (gameState.getGameEndStatus() == null) {
				gameState.setGameEndStatus(GameEndStatus.TERMINATED);
				gameState.setCloseUpExplanation(closeUpReason, errorMessage);
				if (gameState.getGameStatus() != GameStatus.COMPLETED) {
					gameState.cancelGame();
				}
			}

			if (refundReason != null && gameState.hasEntryFeeTransactionId()) {
				gameState.refund(refundReason.name());
			} else {
				gameState.noRefund();
			}

			gameInstance.setGameState(gameState);
			return gameInstance.getAsString();
		});
		return jsonUtil.toJsonObjectWrapper(gameInstanceAsString, GameInstance::new);
	}

	@Override
	public GameInstance calculateIfUnfinished(String gameInstanceId, CloseUpReason closeUpReason, String errorMessage, RefundReason refundReason, boolean isProcessExpiredDuel) {
		final String gameInstanceKey = primaryKeyUtil.createPrimaryKey(gameInstanceId);
		final String gameInstanceAsString = super.updateJson(getSet(), gameInstanceKey, BLOB_BIN_NAME, (gameInstanceAsJsonString, wp) -> {
			final GameInstance gameInstance = jsonUtil.toJsonObjectWrapper(gameInstanceAsJsonString, GameInstance::new);
			final GameState gameState = Optional.ofNullable(gameInstance.getGameState())
					.orElse(new GameState(GameStatus.CANCELLED));
			if (gameState.getGameStatus() == GameStatus.IN_PROGRESS || gameState.getGameStatus() == GameStatus.PREPARED && gameState.hasAnyQuestionAnswered()) {
				gameState.setGameEndStatus(GameEndStatus.CALCULATING_RESULT);
				gameState.fillInMissingAnswersAsEmpty(gameInstance, isProcessExpiredDuel);
			} else if (!isProcessExpiredDuel) {
				gameState.setGameEndStatus(GameEndStatus.TERMINATED);
			}
			if (gameState.getGameStatus() != GameStatus.COMPLETED && !isProcessExpiredDuel) {
				gameState.cancelGame();
			}
			if (refundReason != null && gameState.hasEntryFeeTransactionId() && !isProcessExpiredDuel) {
				gameState.refund(refundReason.name());
			} else if (!isProcessExpiredDuel) {
				gameState.noRefund();
			}
			if (!isProcessExpiredDuel) {
				gameState.setCloseUpExplanation(closeUpReason, errorMessage);
			} else {
				gameInstance.setEndDateTime(DateTimeUtil.getCurrentDateTime());
			}
			gameInstance.setGameState(gameState);
			return gameInstance.getAsString();
		});
		return jsonUtil.toJsonObjectWrapper(gameInstanceAsString, GameInstance::new);
	}
}
