package de.ascendro.f4m.service.util;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.net.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.gson.JsonObject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.store.EventSubscriptionStore;
import de.ascendro.f4m.service.json.JsonMessageDeserializer;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;
import de.ascendro.f4m.service.json.validator.JsonMessageValidator;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.exception.F4MNoServiceRegistrySpecifiedException;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class EventServiceUtilTest {

	private static final String EVENT_SERVICE_NAME = EventMessageTypes.SERVICE_NAME;
	private static final URI EVENT_SERVICE_URI = URI.create("wss://127.0.0.1:8890");
	private static final ServiceConnectionInformation EVENT_SERVICE_INFO = new ServiceConnectionInformation(EVENT_SERVICE_NAME, EVENT_SERVICE_URI, EventMessageTypes.NAMESPACE);

	private static final String TEST_TOPIC = "test";
	
	@Mock
	JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;

	JsonMessageUtil jsonUtil;

	@Mock
	ServiceRegistryClient serviceRegistryClient;

	@Mock
	EventSubscriptionStore eventSubscriptionStore;
	@Mock
	LoggingUtil loggingUtil;

	Config config = new F4MConfigImpl();

	private EventServiceClientImpl eventServiceClient;

	private int counter;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		when(serviceRegistryClient.getServiceConnectionInformation(EVENT_SERVICE_NAME)).thenReturn(EVENT_SERVICE_INFO);
		
		jsonUtil = new JsonMessageUtil(new GsonProvider(new JsonMessageDeserializer(new EventMessageTypeMapper())), 
				new ServiceUtil(), new JsonMessageValidator(new DefaultMessageSchemaMapper(), config));
		
		eventServiceClient = new EventServiceClientImpl(jsonWebSocketClientSessionPool, jsonUtil, serviceRegistryClient,
				eventSubscriptionStore, config, loggingUtil);
		counter = 0;
	}
	
	
	@After
	public void tearDown() {
		eventServiceClient.stopEventServiceMessageQueueProcessing();
	}

	@Test
	public void testResubscribe() throws Exception {
		eventServiceClient.resubscribe();
		RetriedAssertForDefaultService.assertWithWait(() -> verify(jsonWebSocketClientSessionPool).sendAsyncMessage(eq(EVENT_SERVICE_INFO), 
				argThat(hasProperty("content", notNullValue()))));
	}

	@Test
	public void testEventSendingFails_RetryTimeoutTriggersResending() {
		config.setProperty(F4MConfigImpl.EVENT_SERVICE_DISCOVERY_RETRY_DELAY, 200);
		prepareSendMock();

		eventServiceClient.publish(TEST_TOPIC, new JsonObject());
		RetriedAssertForDefaultService.assertWithWait(() -> assertTrue(counter == 2)); // 2 attempts to send messages (1 failure)
	}
	
	@Test
	public void testEventSendingFails_ConnectedEventServiceTriggersResending() {
		config.setProperty(F4MConfigImpl.EVENT_SERVICE_DISCOVERY_RETRY_DELAY, 20000); // 20s - too long to actually run out
		prepareSendMock();
		
		eventServiceClient.publish(TEST_TOPIC, new JsonObject());
		RetriedAssertForDefaultService.assertWithWait(() -> assertTrue(counter == 1));
		eventServiceClient.signalThatEventServiceConnected();
		RetriedAssertForDefaultService.assertWithWait(() -> assertTrue(counter == 2)); // 2 attempts to send messages (1 failure)
	}
	
	@Test
	public void testEventSendingFails_NewMessageTriggersResending() {
		config.setProperty(F4MConfigImpl.EVENT_SERVICE_DISCOVERY_RETRY_DELAY, 20000); // 20s - too long to actually run out
		prepareSendMock();

		eventServiceClient.publish(TEST_TOPIC, new JsonObject());
		RetriedAssertForDefaultService.assertWithWait(() -> assertTrue(counter == 1));
		eventServiceClient.publish(TEST_TOPIC, new JsonObject());
		RetriedAssertForDefaultService.assertWithWait(() -> assertTrue(counter == 3)); // 3 attempts to send messages (1 failure) 
	}
	
	private void prepareSendMock() {
		assertTrue(counter == 0); // to be sure
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				if (counter++ == 0) {
					throw new F4MNoServiceRegistrySpecifiedException(""); // First time throw exception
				}
				return null;
			}
		}).when(jsonWebSocketClientSessionPool).sendAsyncMessage(any(ServiceConnectionInformation.class), Mockito.<JsonMessage<? extends JsonMessageContent>>any());
	}
	
}
