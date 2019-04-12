package de.ascendro.f4m.service.voucher.integration;

import static de.ascendro.f4m.service.integration.F4MAssert.assertReceivedMessagesAnyOrderWithWait;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.integration.test.ServiceStartupTest;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringConnectionStatus;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringDbConnectionInfo;
import de.ascendro.f4m.service.util.MonitoringDbConnectionInfoBuilder;
import de.ascendro.f4m.service.voucher.config.VoucherConfig;

public class DefaultServiceStartupTest extends ServiceStartupTest {

	@Override
	protected ServiceStartup getServiceStartup() {
		return new VoucherServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE);
	}

	@Override
	public Class<? extends F4MConfigImpl> getServiceConfigClass() {
		return VoucherConfig.class;
	}

	@Override
	protected void verifyAndCleanStartupMessagesReceivedOnMock() {
		assertReceivedMessagesAnyOrderWithWait(receivedMessageListOnMock, EventMessageTypes.SUBSCRIBE);
		receivedMessageListOnMock.clear();
	}

	@Override
	protected MonitoringDbConnectionInfo getExpectedDbStatus() {
		return new MonitoringDbConnectionInfoBuilder(super.getExpectedDbStatus())
				.aerospike(MonitoringConnectionStatus.NC).build();
	}
}