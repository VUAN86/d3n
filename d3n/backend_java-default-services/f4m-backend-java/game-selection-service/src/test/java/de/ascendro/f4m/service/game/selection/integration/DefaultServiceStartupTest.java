package de.ascendro.f4m.service.game.selection.integration;

import static de.ascendro.f4m.service.integration.F4MAssert.assertReceivedMessagesAnyOrderWithWait;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.game.selection.config.GameSelectionConfig;
import de.ascendro.f4m.service.integration.test.ServiceStartupTest;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringConnectionStatus;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringDbConnectionInfo;
import de.ascendro.f4m.service.util.MonitoringDbConnectionInfoBuilder;

public class DefaultServiceStartupTest extends ServiceStartupTest {

	@Override
	public Class<? extends F4MConfigImpl> getServiceConfigClass() {
		return GameSelectionConfig.class;
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new GameSelectionStartupUsingAerospikeMock(DEFAULT_TEST_STAGE);
	}

	@Override
	protected void verifyAndCleanStartupMessagesReceivedOnMock() {
		assertReceivedMessagesAnyOrderWithWait(receivedMessageListOnMock, EventMessageTypes.SUBSCRIBE,
				EventMessageTypes.SUBSCRIBE, EventMessageTypes.SUBSCRIBE, EventMessageTypes.SUBSCRIBE);
		receivedMessageListOnMock.clear();
	}

	@Override
	protected MonitoringDbConnectionInfo getExpectedDbStatus() {
		return new MonitoringDbConnectionInfoBuilder(super.getExpectedDbStatus())
				.aerospike(MonitoringConnectionStatus.NC)
				.elastic(MonitoringConnectionStatus.NC).build();
	}
}
