package de.ascendro.f4m.service.game.engine.dao.history;

import static de.ascendro.f4m.server.history.dao.CommonGameHistoryDaoImpl.HISTORY_BIN_NAME;

import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang3.Validate;

import com.aerospike.client.Operation;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.MapOperation;
import com.aerospike.client.cdt.MapPolicy;
import com.aerospike.client.cdt.MapReturnType;
import com.aerospike.client.policy.RecordExistsAction;
import com.google.gson.JsonObject;
import com.google.inject.Inject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.history.dao.GameHistoryPrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.model.GameHistory;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;

public class GameHistoryDaoImpl extends AerospikeOperateDaoImpl<GameHistoryPrimaryKeyUtil> implements GameHistoryDao {

	private static final String PLAYED_QUESTION_BIN_NAME = "playedQts";
	
	@Inject
	public GameHistoryDaoImpl(Config config, GameHistoryPrimaryKeyUtil primaryKeyUtil, JsonUtil jsonUtil,
			AerospikeClientProvider aerospikeClientProvider) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public GameHistory createUserGameHistoryEntry(String userId, String gameInstanceId, GameHistory newHistoryEntry) {
		final String userGameHistoryKey = primaryKeyUtil.createPrimaryKey(userId);

		final String resultGameHistory = super.<String, String> createOrUpdateMapValueByKey(getSet(), userGameHistoryKey, HISTORY_BIN_NAME, gameInstanceId, 
				(v, wp) -> newHistoryEntry.getAsString());
		return new GameHistory(jsonUtil.fromJson(resultGameHistory, JsonObject.class));
	}
	
	@Override
	public void updateGameHistoryStatus(String userId, String gameInstanceId, GameStatus status,
			GameEndStatus endStatus) {
		Validate.isTrue(status != null || endStatus != null, "Cannot update history status if neither of game status or game end status provided");
		final Value gameInstanIdAsValue = Value.get(gameInstanceId);

		final String userGameHistoryKey = primaryKeyUtil.createPrimaryKey(userId);		
		final Operation readHistoryEntryOperation = MapOperation.getByKey(HISTORY_BIN_NAME, gameInstanIdAsValue, MapReturnType.VALUE);
		super.operate(getSet(), userGameHistoryKey, new Operation[]{readHistoryEntryOperation}, (readResult, wp) -> {
			wp.recordExistsAction = RecordExistsAction.UPDATE_ONLY;
			Operation putHistoryOperation = null;
			
			if(readResult.bins.containsKey(HISTORY_BIN_NAME)){
				final String historyAsString = readResult.getString(HISTORY_BIN_NAME);
				
				if(historyAsString != null){
					final GameHistory gameHistory = new GameHistory(jsonUtil.fromJson(historyAsString, JsonObject.class));
					
					if(status != null){
						gameHistory.setStatus(status);
					}
					if(endStatus != null){
						gameHistory.setEndStatus(endStatus);
					}
					final Value newGameHistoryAsValue = Value.get(gameHistory.getAsString());
					
					putHistoryOperation = MapOperation.put(MapPolicy.Default, HISTORY_BIN_NAME, gameInstanIdAsValue,
							newGameHistoryAsValue);
				}				
			}
			
			return putHistoryOperation != null? Arrays.asList(putHistoryOperation) : Collections.emptyList();
		});
		
	}

	@Override
	public void addPlayedQuestions(String userId, String questionId) {
		final String playedQuestionKey = primaryKeyUtil.createPlayedQuestionPrimaryKey(userId, questionId);
		createOrUpdateString(getSet(), playedQuestionKey, PLAYED_QUESTION_BIN_NAME, (v, wp) -> {
			final long timeToLiveInDays = config
					.getPropertyAsLong(GameEngineConfig.PLAYED_QUESTION_ENTRY_TIME_TO_LIVE_IN_DAYS);
			wp.expiration = (int) (timeToLiveInDays * 24 * 60 * 60);
			return questionId;
		});
	}

	@Override
	public boolean isQuestionPlayed(String userId, String questionId) {
		final String playedQuestionKey = primaryKeyUtil.createPlayedQuestionPrimaryKey(userId, questionId);
		return exists(getSet(), playedQuestionKey);
	}
	
	@Override
	public GameHistory getGameHistory(String userId, String gameInstanceId) {
		final String userGameHistoryKey = primaryKeyUtil.createPrimaryKey(userId);
		final String gameHistoryEntry = super.<String, String>getByKeyFromMap(getSet(), userGameHistoryKey,
				HISTORY_BIN_NAME, gameInstanceId);
		return new GameHistory(jsonUtil.fromJson(gameHistoryEntry, JsonObject.class));
	}
	
	private String getSet() {
		return config.getProperty(GameConfigImpl.AEROSPIKE_GAME_HISTORY_SET);
	}

}
