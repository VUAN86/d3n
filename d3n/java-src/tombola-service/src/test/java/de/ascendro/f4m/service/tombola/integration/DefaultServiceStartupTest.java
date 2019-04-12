package de.ascendro.f4m.service.tombola.integration;

import static de.ascendro.f4m.service.integration.F4MAssert.assertReceivedMessagesAnyOrderWithWait;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.subscribe.SubscribeRequest;
import de.ascendro.f4m.service.integration.test.ServiceStartupTest;
import de.ascendro.f4m.service.profile.model.merge.ProfileMergeEvent;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringConnectionStatus;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringDbConnectionInfo;
import de.ascendro.f4m.service.tombola.config.TombolaConfig;
import de.ascendro.f4m.service.tombola.model.events.TombolaEvents;
import de.ascendro.f4m.service.util.MonitoringDbConnectionInfoBuilder;

public class DefaultServiceStartupTest extends ServiceStartupTest {
    @Override
    protected ServiceStartup getServiceStartup() {
        return new TombolaServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE);
    }
	
	@Override
	protected MonitoringDbConnectionInfo getExpectedDbStatus() {
		return new MonitoringDbConnectionInfoBuilder(super.getExpectedDbStatus())
				.aerospike(MonitoringConnectionStatus.NC).build();
	}

	@Override
	public Class<? extends F4MConfigImpl> getServiceConfigClass() {
		return TombolaConfig.class;
	}

	@Override
	protected void verifyAndCleanStartupMessagesReceivedOnMock() {
		assertReceivedMessagesAnyOrderWithWait(receivedMessageListOnMock, EventMessageTypes.SUBSCRIBE,
				EventMessageTypes.SUBSCRIBE, EventMessageTypes.SUBSCRIBE, EventMessageTypes.SUBSCRIBE);
		List<? extends String> subscribeTopics = receivedMessageListOnMock.stream()
				.map(m -> ((SubscribeRequest) m.getContent()).getTopic()).collect(Collectors.toList());
		assertThat(subscribeTopics, containsInAnyOrder(
				ProfileMergeEvent.PROFILE_MERGE_EVENT_TOPIC,
				TombolaEvents.getCloseCheckoutTopic(),
				TombolaEvents.getDrawEventTopic(), 
				TombolaEvents.getOpenCheckoutTopic()
				//TombolaEvents.TOMBOLA_PRE_CLOSE_CHECKOUT_EVENT_TOPIC_PREFIX - no occurence of this
				));
		receivedMessageListOnMock.clear();
	}
}
