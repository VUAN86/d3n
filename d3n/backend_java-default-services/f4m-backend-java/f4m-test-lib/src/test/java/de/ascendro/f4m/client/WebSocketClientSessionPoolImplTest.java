package de.ascendro.f4m.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import javax.inject.Provider;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Session;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.ServiceEndpoint;
import de.ascendro.f4m.service.config.F4MConfig;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.integration.F4MAssert;
import de.ascendro.f4m.service.integration.rule.MockServiceRule;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.JsonServiceEndpoint;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.session.JettySessionWrapper;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.SessionWrapperFactory;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class WebSocketClientSessionPoolImplTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketClientSessionPoolImplTest.class);

	private static final int THREAD_COUNT = 1000;

	private static final int MOCK_JETTY_PORT = 8897;
	private static final URI DUMMY_SERVICE_URI = URI.create("wss://localhost:" + MOCK_JETTY_PORT);
	private static final ServiceConnectionInformation DUMMY_SERVICE_INFO = new ServiceConnectionInformation("DUMMY",
			DUMMY_SERVICE_URI, "anyNamespace");

	@ClassRule
	public static final TemporaryFolder keystoreFolder = new TemporaryFolder();

	// Pseudo service which accepts messages
	@Rule
	public MockServiceRule mockService = new MockServiceRule(MOCK_JETTY_PORT, null, null) {
		@Override
		protected void configureInjectionModule(BindableAbstractModule module) {
			// mapper does not matter as no messages are sent
			module.bindToModule(JsonMessageTypeMap.class).to(ServiceRegistryMessageTypeMapper.class);
		}

		@Override
		protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandler() {
			return ctx -> onReceivedMessage(ctx.getMessage());
		}
	};

	@Mock
	private Provider<? extends ServiceEndpoint<?, ?, ?>> endpointProvider;

	@Mock
	private SessionWrapper sessionWrapper;

	private SessionWrapperFactory sessionWrapperFactory;

	@Mock
	private JsonMessageHandlerProvider jsonMessageHandlerProvider;

	@Mock
	private JsonMessageHandler jsonMessageHandler;

	@Mock
	private SessionPool sessionPool;

	@Mock
	private LoggingUtil loggedMessageUtil;

	private F4MConfig config;

	@Mock
	private JsonMessageUtil jsonUtil;

	@Mock
	private ServiceRegistryClient serviceRegistryClient;

	@Mock
	private EventServiceClient eventServiceClient;

	private WebSocketClientSessionPoolImpl webSocketClientSessionPoolImpl;
	private final AtomicBoolean succeedToConnect = new AtomicBoolean(true);

	@BeforeClass
	public static void setUpClass() throws IOException {
		System.setProperty(F4MConfigImpl.SERVICE_REGISTRY_URI, DUMMY_SERVICE_URI.toString());
		KeyStoreTestUtil.initKeyStore(keystoreFolder);
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		config = new F4MConfigImpl();

		sessionWrapperFactory = new SessionWrapperFactory() {

			@Override
			public SessionWrapper create(Session session) {
				return new JettySessionWrapper(session, sessionPool, config, jsonUtil, loggedMessageUtil);
			}
		};

		when(jsonMessageHandlerProvider.get()).thenReturn(jsonMessageHandler);
		when(jsonMessageHandler.getSessionWrapper()).thenReturn(sessionWrapper);
		endpointProvider = new Provider<ServiceEndpoint<?, ?, ?>>() {

			@Override
			public ServiceEndpoint<?, ?, ?> get() {
				return new JsonServiceEndpoint(jsonMessageHandlerProvider, sessionPool, loggedMessageUtil,
						sessionWrapperFactory, config, serviceRegistryClient, eventServiceClient);
			}
		};

		webSocketClientSessionPoolImpl = new WebSocketClientSessionPoolImpl(endpointProvider, sessionWrapperFactory,
				config) {
			@Override
			public Session connect(ServiceConnectionInformation serviceConnectionInformation) {
				if (succeedToConnect.get()) {
					return super.connect(serviceConnectionInformation);
				} else {
					throw new F4MIOException("Failed to connected by flag: false");
				}
			}
		};
	}

	@Test
	public void testGetSessionWithFailureToConnect() throws Exception {
		succeedToConnect.set(false);
		try {
			webSocketClientSessionPoolImpl.getSession(DUMMY_SERVICE_INFO);
			fail("Should have F4MIOException exception");
		} catch (F4MIOException ioEx) {
		}
		F4MAssert.assertSize(0, webSocketClientSessionPoolImpl.getOpenSessions());

		succeedToConnect.set(true);
		assertNotNull(webSocketClientSessionPoolImpl.connect(DUMMY_SERVICE_INFO));
	}

	@Test
	public void testGetSessionWithMultipleRequests() throws InterruptedException, IOException {
		final List<String> sessionsIds = new CopyOnWriteArrayList<>();
		final List<Throwable> errors = new CopyOnWriteArrayList<>();

		final Thread[] threads = new Thread[THREAD_COUNT];

		// Create threads
		for (int i = 0; i < THREAD_COUNT; i++) {
			threads[i] = new Thread(new GetSessionRunnable(webSocketClientSessionPoolImpl, sessionsIds, errors));
		}

		// Parallel start of clients
		IntStream.range(0, THREAD_COUNT).parallel().forEach(i -> threads[i].start());

		// wait to stop
		for (int i = 0; i < THREAD_COUNT; i++) {
			threads[i].join();
		}

		// Assert
		if (!errors.isEmpty()) {
			errors.forEach(e -> LOGGER.error("Failed to connect", e));
			fail("Failed to connect");
		}

		assertEquals(THREAD_COUNT, sessionsIds.size());

		final Set<String> sessionIdSet = new HashSet<>(sessionsIds);

		assertEquals("Multiple sessions opened: " + sessionIdSet, 1, sessionIdSet.size());
	}

	@Test
	public void testGetSessionWithMultipleRequestsWithClosedSession() throws InterruptedException, IOException {
		final List<String> sessionsIds = new CopyOnWriteArrayList<>();
		final List<Throwable> errors = new CopyOnWriteArrayList<>();

		// Create threads
		final SessionWrapper zeroSession = webSocketClientSessionPoolImpl.getSession(DUMMY_SERVICE_INFO);
		sessionsIds.add(zeroSession.getSessionId());
		zeroSession.getSession().close(new CloseReason(CloseCodes.GOING_AWAY, "Random close"));

		final Thread[] threads = new Thread[THREAD_COUNT];

		// Create
		for (int i = 0; i < THREAD_COUNT; i++) {
			threads[i] = new Thread(new GetSessionRunnable(webSocketClientSessionPoolImpl, sessionsIds, errors));
		}

		// Parallel start of clients
		IntStream.range(0, THREAD_COUNT).parallel().forEach(i -> threads[i].start());

		// wait to stop
		for (int i = 0; i < THREAD_COUNT; i++) {
			threads[i].join();
		}

		// wait to stop
		for (int i = 0; i < THREAD_COUNT; i++) {
			threads[i].join();
		}

		// Assert
		if (!errors.isEmpty()) {
			errors.forEach(e -> LOGGER.error("Failed to connect", e));
			fail("Failed to connect");
		}

		assertEquals(THREAD_COUNT + 1, sessionsIds.size());

		final Set<String> sessionIdSet = new HashSet<>(sessionsIds);

		assertEquals("Exactly two sessions must be created, but was: " + sessionIdSet, 2, sessionIdSet.size());

		assertEquals("Closed session was not removed", 1, webSocketClientSessionPoolImpl.getOpenSessions().size());
	}

	static class GetSessionRunnable implements Runnable {
		private final WebSocketClientSessionPool webSocketClientSessionPoolImpl;
		private final List<String> sessionsIds;
		private final List<Throwable> errors;

		public GetSessionRunnable(WebSocketClientSessionPool webSocketClientSessionPoolImpl, List<String> sessionsIds,
				List<Throwable> errors) {
			this.webSocketClientSessionPoolImpl = webSocketClientSessionPoolImpl;
			this.sessionsIds = sessionsIds;
			this.errors = errors;
		}

		@Override
		public void run() {
			try {
				final SessionWrapper session = webSocketClientSessionPoolImpl.getSession(DUMMY_SERVICE_INFO);

				assertNotNull("GetSession resulted in null wrapper", session);
				assertNotNull("GetSession resulted in null session within wrapper", session.getSession());

				sessionsIds.add(session.getSessionId());
			} catch (Throwable t) {
				errors.add(t);
			}
		}
	}

	protected JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded)
			throws Exception {
		return null;
	}

}
