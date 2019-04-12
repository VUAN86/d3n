package de.ascendro.f4m.service.game.engine.health;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.dao.HealthCheckJsonArrayDao;

public class HealthCheckManagerTest {
	private static final String CLIENT_ID = "anyClientId";
	@Mock
	private HealthCheckJsonArrayDao healthCheckJsonArrayDao;
	@Mock
	private Config config;
	@InjectMocks
	private HealthCheckManager manager;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		when(config.getPropertyAsInteger(GameEngineConfig.HEALTH_CHECK_IMPORTANT_MEASUREMENT_COUNT)).thenReturn(3);
		//ignore numbers larger than 1000ms
		when(config.getPropertyAsInteger(GameEngineConfig.HEALTH_CHECK_ROUNDTRIP_THRESHOLD)).thenReturn(1);
	}
	
	@Test
	public void testPropagationDelayCalculationWithoutHealthCheck() throws Exception {
		assertNull(manager.getGameInstancePropagationDelay(CLIENT_ID));
	}

	@Test
	public void testPropagationDelayCalculation() throws Exception {
		testPropagationDelayCalculation(15, 10, 20);
	}

	@Test
	public void testPropagationDelayCalculationWithRounding() throws Exception {
		testPropagationDelayCalculation(15, 11, 20);
	}

	@Test
	public void testPropagationDelayCalculationWithTooManyMeasurementsInDb() throws Exception {
		//1 and 17 should be skipped, (10+20+3)/3=11
		testPropagationDelayCalculation(11, 1, 17, 10, 20, 3); 
	}

	@Test
	public void testPropagationDelayCalculationWithTooManyMeasurementsInDbIgnoringTooLarge() throws Exception {
		//1 and 17 should be skipped, 123456 should be ignored, (10+20)/2=15
		testPropagationDelayCalculation(15, 1, 17, 10, 20, 123456); 
	}

	@Test
	public void testPropagationDelayCalculationWithAllValuesTooLarge() throws Exception {
		List<HealthCheckAerospikeEntity> entities = new ArrayList<>();
		entities.add(createWithTotalTime(23456));
		entities.add(createWithTotalTime(123456));
		when(healthCheckJsonArrayDao.getEntityList(CLIENT_ID)).thenReturn(entities);
		assertNull(manager.getGameInstancePropagationDelay(CLIENT_ID));
		verify(healthCheckJsonArrayDao).getEntityList(CLIENT_ID);
	}

	private void testPropagationDelayCalculation(int expected, int... totalTimes) {
		List<HealthCheckAerospikeEntity> entities = new ArrayList<>();
		for (int i = 0; i < totalTimes.length; i++) {
			entities.add(createWithTotalTime(totalTimes[i]));
		}
		when(healthCheckJsonArrayDao.getEntityList(CLIENT_ID)).thenReturn(entities);
		assertEquals(expected, (long) manager.getGameInstancePropagationDelay(CLIENT_ID));
		verify(healthCheckJsonArrayDao).getEntityList(CLIENT_ID);
	}
	
	private HealthCheckAerospikeEntity createWithTotalTime(int roundtripTotalTime) {
		HealthCheckAerospikeEntity entity = new HealthCheckAerospikeEntity();
		entity.setRoundtripTotalTime((long) roundtripTotalTime);
		return entity;
	}
}
