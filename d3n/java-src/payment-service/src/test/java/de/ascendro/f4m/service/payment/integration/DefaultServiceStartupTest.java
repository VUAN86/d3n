package de.ascendro.f4m.service.payment.integration;

import static de.ascendro.f4m.service.integration.F4MAssert.assertReceivedMessagesAnyOrderWithWait;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.integration.test.ServiceStartupTest;
import de.ascendro.f4m.service.payment.PaymentServiceStartup;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringConnectionStatus;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringDbConnectionInfo;
import de.ascendro.f4m.service.util.MonitoringDbConnectionInfoBuilder;

public class DefaultServiceStartupTest extends ServiceStartupTest {

	@Override
	public void setUp() throws Exception {
		super.setUp();
		config.setProperty(PaymentConfig.MOCK_MODE, true);
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new PaymentServiceStartup(DEFAULT_TEST_STAGE);
	}

	@Override
	public Class<? extends F4MConfigImpl> getServiceConfigClass() {
		return PaymentConfig.class;
	}
	
	@Override
	protected MonitoringDbConnectionInfo getExpectedDbStatus() {
		return new MonitoringDbConnectionInfoBuilder(super.getExpectedDbStatus())
				.aerospike(MonitoringConnectionStatus.NC).build();
	}

	@Override
	protected void verifyAndCleanStartupMessagesReceivedOnMock() {
		assertReceivedMessagesAnyOrderWithWait(receivedMessageListOnMock, EventMessageTypes.SUBSCRIBE);
		receivedMessageListOnMock.clear();
	}
}