package de.ascendro.f4m.service.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.ping.PingPongMessageSchemaMapper;
import de.ascendro.f4m.service.ping.PingPongMessageTypeMapper;
import de.ascendro.f4m.service.ping.model.JsonPingMessageContent;
import de.ascendro.f4m.service.ping.model.JsonPongMessageContent;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;
import de.ascendro.f4m.service.util.ServiceUtil;

public class ServiceStartupPingPongTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceStartupPingPongTest.class);

	private static final long PING_SEQ = 506L;
	private static final String PING_TEXT = "PING_TEXT-" + PING_SEQ;
	private static final String PONG_TEXT = "PONG_TEXT-" + PING_SEQ;

	static final CountDownLatch pingPongCounterLock = new CountDownLatch(2);

	private ServiceStartup serviceStartup;
	
	@ClassRule
	public static final TemporaryFolder keystoreFolder = new TemporaryFolder();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		KeyStoreTestUtil.initKeyStore(keystoreFolder);
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		final AbstractModule webSocketModule = new AbstractModule() {

			@Override
			protected void configure() {
				bind(JsonMessageTypeMap.class).to(PingPongMessageTypeMapper.class);
				bind(JsonMessageSchemaMap.class).to(PingPongMessageSchemaMapper.class);

				bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class)
						.to(ClientJsonAuthenticationMessageHandlerProvider.class)
						.in(Singleton.class);
				bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class)
						.to(ServerJsonAuthenticationMessageHandlerProvider.class)
						.in(Singleton.class);
			}
		};
		serviceStartup = new ServiceStartup(Stage.DEVELOPMENT) {
			@Override
			public Injector createInjector(Stage stage) {
				return Guice.createInjector(Modules.override(super.getModules())
						.with(getModules()));
			}

			@Override
			protected Iterable<? extends Module> getModules() {
				return Arrays.asList(webSocketModule);
			}

			@Override
			protected String getServiceName() {
				return "F4M-DEFAULT-SERVICE";
			}
		};
		serviceStartup.startupJetty();
		
		serviceStartup.getInjector().getInstance(Config.class)
				.setProperty(F4MConfigImpl.REQUEST_SERVICE_CONNECTION_INFO_ON_CONNECTION_CLOSE, false);
	}
	
	@After
	public void tearDown() throws Exception {
		serviceStartup.stopJetty();
	}

	@Test
	public void testPingPong() throws Exception {
		final Injector injector = serviceStartup.getInjector();

		final ServiceUtil serviceUtil = injector.getInstance(ServiceUtil.class);
		final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool =
				injector.getInstance(JsonWebSocketClientSessionPool.class);
		final JsonMessage<JsonPingMessageContent> pingMessage =
				getPingMessage(String.valueOf(serviceUtil.generateId()), serviceUtil.getMessageTimestamp());

		final ServiceConnectionInformation serviceConnInfo =
				new ServiceConnectionInformation("test", "wss://localhost:8443/", (String) null);
		jsonWebSocketClientSessionPool.sendAsyncMessage(serviceConnInfo, pingMessage);

		pingPongCounterLock.await(2, TimeUnit.SECONDS);
		assertEquals(1, ServerJsonAuthenticationMessageHandler.getReceivedMessageList()
				.size());
		assertEquals(1, ClientJsonAuthenticationMessageHandler.getReceivedMessageList()
				.size());

		JsonMessage<? extends JsonMessageContent> clientReceivedMessage =
				ClientJsonAuthenticationMessageHandler.getReceivedMessageList()
						.get(0);

		assertEquals(pingMessage.getSeq(), clientReceivedMessage.getFirstAck());
		assertEquals(pingMessage.getClientId(), clientReceivedMessage.getClientId());
		assertNotNull(clientReceivedMessage.getTimestamp());
		assertTrue(pingMessage.getTimestamp() <= clientReceivedMessage.getTimestamp());
	}

	private JsonMessage<JsonPingMessageContent> getPingMessage(String clientId, Long timestamp) {
		final JsonMessage<JsonPingMessageContent> pingMessage =
				new JsonMessage<JsonPingMessageContent>(JsonPingMessageContent.MESSAGE_NAME);
		pingMessage.setSeq(PING_SEQ);

		pingMessage.setClientId(clientId);
		pingMessage.setContent(new JsonPingMessageContent(PING_TEXT));
		pingMessage.setTimestamp(timestamp);

		return pingMessage;
	}

	static class ServerJsonAuthenticationMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {

		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new ServerJsonAuthenticationMessageHandler();
		}
	}

	static class ServerJsonAuthenticationMessageHandler extends DefaultJsonMessageHandler {
		private static List<JsonMessage<? extends JsonMessageContent>> receivedMessageList =
				new CopyOnWriteArrayList<>();

		@Override
		public ClientInfo onAuthentication(RequestContext context) {
			return null;
		}
		
		@Override
		public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
			LOGGER.info("ServerJsonAuthenticationMessageHandler.onUserMessage: " + originalMessageDecoded);

			receivedMessageList.add(originalMessageDecoded);
			pingPongCounterLock.countDown();

			return new JsonPongMessageContent(PONG_TEXT);
		}

		public static List<JsonMessage<? extends JsonMessageContent>> getReceivedMessageList() {
			return receivedMessageList;
		}

		public void clearReceivedMessageList() {
			receivedMessageList.clear();
		}
	}

	static class ClientJsonAuthenticationMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {

		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new ClientJsonAuthenticationMessageHandler();
		}
	}

	static class ClientJsonAuthenticationMessageHandler extends DefaultJsonMessageHandler {
		private static List<JsonMessage<? extends JsonMessageContent>> receivedMessageList =
				new CopyOnWriteArrayList<>();

		@Override
		public ClientInfo onAuthentication(RequestContext context) {
			return null;
		}
		
		@Override
		public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
			LOGGER.info("ClientJsonAuthenticationMessageHandler.onUserMessage: " + originalMessageDecoded);

			receivedMessageList.add(originalMessageDecoded);
			pingPongCounterLock.countDown();

			return null;// stop ping-pong by responsing with nothing
		}

		public static List<JsonMessage<? extends JsonMessageContent>> getReceivedMessageList() {
			return receivedMessageList;
		}

		public void clearReceivedMessageList() {
			receivedMessageList.clear();
		}

	}

}
