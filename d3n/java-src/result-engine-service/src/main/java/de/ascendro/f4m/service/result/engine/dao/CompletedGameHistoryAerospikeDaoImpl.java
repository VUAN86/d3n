package de.ascendro.f4m.service.result.engine.dao;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.aerospike.client.Operation;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.MapOperation;
import com.aerospike.client.cdt.MapPolicy;
import com.aerospike.client.cdt.MapReturnType;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.history.dao.GameHistoryPrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.server.util.TimeBasedPaginationUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.result.engine.model.CompletedGameHistoryInfo;
import de.ascendro.f4m.service.result.engine.model.CompletedGameHistoryList;
import de.ascendro.f4m.service.result.engine.model.GameHistoryUpdateKind;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.F4MEnumUtils;

public class CompletedGameHistoryAerospikeDaoImpl extends AerospikeOperateDaoImpl<GameHistoryPrimaryKeyUtil>
		implements CompletedGameHistoryAerospikeDao {

	private static final String BIN_NAME_HISTORY = "history";

	private static final String SEPARATOR = ":";
	private static final String STRING_FORMAT_KEY_VALUE = "%s" + SEPARATOR + "%s";
	
    @Inject
    public CompletedGameHistoryAerospikeDaoImpl(Config config, GameHistoryPrimaryKeyUtil primaryKeyUtil,
            AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil) {
        super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
    }

	@Override
	public void saveResultsForHistory(String userId, String tenantId, GameType gameType, LocalDate endDate,
			CompletedGameHistoryInfo completedGameHistoryInfo, GameHistoryUpdateKind updateKind) {
		String strEndDate = DateTimeUtil.formatISODate(endDate);
		String key = primaryKeyUtil.createCompletedGameHistoryPrimaryKey(userId, tenantId, gameType, strEndDate);
		
		Value gameInstanceIdValue = Value.get(completedGameHistoryInfo.getGameInstanceId());
		Operation readHistory = MapOperation.getByKey(BIN_NAME_HISTORY, gameInstanceIdValue, MapReturnType.VALUE);
		operate(getSet(), key, new Operation[] { readHistory }, (readResult, writePolicy) -> {
			String existingHistoryStr = readResult == null ? null : readResult.getString(BIN_NAME_HISTORY);
			CompletedGameHistoryInfo existingHistory = existingHistoryStr == null ? null : jsonUtil.fromJson(existingHistoryStr, CompletedGameHistoryInfo.class);
			String result;
			if (existingHistory != null) {
				switch (updateKind) {
					case MULTIPLAYER_GAME_FINISH:
						// Update only multiplayer game fields
						existingHistory.setMultiplayerGameFinished(true);
						existingHistory.setPlacement(completedGameHistoryInfo.getPlacement());
						existingHistory.setGameOutcome(completedGameHistoryInfo.getGameOutcome());
						existingHistory.setPrizeWonAmount(completedGameHistoryInfo.getPrizeWonAmount());
						existingHistory.setPrizeWonCurrency(completedGameHistoryInfo.getPrizeWonCurrency());
						existingHistory.setOpponents(completedGameHistoryInfo.getOpponents());
						break;
					case BONUS_POINTS:
						// Update only bonus points
						existingHistory.setBonusPointsWonAmount(completedGameHistoryInfo.getBonusPointsWonAmount());
						break;
					case INITIAL:
					default:
						// Just use the new data. Should not happen, really.
						if (existingHistory.isMultiplayerGameFinished()) {
							// Don't let the game finished status go back to false
							completedGameHistoryInfo.setMultiplayerGameFinished(true);
						}
						existingHistory = completedGameHistoryInfo;
						break;
				}
				result = jsonUtil.toJson(existingHistory);
			} else {
				result = jsonUtil.toJson(completedGameHistoryInfo);
			}
			Operation writeOperation = MapOperation.put(MapPolicy.Default, BIN_NAME_HISTORY, gameInstanceIdValue,
					Value.get(result));
			return Arrays.asList(writeOperation);
		});
		String userHistoryEntryPrimaryKey = primaryKeyUtil.createCompletedGameHistoryIndexPrimaryKey(userId, tenantId);
		String indexedValue = String.format(STRING_FORMAT_KEY_VALUE, gameType, strEndDate);
		createOrUpdateMapValueByKey(getSet(), userHistoryEntryPrimaryKey, BIN_NAME_HISTORY, indexedValue, 
				(readResult, writePolicy) -> indexedValue);
	}
	
	@Override
	public void moveCompletedGameHistory(String sourceUserId, String targetUserId, String tenantId) {
		// Find source user history entries using index
		String sourceIndexKey = primaryKeyUtil.createCompletedGameHistoryIndexPrimaryKey(sourceUserId, tenantId);
		Map<Object, Object> historyIndex = getAllMap(getSet(), sourceIndexKey, BIN_NAME_HISTORY);
		// Move the history entries
		historyIndex.keySet().forEach(value -> {
			GameType gameType = F4MEnumUtils.getEnum(GameType.class, StringUtils.substringBefore((String) value, SEPARATOR));
			String date = StringUtils.substringAfter((String) value, SEPARATOR);
			moveCompletedGameHistoryForDateAndGameType(sourceUserId, targetUserId, tenantId, gameType, date);
		});
		
		// Move the index entry
		String targetIndexKey = primaryKeyUtil.createCompletedGameHistoryIndexPrimaryKey(targetUserId, tenantId);
		createOrUpdateMap(getSet(), targetIndexKey, BIN_NAME_HISTORY, (v, wp) -> {
			if (v != null) {
				historyIndex.putAll(v);
			}
			return historyIndex;
		});
		delete(getSet(), sourceIndexKey);
	}

	private void moveCompletedGameHistoryForDateAndGameType(String sourceUserId, String targetUserId, String tenantId,
			GameType gameType, String date) {
		String sourceKey = primaryKeyUtil.createCompletedGameHistoryPrimaryKey(sourceUserId, tenantId, gameType, date);
		Map<String, String> history = getAllMap(getSet(), sourceKey, BIN_NAME_HISTORY);
		if (MapUtils.isNotEmpty(history)) {
			final Map<Object, Object> resultMap = new HashMap<>();
			history.forEach((k, v) -> {
				CompletedGameHistoryInfo info = jsonUtil.fromJson(v, CompletedGameHistoryInfo.class);
				info.setUserId(targetUserId); // Update userId also in the information
				resultMap.put(k, jsonUtil.toJson(info));
			}); 
			String targetKey = primaryKeyUtil.createCompletedGameHistoryPrimaryKey(targetUserId, tenantId, gameType,
					date);
			createOrUpdateMap(getSet(), targetKey, BIN_NAME_HISTORY, (v, wp) -> {
				if (v != null) {
					resultMap.putAll(v);
				}
				return resultMap;
			});				
		}
		delete(getSet(), sourceKey);
	}

	private String getSet() {
		return config.getProperty(GameConfigImpl.AEROSPIKE_GAME_HISTORY_SET);
	}

	private List<CompletedGameHistoryInfo> getCompletedGamesForDate(String userId, String tenantId, GameType gameType,
			LocalDate endDate,
            String isMultiplayerGameFinished,
            String gameOutcome) {
		String set = getSet();
		String key = primaryKeyUtil.createCompletedGameHistoryPrimaryKey(userId, tenantId, gameType,
				DateTimeUtil.formatISODate(endDate));
		List<CompletedGameHistoryInfo> list =  getAllMap(set, key, BIN_NAME_HISTORY).entrySet().stream()
				.map(e -> jsonUtil.fromJson((String) e.getValue(), CompletedGameHistoryInfo.class))
				.filter(res ->
						isMultiplayerGameFinished == null || (res.isMultiplayerGameFinished() == Boolean.valueOf(isMultiplayerGameFinished) &&
								(gameOutcome == null || !gameOutcome.equals("notUndefined")) || res.getGameOutcome() != null)
				)
				.collect(Collectors.toList());
		return list;
	}

	@Override
	public CompletedGameHistoryList getCompletedGamesList(String userId, String tenantId, GameType gameType, long offset,
        int limit, ZonedDateTime dateTimeFrom, ZonedDateTime dateTimeTo, String isMultiplayerGameFinished, String gameOutcome) {
		ListResult<CompletedGameHistoryInfo> completedGameHistoryInfoListResult =
				TimeBasedPaginationUtil.getPaginatedItems(offset, limit,
						dateTimeFrom, dateTimeTo,
						zonedDateTime ->
								getCompletedGamesForDate(userId, tenantId, gameType, zonedDateTime.toLocalDate(),
										isMultiplayerGameFinished,
										gameOutcome),
						CompletedGameHistoryInfo::getEndDateTime,
						zonedDateTime ->
								getNumberOfCompletedGamesForDate(userId, tenantId, gameType, zonedDateTime.toLocalDate()),
						(zonedDateTime1, zonedDateTime2) -> zonedDateTime1.toLocalDate().equals(zonedDateTime2.toLocalDate()),
						zonedDateTime -> !zonedDateTime.toLocalDate().isBefore(dateTimeFrom.toLocalDate()),
						zonedDateTime -> zonedDateTime.minusDays(1));

		return new CompletedGameHistoryList(completedGameHistoryInfoListResult.getTotal(),
				completedGameHistoryInfoListResult.getItems());
	}

	@Override
	public int getNumberOfCompletedGamesForDate(String userId, String tenantId, GameType gameType, LocalDate endDate) {
		String key = primaryKeyUtil.createCompletedGameHistoryPrimaryKey(userId, tenantId, gameType,
				DateTimeUtil.formatISODate(endDate));
		Integer size = getMapSize(getSet(), key, BIN_NAME_HISTORY);
		return size != null ? size : 0;
	}

}
