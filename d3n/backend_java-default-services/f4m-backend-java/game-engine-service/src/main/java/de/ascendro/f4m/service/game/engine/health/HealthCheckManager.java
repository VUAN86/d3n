package de.ascendro.f4m.service.game.engine.health;

import java.util.List;

import javax.inject.Inject;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.dao.HealthCheckJsonArrayDao;

public class HealthCheckManager {
	private HealthCheckJsonArrayDao healthCheckJsonArrayDao;
	private Config config;

	@Inject
	public HealthCheckManager(HealthCheckJsonArrayDao healthCheckJsonArrayDao, Config config) {
		this.healthCheckJsonArrayDao = healthCheckJsonArrayDao;
		this.config = config;
	}

	public void calculateAndStoreHealthCheck(HealthCheckRequestInfoImpl healthCheckRequestInfo) {
		HealthCheckAerospikeEntity entity = new HealthCheckAerospikeEntity();
		entity.setRoundtripStartTime(healthCheckRequestInfo.getRoundtripStartTime());
		long roundtripTotalTime = System.currentTimeMillis() - healthCheckRequestInfo.getRoundtripStartTime();
		entity.setRoundtripTotalTime(roundtripTotalTime);
		healthCheckJsonArrayDao.addEntityToDbJsonArray(healthCheckRequestInfo.getClientId(), entity);
	}

	public Long getGameInstancePropagationDelay(String clientId) {
		List<HealthCheckAerospikeEntity> healthCheckList = healthCheckJsonArrayDao.getEntityList(clientId);
		if (!healthCheckList.isEmpty()) {
			int importantMeasurementCount = Math.min(healthCheckList.size(), getNumberOfMeasurementToTakeIntoAccount());
			long sum = 0;
			int importantMeasurements = 0;
			int remaining = healthCheckList.size() - 1;
			for (HealthCheckAerospikeEntity healthCheckAerospikeEntity : healthCheckList) {
				// ignore irrelevant threshold to avoid overflow
				if (remaining < importantMeasurementCount && healthCheckAerospikeEntity
						.getRoundtripTotalTime() <= getMaxAllowedRoundtripInMilliseconds()) {
					sum = sum + healthCheckAerospikeEntity.getRoundtripTotalTime();
					importantMeasurements++;
				}
				remaining--;
			}
			if (importantMeasurements > 0) {
				return sum / importantMeasurements;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public int getNumberOfMeasurementToTakeIntoAccount() {
		return config.getPropertyAsInteger(GameEngineConfig.HEALTH_CHECK_IMPORTANT_MEASUREMENT_COUNT);
	}

	public int getMaxAllowedRoundtripInMilliseconds() {
		return config.getPropertyAsInteger(GameEngineConfig.HEALTH_CHECK_ROUNDTRIP_THRESHOLD) * 1000;
	}
}
