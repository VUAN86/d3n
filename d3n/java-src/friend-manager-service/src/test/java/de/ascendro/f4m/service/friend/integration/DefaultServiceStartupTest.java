package de.ascendro.f4m.service.friend.integration;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.friend.config.FriendManagerConfig;
import de.ascendro.f4m.service.integration.test.ServiceStartupTest;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringConnectionStatus;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringDbConnectionInfo;
import de.ascendro.f4m.service.util.MonitoringDbConnectionInfoBuilder;

public class DefaultServiceStartupTest extends ServiceStartupTest {

	@Override
	protected ServiceStartup getServiceStartup() {
		return new FriendManagerServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE);
	}

	@Override
	protected MonitoringDbConnectionInfo getExpectedDbStatus() {
		return new MonitoringDbConnectionInfoBuilder(super.getExpectedDbStatus())
				.aerospike(MonitoringConnectionStatus.NC)
				.elastic(MonitoringConnectionStatus.NC).build();
	}

	@Override
	public Class<? extends F4MConfigImpl> getServiceConfigClass() {
		return FriendManagerConfig.class;
	}
}