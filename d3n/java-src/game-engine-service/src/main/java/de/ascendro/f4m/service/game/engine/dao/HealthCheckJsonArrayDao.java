package de.ascendro.f4m.service.game.engine.dao;

import java.util.List;

import javax.inject.Inject;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.JsonArrayDao;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.health.HealthCheckAerospikeEntity;

public class HealthCheckJsonArrayDao extends JsonArrayDao<HealthCheckAerospikeEntity, String, PrimaryKeyUtil<String>> {
	public static final String HEALTH_CHECK_BIN_NAME = "healthCheck";

	@Inject
	public HealthCheckJsonArrayDao(AerospikeDao aerospikeDao, PrimaryKeyUtil<String> primaryKeyUtil,
			JsonUtil aerospikeJsonUtil, Config config) {
		super(aerospikeDao, primaryKeyUtil, aerospikeJsonUtil);
		String setName = config.getProperty(GameEngineConfig.AEROSPIKE_HEALTH_CHECK_SET);
		init(setName, HEALTH_CHECK_BIN_NAME, new TypeToken<List<HealthCheckAerospikeEntity>>() {
		}.getType());
	}
}
