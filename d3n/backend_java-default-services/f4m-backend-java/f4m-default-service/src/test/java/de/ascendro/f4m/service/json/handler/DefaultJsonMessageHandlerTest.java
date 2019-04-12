package de.ascendro.f4m.service.json.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypes;
import de.ascendro.f4m.service.json.JsonMessageDeserializer;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.validator.JsonMessageValidator;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypes;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.registry.model.ServiceRegistryGetResponse;
import de.ascendro.f4m.service.registry.model.ServiceRegistryHeartbeatResponse;
import de.ascendro.f4m.service.registry.model.ServiceStatus;
import de.ascendro.f4m.service.registry.store.ServiceStore;
import de.ascendro.f4m.service.registry.store.ServiceStoreImpl;
import de.ascendro.f4m.service.session.JettySessionWrapper;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.pool.SessionStore;
import de.ascendro.f4m.service.session.pool.SessionStoreImpl;
import de.ascendro.f4m.service.util.ServiceUtil;
import de.ascendro.f4m.service.util.register.HeartbeatMonitoringProvider;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class DefaultJsonMessageHandlerTest {
	private static final String CLIENT_ID = UUID.randomUUID().toString();

	private Config config;
	private JsonMessageUtil jsonUtil;
	private ServiceUtil serviceUtil;
	private GsonProvider gsonProvider;
	private ServiceStore<ServiceConnectionInformation> serviceStore;

	@Mock
	private JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;

	@Mock
	private JsonMessageValidator jsonMessageValidator;
	@Mock
	private LoggingUtil loggingUtil;

	@Mock
	private ServiceRegistryClient serviceRegistryClient;
	
	@Mock
	private SessionWrapper sessionWrapper;
	@Mock
	private HeartbeatMonitoringProvider serviceMonitoringStatus;

	private DefaultJsonMessageHandler defaultService;

	private SessionStore sessionStore;

	@SuppressWarnings("serial")
	@Before
	public void setUp() throws Exception {
		config = new F4MConfigImpl();
		sessionStore = new SessionStoreImpl(config, loggingUtil, sessionWrapper);
		serviceUtil = new ServiceUtil();
		serviceStore = new ServiceStoreImpl();
		gsonProvider = new GsonProvider(new JsonMessageDeserializer(new DefaultJsonMessageMapper(
				new ServiceRegistryMessageTypeMapper(), new GatewayMessageTypeMapper()) {
		}));
		MockitoAnnotations.initMocks(this);

		doReturn(serviceStore).when(serviceRegistryClient).getServiceStore();

		jsonUtil = new JsonMessageUtil(gsonProvider, serviceUtil, jsonMessageValidator);

		defaultService = spy(createDefaultJsonMessageHandler(serviceRegistryClient, config));
	}

	@Test
	public void testOnUserMessageDefault() throws F4MException {
		final JsonMessageContent response = defaultService
				.onUserDefaultMessage(new JsonMessage<JsonMessageContent>(ServiceRegistryMessageTypes.HEARTBEAT));

		assertNotNull(response);
		assertTrue(response instanceof ServiceRegistryHeartbeatResponse);
		assertEquals(ServiceStatus.GOOD.name(), ((ServiceRegistryHeartbeatResponse) response).getStatus());
	}

	@Test(expected = F4MException.class)
	public void testOnUserMessageDefaultUnsupportedType() throws F4MException {
		defaultService.onUserDefaultMessage(new JsonMessage<JsonMessageContent>(ServiceRegistryMessageTypes.GET));
	}

	@Test
	public void testOnHeartbeat() {
		final JsonMessageContent response = defaultService.onHeartbeat(null);
		assertNotNull(response);
		assertTrue(response instanceof ServiceRegistryHeartbeatResponse);
		assertEquals(ServiceStatus.GOOD.name(), ((ServiceRegistryHeartbeatResponse) response).getStatus());
	}

	@Test
	public void testOnGatewayServiceMessage() throws F4MException {
		config.setProperty(F4MConfigImpl.USERS_CACHE_TIME_TO_LIVE, Long.MAX_VALUE);
		final JsonMessage<JsonMessageContent> clientDiconnectedMessage = new JsonMessage<JsonMessageContent>(
				"Unknown type");
		clientDiconnectedMessage.setClientId(CLIENT_ID);

		final SessionStore sessionStore = defaultService.getSessionWrapper().getSessionStore();
		sessionStore.registerClient(CLIENT_ID);

		assertTrue(sessionStore.hasClient(CLIENT_ID));

		defaultService.onGatewayServiceMessage(GatewayMessageTypes.CLIENT_DISCONNECT, clientDiconnectedMessage);

		assertFalse(sessionStore.hasClient(CLIENT_ID));
	}

	@Test
	public void testOnGetResponse() throws F4MException {
		final String serviceName = "TestService", uri = "wss://test:85885";

		final JsonMessage<ServiceRegistryGetResponse> getResponseMessage = jsonUtil
				.createNewMessage(ServiceRegistryMessageTypes.GET_RESPONSE);
		final ServiceConnectionInformation serviceConnInfo = new ServiceConnectionInformation(serviceName, uri, serviceName);
		getResponseMessage.setContent(new ServiceRegistryGetResponse(
				serviceConnInfo));
		final JsonMessageContent result = defaultService
				.onServiceRegistryMessage(ServiceRegistryMessageTypes.GET_RESPONSE, getResponseMessage);

		assertNull(result);

		verify(serviceRegistryClient, times(1)).addService(serviceConnInfo);
	}

	@Test
	public void testScheduledServiceInfoRequestCall() throws F4MException {
		final String serviceName = "TestService";

		final JsonMessage<ServiceRegistryGetResponse> getResponseMessage = jsonUtil
				.createNewMessage(ServiceRegistryMessageTypes.GET_RESPONSE);
		getResponseMessage.setContent(new ServiceRegistryGetResponse(
				new ServiceConnectionInformation(serviceName, null, Collections.emptyList())));
		when(serviceRegistryClient.getServiceConnectionInformation(serviceName)).thenReturn(null);
		final JsonMessageContent result = defaultService
				.onServiceRegistryMessage(ServiceRegistryMessageTypes.GET_RESPONSE, getResponseMessage);

		assertNull(result);
		assertNull(serviceStore.getService(serviceName));
		verify(serviceRegistryClient, times(1)).getServiceConnectionInformation(serviceName);
	}

	@Test(expected = F4MException.class)
	public void testOnGetResponseNoService() throws F4MException {

		final JsonMessage<ServiceRegistryGetResponse> getResponseMessage = jsonUtil
				.createNewMessage(ServiceRegistryMessageTypes.GET_RESPONSE);
		getResponseMessage
				.setContent(new ServiceRegistryGetResponse(new ServiceConnectionInformation()));
		final JsonMessageContent result = defaultService
				.onServiceRegistryMessage(ServiceRegistryMessageTypes.GET_RESPONSE, getResponseMessage);

		assertNotNull(result);
	}

	private DefaultJsonMessageHandler createDefaultJsonMessageHandler(ServiceRegistryClient serviceRegistryClient, Config config) {
		final DefaultJsonMessageHandler defaultJsonMessageHandler = new DefaultJsonMessageHandler() {

			@Override
			public SessionWrapper getSessionWrapper() {
				return new JettySessionWrapper(null, null, config, jsonMessageUtil, loggingUtil) {
					@SuppressWarnings("unchecked")
					@Override
					public <S extends SessionStore> S getSessionStore() {
						return (S) sessionStore;
					}
				};
			}
		};
		defaultJsonMessageHandler.setConfig(config);
		defaultJsonMessageHandler.setServiceRegistryClient(serviceRegistryClient);
		defaultJsonMessageHandler.setServiceMonitoringStatus(serviceMonitoringStatus);

		return defaultJsonMessageHandler;
	}
}
