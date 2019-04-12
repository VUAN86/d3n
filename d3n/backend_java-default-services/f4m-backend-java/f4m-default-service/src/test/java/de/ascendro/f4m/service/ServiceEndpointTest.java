package de.ascendro.f4m.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.handler.MessageHandlerProvider;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.exception.server.F4MSessionStoreDestroyException;
import de.ascendro.f4m.service.exception.server.F4MSessionStoreInitException;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.SessionWrapperFactory;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class ServiceEndpointTest {
	private static final String SESSION_ID = UUID.randomUUID().toString();

	@Mock
	private MessageHandlerProvider<String> messageHandlerProvider;
	@Mock
	private SessionPool sessionPool;

	@Mock
	private JsonMessageHandler jsonMessageHandler;

	@Mock
	private Session session;

	@Mock
	private EndpointConfig endpointConfig;

	private LoggingUtil loggedMessageUtil;

	@Mock
	private F4MConfigImpl config;

	@Mock
	private ServiceRegistryClient serviceRegistryClient;

	@Mock
	private EventServiceClient eventServiceClient;

	@Mock
	private SessionWrapper sessionWrapper;

	@Mock
	private SessionWrapperFactory sessionWrapperFactory;

	private ServiceEndpoint<String, String, String> serviceEndpoint;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		when(session.getId()).thenReturn(SESSION_ID);
		when(messageHandlerProvider.get()).thenReturn(jsonMessageHandler);
		when(jsonMessageHandler.getSessionWrapper()).thenReturn(sessionWrapper);
		when(session.getMessageHandlers()).thenReturn(new HashSet<MessageHandler>(Arrays.asList(jsonMessageHandler)));
		when(config.getPropertyAsBoolean(F4MConfigImpl.SERVICE_RECONNECT_ON_SERVICE_REGISTRY_CLOSE)).thenReturn(true);
		when(config.getPropertyAsBoolean(F4MConfigImpl.EVENT_SERVICE_CLIENT_AUTO_RESUBSCRIBE_ON_CLOSE)).thenReturn(true);

		when(sessionWrapperFactory.create(any())).thenReturn(sessionWrapper);

		loggedMessageUtil = new LoggingUtil(config);

		serviceEndpoint = new ServiceEndpoint<String, String, String>(messageHandlerProvider, sessionPool,
				loggedMessageUtil, sessionWrapperFactory, config, serviceRegistryClient, eventServiceClient);
	}

	@Test
	public void testOnOpen() throws F4MSessionStoreInitException {
		serviceEndpoint.onOpen(session, endpointConfig);
		verify(jsonMessageHandler, times(1)).setSession(session);
		verify(sessionPool, times(1)).createSession(SESSION_ID, sessionWrapper);
		verify(session, times(1)).addMessageHandler(jsonMessageHandler);
	}

	@Test
	public void testOnClose() throws F4MSessionStoreDestroyException {
		serviceEndpoint.onClose(session, new CloseReason(CloseCodes.GOING_AWAY, "Testing onClose via GOING_AWAY"));
		verify(jsonMessageHandler, times(1)).destroy();
		verify(sessionPool, times(1)).removeSession(SESSION_ID);
	}

	@Test
	public void testOnClientServiceRegistryClose() throws F4MSessionStoreDestroyException {
		final URI localClientURI = URI.create("wss://127.0.0.1:8889");

		when(sessionWrapper.isClient()).thenReturn(true);
		when(sessionWrapper.getLocalClientSessionURI()).thenReturn(localClientURI);
		when(serviceRegistryClient.containsServiceRegistryUri(localClientURI)).thenReturn(true);

		serviceEndpoint.onClose(session, new CloseReason(CloseCodes.GOING_AWAY, "Testing onClose via GOING_AWAY"));
		verify(jsonMessageHandler, times(1)).destroy();
		verify(sessionPool, times(1)).removeSession(SESSION_ID);
		verify(serviceRegistryClient, times(1)).register();
	}

	@Test
	public void testOnClientUnknownServiceRegistryClose() throws F4MSessionStoreDestroyException {
		final URI localClientURI = URI.create("wss://127.0.0.1:8889");

		when(sessionWrapper.isClient()).thenReturn(true);
		when(sessionWrapper.getLocalClientSessionURI()).thenReturn(localClientURI);
		when(serviceRegistryClient.containsServiceRegistryUri(localClientURI)).thenReturn(false);

		serviceEndpoint.onClose(session, new CloseReason(CloseCodes.GOING_AWAY, "Testing onClose via GOING_AWAY"));
		verify(jsonMessageHandler, times(1)).destroy();
		verify(sessionPool, times(1)).removeSession(SESSION_ID);
		verify(serviceRegistryClient, times(0)).register();
	}

	@Test
	public void testOnEventServiceCloseResubscribe() {
		final String eventServiceName = EventMessageTypes.SERVICE_NAME;
		final URI eventServiceURI = URI.create("wss://127.0.0.1:8890");

		when(sessionWrapper.isClient()).thenReturn(true);
		when(sessionWrapper.getLocalClientSessionURI()).thenReturn(eventServiceURI);
		when(serviceRegistryClient.getServiceConnInfoFromStore(eventServiceName)).thenReturn(
				new ServiceConnectionInformation(eventServiceName, eventServiceURI.toString(), Collections.singletonList("testNS")));

		serviceEndpoint.onClose(session, new CloseReason(CloseCodes.GOING_AWAY, "Testing onClose via GOING_AWAY"));
		verify(eventServiceClient, times(1)).resubscribe();
	}

}
