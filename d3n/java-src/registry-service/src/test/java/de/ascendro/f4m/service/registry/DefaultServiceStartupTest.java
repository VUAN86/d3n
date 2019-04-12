package de.ascendro.f4m.service.registry;

import org.junit.Test;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.integration.test.ServiceStartupTest;
import de.ascendro.f4m.service.registry.config.ServiceRegistryConfig;

public class DefaultServiceStartupTest extends ServiceStartupTest {

	@Test(expected = AssertionError.class)
	@Override
	public void testOnMessageClientDisconnect() throws Exception {
		super.testOnMessageClientDisconnect();
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new RegistryServiceStartup(DEFAULT_TEST_STAGE);
	}

	@Override
	protected void verifyMonitoring() {
		// no monitoring expected for service registry
	}

	@Override
	public Class<? extends F4MConfigImpl> getServiceConfigClass() {
		return ServiceRegistryConfig.class;
	}
}