package de.ascendro.f4m.service.achievement.integration;

import static org.junit.Assert.assertNotNull;

import de.ascendro.f4m.server.achievement.config.AchievementConfig;
import de.ascendro.f4m.server.achievement.dao.AchievementAerospikeDao;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.integration.test.ServiceStartupTest;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringConnectionStatus;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringDbConnectionInfo;
import de.ascendro.f4m.service.util.MonitoringDbConnectionInfoBuilder;

public class DefaultServiceStartupTest extends ServiceStartupTest {
	@Override
	public void setUp() throws Exception {
		super.setUp();
		//trigger initialisation of Aerospike connection by requesting AerospikeDao creation with injection
		AchievementAerospikeDao achievementAerospikeDao = jettyServerRule.getServerStartup().getInjector()
				.getInstance(AchievementAerospikeDao.class);
		assertNotNull(achievementAerospikeDao);
	}
	
    @Override
    protected ServiceStartup getServiceStartup() {
        return new AchievementServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE);
    }

    @Override
    public Class<? extends F4MConfigImpl> getServiceConfigClass() {
        return AchievementConfig.class;
    }

    @Override
    protected MonitoringDbConnectionInfo getExpectedDbStatus() {
        return new MonitoringDbConnectionInfoBuilder(super.getExpectedDbStatus())
                .aerospike(MonitoringConnectionStatus.NC)
				.elastic(MonitoringConnectionStatus.NC).build();
    }
}
