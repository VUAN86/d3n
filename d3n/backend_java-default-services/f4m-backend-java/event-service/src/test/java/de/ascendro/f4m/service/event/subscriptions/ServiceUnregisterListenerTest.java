package de.ascendro.f4m.service.event.subscriptions;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import de.ascendro.f4m.service.event.activemq.BrokerNetworkActiveMQ;
import de.ascendro.f4m.service.event.config.EventConfig;
import de.ascendro.f4m.service.event.model.publish.PublishMessageContent;
import de.ascendro.f4m.service.exception.validation.F4MValidationException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.model.ServiceRegistryUnregisterEvent;
import de.ascendro.f4m.service.util.JsonTestUtil;

public class ServiceUnregisterListenerTest {

	@Mock
	private BrokerNetworkActiveMQ brokerNetworkActiveMQ;

	@Mock
	private JsonMessageUtil jsonUtil;

	@Mock
	private TextMessage textMessage;
	@Spy
	private EventConfig config;
	@Mock
	private LoggingUtil loggingUtil;
	@InjectMocks
	private ServiceUnregisterListener serviceUnregisterListener;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testOnMessage() throws IOException, F4MValidationException, JMSException {
		final String publishMessageString = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream(
				"de/ascendro/f4m/service/event/integration/publishEventServiceUnregister.json"), "UTF-8");
		when(textMessage.getText()).thenReturn(publishMessageString);

		final Gson gson = JsonTestUtil.getGson();
		@SuppressWarnings("serial")
		final Type publishMessageContentType = new TypeToken<JsonMessage<PublishMessageContent>>() {
		}.getType();
		final JsonMessage<PublishMessageContent> publishMessage = gson.fromJson(publishMessageString,
				publishMessageContentType);
		final PublishMessageContent publishMessageContent = publishMessage.getContent();
		final JsonElement notifactionContent = publishMessageContent.getNotificationContent();
		final ServiceRegistryUnregisterEvent serviceUnregisterContent = gson.fromJson(notifactionContent,
				ServiceRegistryUnregisterEvent.class);

		doReturn(publishMessage).when(jsonUtil).fromJson(publishMessageString);
		doReturn(serviceUnregisterContent).when(jsonUtil).fromJson(notifactionContent,
				ServiceRegistryUnregisterEvent.class);

		serviceUnregisterListener.onMessage(textMessage);

		verify(brokerNetworkActiveMQ, times(1)).removeBrokerFromNetwork(URI.create(serviceUnregisterContent.getUri()));
	}
}
