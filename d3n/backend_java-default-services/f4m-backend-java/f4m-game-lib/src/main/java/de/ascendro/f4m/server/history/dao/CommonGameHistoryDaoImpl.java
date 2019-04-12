package de.ascendro.f4m.server.history.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.collections.MapUtils;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;

public class CommonGameHistoryDaoImpl extends AerospikeOperateDaoImpl<GameHistoryPrimaryKeyUtil>
		implements CommonGameHistoryDao {

	public static final String HISTORY_BIN_NAME = "history";

	@Inject
	public CommonGameHistoryDaoImpl(Config config, GameHistoryPrimaryKeyUtil primaryKeyUtil, JsonUtil jsonUtil,
			AerospikeClientProvider aerospikeClientProvider) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public List<String> getUserGameHistory(String userId) {
		String userGameHistoryKey = primaryKeyUtil.createPrimaryKey(userId);
		Map<String, String> history = getAllMap(getSet(), userGameHistoryKey, HISTORY_BIN_NAME);

		return MapUtils.isNotEmpty(history) ? new ArrayList<>(history.values()) : Collections.emptyList();
	}

	private String getSet() {
		return config.getProperty(GameConfigImpl.AEROSPIKE_GAME_HISTORY_SET);
	}

}
