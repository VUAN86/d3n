package de.ascendro.f4m.service.result.engine.dao;

import javax.inject.Inject;

import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.result.engine.config.ResultEngineConfig;
import de.ascendro.f4m.service.result.engine.model.GameStatistics;
import de.ascendro.f4m.service.result.engine.util.GameStatisticsPrimaryKeyUtil;

public class GameStatisticsAerospikeDaoImpl extends AerospikeDaoImpl<GameStatisticsPrimaryKeyUtil> implements GameStatisticsDao {


	@Inject
	public GameStatisticsAerospikeDaoImpl(Config config, GameStatisticsPrimaryKeyUtil primaryKeyUtil,
			AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	private String getSet() {
		return config.getProperty(ResultEngineConfig.AEROSPIKE_GAME_STATISTICS_SET);
	}

	@Override
	public GameStatistics updateGameStatistics(String gameId, long incrementPlayedCountBy, 
			long incrementSpecialPrizeAvailableCountBy, long incrementSpecialPrizeWonCountBy) {
		final String key = primaryKeyUtil.createPrimaryKey(gameId);
		Record record = getAerospikeClient().operate(null, new Key(getNamespace(), getSet(), key), 
				Operation.add(getLongBin(BIN_NAME_PLAYED_COUNT, incrementPlayedCountBy)), 
				Operation.add(getLongBin(BIN_NAME_SPECIAL_PRIZE_AVAILABLE_COUNT, incrementSpecialPrizeAvailableCountBy)), 
				Operation.add(getLongBin(BIN_NAME_SPECIAL_PRIZE_WON_COUNT, incrementSpecialPrizeWonCountBy)), 
				Operation.get());
		return new GameStatistics(record.getLong(BIN_NAME_PLAYED_COUNT), record.getLong(BIN_NAME_SPECIAL_PRIZE_AVAILABLE_COUNT),
				record.getLong(BIN_NAME_SPECIAL_PRIZE_WON_COUNT));
	}

}
