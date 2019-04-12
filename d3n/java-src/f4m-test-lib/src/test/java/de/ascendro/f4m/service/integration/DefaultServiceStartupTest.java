package de.ascendro.f4m.service.integration;

import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.integration.test.ServiceStartupTest;

public class DefaultServiceStartupTest extends ServiceStartupTest {
	@Override
	public Class<? extends F4MConfigImpl> getServiceConfigClass() {
		return F4MConfigImpl.class;
	}

}
