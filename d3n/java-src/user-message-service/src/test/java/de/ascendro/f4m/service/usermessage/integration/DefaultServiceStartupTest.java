package de.ascendro.f4m.service.usermessage.integration;

import com.google.inject.Module;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.integration.test.ServiceStartupTest;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringConnectionStatus;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringDbConnectionInfo;
import de.ascendro.f4m.service.usermessage.UserMessageServiceStartup;
import de.ascendro.f4m.service.usermessage.config.UserMessageConfig;
import de.ascendro.f4m.service.util.MonitoringDbConnectionInfoBuilder;

public class DefaultServiceStartupTest extends ServiceStartupTest {

	@Override
	protected ServiceStartup getServiceStartup() {
		return new UserMessageServiceStartup(DEFAULT_TEST_STAGE) {
			@Override
			protected Module getModule() {
				return Modules.override(super.getModule()).with(new UserMessageMockModule());
			}
		};
	}

	@Override
	public Class<? extends F4MConfigImpl> getServiceConfigClass() {
		return UserMessageConfig.class;
	}

	@Override
	protected MonitoringDbConnectionInfo getExpectedDbStatus() {
		return new MonitoringDbConnectionInfoBuilder(super.getExpectedDbStatus())
				.aerospike(MonitoringConnectionStatus.NC).build();
	}
}