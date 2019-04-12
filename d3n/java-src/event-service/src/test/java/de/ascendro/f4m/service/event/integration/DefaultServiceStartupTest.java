package de.ascendro.f4m.service.event.integration;

import org.junit.Test;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.event.EventServiceStartup;
import de.ascendro.f4m.service.event.config.EventConfig;
import de.ascendro.f4m.service.integration.test.ServiceStartupTest;

public class DefaultServiceStartupTest extends ServiceStartupTest {

	@Test(expected = AssertionError.class)
	@Override
	public void testOnMessageClientDisconnect() throws Exception {
		super.testOnMessageClientDisconnect();
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new EventServiceStartup(DEFAULT_TEST_STAGE);
	}

	@Override
	public Class<? extends F4MConfigImpl> getServiceConfigClass() {
		return EventConfig.class;
	}
}
