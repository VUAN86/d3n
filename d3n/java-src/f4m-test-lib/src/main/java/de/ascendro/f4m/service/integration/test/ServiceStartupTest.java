package de.ascendro.f4m.service.integration.test;

import static de.ascendro.f4m.service.integration.F4MAssert.assertReceivedMessagesAnyOrderWithWait;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.config.F4MConfig;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.DefaultJsonMessageMapper;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.gateway.GatewayMessageTypeMapper;
import de.ascendro.f4m.service.integration.F4MAssert;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.collector.ReceivedMessageCollector;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.BindableAbstractModule;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.ThrowingFunction;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypes;
import de.ascendro.f4m.service.registry.model.ServiceRegistryHeartbeatResponse;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringConnectionStatus;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringDbConnectionInfo;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringServiceConnectionInfo;
import de.ascendro.f4m.service.registry.model.monitor.PushServiceStatistics;
import de.ascendro.f4m.service.registry.model.monitor.ServiceStatistics;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.session.pool.SessionPoolImpl;
import de.ascendro.f4m.service.session.pool.SessionStore;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.MonitoringDbConnectionInfoBuilder;

public abstract class ServiceStartupTest extends F4MServiceWithMockIntegrationTestBase {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceStartupTest.class);
	
	private ReceivedMessageCollector receivedMessageCollector;
	protected List<JsonMessage<? extends JsonMessageContent>> receivedMessageListOnMock = new CopyOnWriteArrayList<>();

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		receivedMessageCollector = (ReceivedMessageCollector) clientInjector.getInstance(
				Key.get(JsonMessageHandlerProvider.class, ClientMessageHandler.class)).get();

		F4MConfig configF4M = jettyServerRule.getServerStartup().getInjector().getInstance(F4MConfig.class);
		assertSame(config, configF4M);
		F4MConfigImpl configImpl = jettyServerRule.getServerStartup().getInjector().getInstance(F4MConfigImpl.class);
		assertSame(config, configImpl);
		F4MConfigImpl serviceConfig = jettyServerRule.getServerStartup().getInjector().getInstance(getServiceConfigClass());
		assertSame(config, serviceConfig);
	}
	
	/**
	 * Config implementation for specific service.
	 * There was a thought to create such a class in prod code, but until now there seems to be no need for that, so this will be used only for testing.
	 * @return
	 */
	public abstract Class<? extends F4MConfigImpl> getServiceConfigClass();

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(DefaultTypeMapper.class, DefaultMessageSchemaMapper.class);
	}

	@Test
	public void testOnMessageClientDisconnect() throws Exception {
		Assume.assumeThat("Running client disconnect tests only for services where it is enabled",
				config.getPropertyAsLong(F4MConfigImpl.USERS_CACHE_TIME_TO_LIVE), Matchers.greaterThan(0L));
		// Send auth public key
		final String clientId = "testClientId";
		final ClientInfo testClient = ClientInfo.byClientId(clientId);
		
		final String registerRequestJson = jsonLoader
				.getPlainTextJsonFromResources("/integration/register/heartbeat.json", testClient);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), registerRequestJson);

		final SessionPoolImpl sessionPool = (SessionPoolImpl) jettyServerRule.getServerStartup().getInjector()
				.getInstance(SessionPool.class);

		RetriedAssert.assertWithWait(() -> assertTrue(findSessionStoreByClientId(sessionPool, clientId).isPresent()));
		SessionStore sessionStore = findSessionStoreByClientId(sessionPool, clientId).get();

		RetriedAssert.assertWithWait(() -> assertTrue(sessionStore.hasClient(clientId)));
		assertReceivedMessagesWithWait(ServiceRegistryMessageTypes.HEARTBEAT_RESPONSE);
		receivedMessageCollector.clearReceivedMessageList();

		final String gatewayClientDisconnectMessageText = jsonLoader
				.getPlainTextJsonFromResources("/integration/gateway/clientDisconnect.json", testClient);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), gatewayClientDisconnectMessageText);

		RetriedAssert.assertWithWait(() -> assertNotNull(sessionStore));
		RetriedAssert.assertWithWait(() -> assertFalse(sessionStore.hasClient(clientId)));
		RetriedAssert.assertWithWait(() -> assertTrue(receivedMessageCollector.getReceivedMessageList().isEmpty()));
	}

	private Optional<SessionStore> findSessionStoreByClientId(final SessionPoolImpl sessionPool, final String clientId) {
		return sessionPool.values().stream().filter(ss -> ss.hasClient(clientId)).findAny();
	}

	@Test
	public void testOnMessageHeartbeat() throws Exception {
		verifyAndCleanStartupMessagesReceivedOnMock();
		
		// Send auth public key
		final String registerRequestJson = jsonLoader
				.getPlainTextJsonFromResources("/integration/register/heartbeat.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), registerRequestJson);

		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));

		final JsonMessage<?> heartbeatResponseMessage = receivedMessageCollector.getReceivedMessageList().get(0);
		assertMessageContentType(heartbeatResponseMessage, ServiceRegistryHeartbeatResponse.class);
		assertNotNull(heartbeatResponseMessage.getContent());
		
		verifyMonitoring();
	}

	protected void verifyMonitoring() {
		jettyServerRule.getServerStartup().startMonitoring();
		assertReceivedMessagesAnyOrderWithWait(receivedMessageListOnMock,
				ServiceRegistryMessageTypes.PUSH_SERVICE_STATISTICS);
		JsonMessageContent content = receivedMessageListOnMock.get(0).getContent();
		assertThat(content, instanceOf(PushServiceStatistics.class));
		ServiceStatistics statistics = ((PushServiceStatistics)content).getStatistics();
		LOGGER.debug("Received statistics {}", statistics);
		assertThat(statistics.getLastHeartbeatTimestamp(), Matchers.lessThanOrEqualTo(DateTimeUtil.getUTCTimestamp()));
		//assert other properties
		verifyConnectionsFromServiceToOther(statistics);
		verifyConnectionsToServiceFromOther(statistics);
		verifyConnectionsToDbStatus(statistics);
	}
	
	protected void verifyConnectionsToServiceFromOther(ServiceStatistics statistics) {
		F4MAssert.assertSize(1, statistics.getConnectionsToService());
		MonitoringServiceConnectionInfo testClientConnectionInfo = statistics.getConnectionsToService().get(0);
		assertEquals(F4MIntegrationTestBase.TEST_CLIENT_SERVICE_NAME, testClientConnectionInfo.getServiceName());
		assertThat(testClientConnectionInfo.getAddress(), not(isEmptyOrNullString())); //address - should be "127.0.0.1", but might change with address resolving
		assertNotNull(testClientConnectionInfo.getPort()); //client port is always picked up on random, so cannot test it directly
	}

	protected void verifyConnectionsFromServiceToOther(ServiceStatistics statistics) {
		F4MAssert.assertSize(1, statistics.getConnectionsFromService());
		MonitoringServiceConnectionInfo testClientConnectionInfo = statistics.getConnectionsFromService().get(0);
		assertEquals(ServiceRegistryMessageTypes.SERVICE_NAME, testClientConnectionInfo.getServiceName());
		//TODO: change URL&PORT as a single attribute URL:PORT according in the same manner as for registry/register call?
		assertEquals("localhost", testClientConnectionInfo.getAddress()); //replace with DUMMY_SERVICE_URI
		assertEquals(MOCK_JETTY_PORT, (int) testClientConnectionInfo.getPort());
	}
	
	protected void verifyConnectionsToDbStatus(ServiceStatistics statistics) {
		MonitoringDbConnectionInfo expectedStatus = getExpectedDbStatus();
		assertEquals(expectedStatus.getAerospike(), statistics.getConnectionsToDb().getAerospike());
		assertEquals(expectedStatus.getElastic(), statistics.getConnectionsToDb().getElastic());
		assertEquals(expectedStatus.getMysql(), statistics.getConnectionsToDb().getMysql());
		assertEquals(expectedStatus.getSpark(), statistics.getConnectionsToDb().getSpark());
	}
	
	protected MonitoringDbConnectionInfo getExpectedDbStatus() {
		return new MonitoringDbConnectionInfoBuilder(new MonitoringDbConnectionInfo())
				.aerospike(MonitoringConnectionStatus.NA)
				.elastic(MonitoringConnectionStatus.NA)
				.mysql(MonitoringConnectionStatus.NA)
				.spark(MonitoringConnectionStatus.NA).build();
	}

	protected void verifyAndCleanStartupMessagesReceivedOnMock() {
		//should be extended in subclasses for verifying subscriptions to events etc.
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new ServiceStartup(DEFAULT_TEST_STAGE) {
			@Override
			protected String getServiceName() {
				return "DEFAULT-SERVICE-BASE";
			}

			@Override
			protected Iterable<? extends Module> getModules() {
				return Arrays.asList(new AbstractModule() {
					@Override
					protected void configure() {
						bind(JsonMessageTypeMap.class).to(DefaultTypeMapper.class).in(Singleton.class);
						bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class)
								.to(DefaultJsonMessageHandlerProviderBase.class).in(Singleton.class);
						bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class)
								.to(DefaultJsonMessageHandlerProviderBase.class).in(Singleton.class);

					}
				});
			}

			@Override
			public Injector createInjector(Stage stage) {
				return Guice.createInjector(Modules.override(super.getModules()).with(getModules()));
			}
		};
	}

	static class DefaultTypeMapper extends DefaultJsonMessageMapper {
		private static final long serialVersionUID = -6806591530881427763L;

		@Inject
		public DefaultTypeMapper(ServiceRegistryMessageTypeMapper serviceRegistryMessageTypeMapper,
				GatewayMessageTypeMapper gatewayMessageTypeMapper, EventMessageTypeMapper eventMessageTypeMapper) {
			super(serviceRegistryMessageTypeMapper, gatewayMessageTypeMapper);
			init(eventMessageTypeMapper);
		}
	}

	static class DefaultJsonMessageHandlerProviderBase extends DefaultJsonMessageHandlerProviderImpl {

		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new DefaultJsonMessageHandler(){};
		}
	}

	@Override
	protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandlerMock() {
		return ctx -> onMockReceivedMessage(ctx.getMessage());
	}
	
	protected JsonMessageContent onMockReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded)
			throws Exception {
		LOGGER.info("Mock service received message {}", originalMessageDecoded);
		receivedMessageListOnMock.add(originalMessageDecoded);
		return null;
	}
	

	@Override
	protected void configureInjectionModuleMock(BindableAbstractModule module) {
		module.bindToModule(JsonMessageTypeMap.class).to(DefaultTypeMapper.class);
	}
}
