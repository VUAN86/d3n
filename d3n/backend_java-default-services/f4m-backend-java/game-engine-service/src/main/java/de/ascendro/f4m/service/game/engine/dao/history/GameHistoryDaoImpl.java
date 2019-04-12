package de.ascendro.f4m.service.game.engine.dao.history;

import com.aerospike.client.Record;
import com.aerospike.client.query.PredExp;
import com.aerospike.client.query.RecordSet;
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
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.ascendro.f4m.server.history.dao.CommonGameHistoryDaoImpl.HISTORY_BIN_NAME;

public class GameHistoryDaoImpl extends AerospikeOperateDaoImpl<GameHistoryPrimaryKeyUtil> implements GameHistoryDao {

	private static final String PLAYED_QUESTION_BIN_NAME = "playedQts";
	private static final String PROFILE_BIN_NAME = "profile";
	private static final String GAMEINSTANCEID_BIN_NAME = "gameInstanceId";
	private static final String STATUS_BIN_NAME = "status";
	private static final String GAME_BIN_NAME = "game";
	private static final String MGI_BIN_NAME = "mgiId";
	private static final String TIMESTAMP_BIN_NAME = "timestamp";
	private static final Logger LOGGER = LoggerFactory.getLogger(GameHistoryDaoImpl.class);

	@Inject
	public GameHistoryDaoImpl(Config config, GameHistoryPrimaryKeyUtil primaryKeyUtil, JsonUtil jsonUtil,
			AerospikeClientProvider aerospikeClientProvider) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public void createUserGameHistoryEntry(String userId, String gameInstanceId, GameHistory newHistoryEntry) {
		final String userGameHistoryKey = primaryKeyUtil.createPrimaryKey(userId);
        String key;
        Map<String, String> oldMap = read(getSet(),userGameHistoryKey,HISTORY_BIN_NAME);     // huge map was used in old version
        if (oldMap != null) {
            int totalOld = 0;
            for (String gameHistoryEntry : oldMap.values()) {
                GameHistory gameHistory = new GameHistory(jsonUtil.fromJson(gameHistoryEntry, JsonObject.class));

                key = primaryKeyUtil.createPlayerGameInstancePrimaryKey(userId, gameHistory.getGameInstanceId());
                try {
                    createRecord(getSet(), key,
                            getStringBin(PROFILE_BIN_NAME, userId),
                            getStringBin(GAMEINSTANCEID_BIN_NAME, gameHistory.getGameInstanceId()),
                            getStringBin(STATUS_BIN_NAME, gameHistory.getStatus().name()),
                            getStringBin(GAME_BIN_NAME, gameHistory.getGameId()),
                            getStringBin(MGI_BIN_NAME, gameHistory.getMgiId()),
                            getLongBin(TIMESTAMP_BIN_NAME, gameHistory.getTimestamp()));
                } catch (Exception e) {
                    if (!e.getMessage().contains("Key already exists")) {
                        throw e;
                    }
                }
                totalOld++;
                if (totalOld == oldMap.values().size()) {
                    delete(getSet(), userGameHistoryKey);
                }
            }

        }


		key = primaryKeyUtil.createPlayerGameInstancePrimaryKey(userId, gameInstanceId);
		createRecord(getSet(), key,
				getStringBin(PROFILE_BIN_NAME, userId),
				getStringBin(GAMEINSTANCEID_BIN_NAME, gameInstanceId),
				getStringBin(STATUS_BIN_NAME, newHistoryEntry.getStatus().name()),
				getStringBin(GAME_BIN_NAME, newHistoryEntry.getGameId()),
				getStringBin(MGI_BIN_NAME, newHistoryEntry.getMgiId()),
				getLongBin(TIMESTAMP_BIN_NAME, newHistoryEntry.getTimestamp()));


	}
	
	@Override
	public void updateGameHistoryStatus(String userId, String gameInstanceId, GameStatus status,
			GameEndStatus endStatus) {

		Validate.isTrue(status != null || endStatus != null, "Cannot update history status if neither of game status or game end status provided");

		final String key = primaryKeyUtil.createPlayerGameInstancePrimaryKey(userId, gameInstanceId);
		updateString(getSet(), key, STATUS_BIN_NAME,
				(v, wp) -> status.name());

	}

	@Override
	public void addPlayedQuestions(String userId, String questionId) {
		final String playedQuestionKey = primaryKeyUtil.createPlayedQuestionPrimaryKey(userId, questionId);
		LOGGER.debug("addPlayedQuestions getSet{} key {}", getSet(), playedQuestionKey);

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

		final String key = primaryKeyUtil.createPlayerGameInstancePrimaryKey(userId, gameInstanceId);
		Record record = readRecord(getSet(),key);
		GameHistory gameHistory=new GameHistory();
		if (record!=null) {
			gameHistory.setStatus(GameStatus.valueOf(record.getString(STATUS_BIN_NAME)));
			gameHistory.setGameInstanceId(record.getString(GAMEINSTANCEID_BIN_NAME));
			gameHistory.setGameId(record.getString(GAME_BIN_NAME));
			gameHistory.setMgiId(record.getString(MGI_BIN_NAME));
			gameHistory.setTimestamp(record.getLong(TIMESTAMP_BIN_NAME));
		}
		return gameHistory;
	}

    @Override
    public List<GameHistory> getGameHistory(String userId) {
        final PredExp[] predExp = new PredExp[] {
                PredExp.stringBin(PROFILE_BIN_NAME),
                PredExp.stringValue(userId),
                PredExp.stringEqual()
        };
		List<GameHistory> history = new ArrayList<>();
		RecordSet recordSet=readByFilters(getSet(), new String[]
				{ PROFILE_BIN_NAME , STATUS_BIN_NAME, GAME_BIN_NAME, GAMEINSTANCEID_BIN_NAME, TIMESTAMP_BIN_NAME, MGI_BIN_NAME},
				predExp);
		while (recordSet.next()){
			Record record = recordSet.getRecord();
			GameHistory gameHistory = new GameHistory();
			gameHistory.setStatus(GameStatus.valueOf(record.getString(STATUS_BIN_NAME)));
			gameHistory.setGameInstanceId(record.getString(GAMEINSTANCEID_BIN_NAME));
			gameHistory.setGameId(record.getString(GAME_BIN_NAME));
			gameHistory.setMgiId(record.getString(MGI_BIN_NAME));
			gameHistory.setTimestamp(record.getLong(TIMESTAMP_BIN_NAME));
			history.add(gameHistory);
		}
        return history;

    }
	
	private String getSet() {
		return config.getProperty(GameConfigImpl.AEROSPIKE_GAME_HISTORY_SET);
	}

}
