package de.ascendro.f4m.service.advertisement.integration;

import de.ascendro.f4m.server.advertisement.config.AdvertisementConfig;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.integration.test.ServiceStartupTest;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringConnectionStatus;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringDbConnectionInfo;
import de.ascendro.f4m.service.util.MonitoringDbConnectionInfoBuilder;

public class DefaultServiceStartupTest extends ServiceStartupTest {
	
	@Override
	protected ServiceStartup getServiceStartup() {
		return new AdvertisementServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE);
	}

	@Override
	public Class<? extends F4MConfigImpl> getServiceConfigClass() {
		return AdvertisementConfig.class;
	}

	@Override
	protected MonitoringDbConnectionInfo getExpectedDbStatus() {
		return new MonitoringDbConnectionInfoBuilder(super.getExpectedDbStatus())
				.aerospike(MonitoringConnectionStatus.NC).build();
	}
}
