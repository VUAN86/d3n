package de.ascendro.f4m.server.dashboard.dao;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.Operation;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.selection.model.dashboard.MostPlayedGame;
import de.ascendro.f4m.service.game.selection.model.dashboard.PlayedGameInfo;
import de.ascendro.f4m.service.game.selection.model.game.GameType;

public class DashboardDaoImpl extends AerospikeOperateDaoImpl<DashboardPrimaryKeyUtil>implements DashboardDao {

	private static final Logger LOGGER = LoggerFactory.getLogger(DashboardDaoImpl.class);

	private static final String BIN_INFO = "info";
	private static final String BIN_PLAYS = "plays";

	@Inject
	public DashboardDaoImpl(Config config, DashboardPrimaryKeyUtil primaryKeyUtil, JsonUtil jsonUtil,
			AerospikeClientProvider aerospikeClientProvider) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public void updateLastPlayedGame(String tenantId, String userId, PlayedGameInfo gameInfo) {
		String key = primaryKeyUtil.createUserInfoKey(tenantId, userId);
		createOrUpdateJson(getSet(), key, gameInfo.getType().getCode(), (r, p) -> jsonUtil.toJson(gameInfo));

		updatePlayedGameInfo(gameInfo);
	}

	private void updatePlayedGameInfo(PlayedGameInfo gameInfo) {
		String key = primaryKeyUtil.createGameInfoKey(gameInfo.getGameId());
		createOrUpdateJson(getSet(), key, BIN_INFO, (r, p) -> jsonUtil.toJson(gameInfo));
		
		Operation readPlays = Operation.get(BIN_PLAYS);
		operate(getSet(), key, new Operation[] { readPlays }, (readResult, wp) -> {
			List<Operation> result = new ArrayList<>(1);
			if (readResult != null) {
				result.add(Operation.add(getLongBin(BIN_PLAYS, 1L)));
			} else {
				LOGGER.error("Failed to read number of plays for game [{}]", gameInfo.getGameId());
			}
			return result;
		});
	}

	@Override
	public PlayedGameInfo getLastPlayedGame(String tenantId, String userId, GameType gameType) {
		String key = primaryKeyUtil.createUserInfoKey(tenantId, userId);
		return readPlayedGameInfo(key, gameType.getCode());
	}
	
	@Override
	public MostPlayedGame getMostPlayedGame(List<String> gameIds) {
		String[] keys = gameIds.stream()
				.map(primaryKeyUtil::createGameInfoKey)
				.toArray(String[]::new);
		Long[] numberOfPlays = readLongs(getSet(), keys, BIN_PLAYS);
		Integer index = getIndexOfMax(numberOfPlays);
		
		MostPlayedGame mostPlayedGame;
		if (index != null) {
			PlayedGameInfo gameInfo = readPlayedGameInfo(keys[index], BIN_INFO);
			mostPlayedGame = new MostPlayedGame(numberOfPlays[index], gameInfo);
		} else {
			mostPlayedGame = new MostPlayedGame();
		}
		return mostPlayedGame;
	}
	
	private PlayedGameInfo readPlayedGameInfo(String key, String binName) {
		String json = readJson(getSet(), key, binName);
		return json == null ? new PlayedGameInfo() : jsonUtil.fromJson(json, PlayedGameInfo.class);
	}
	
	protected static Integer getIndexOfMax(Long[] array) {
		Integer maxIndex = null;
		if (ArrayUtils.isNotEmpty(array)) {
			for (int i = 0; i < array.length; i++) {
				if (array[i] != null && (maxIndex == null || array[i] > array[maxIndex])) {
					maxIndex = i;
				}
			}
		}
		return maxIndex;
	}
	
	private String getSet() {
		return config.getProperty(GameConfigImpl.AEROSPIKE_DASHBOARD_SET);
	}

}
