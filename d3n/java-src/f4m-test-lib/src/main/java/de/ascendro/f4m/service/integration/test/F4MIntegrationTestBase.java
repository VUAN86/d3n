package de.ascendro.f4m.service.integration.test;

import static de.ascendro.f4m.service.integration.RetriedAssert.assertWithWait;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfig;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.di.handler.JsonParallelServiceMessageHandlerProvider;
import de.ascendro.f4m.service.di.handler.MessageHandlerProvider;
import de.ascendro.f4m.service.exception.ExceptionType;
import de.ascendro.f4m.service.integration.F4MAssert;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.collector.ClearableErrorCollector;
import de.ascendro.f4m.service.integration.collector.ReceivedMessageCollector;
import de.ascendro.f4m.service.integration.collector.ReceivedMessageCollectorProvider;
import de.ascendro.f4m.service.integration.rule.JettyServerRule;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessageError;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.json.model.type.MessageType;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.session.SessionWrapperFactory;
import de.ascendro.f4m.service.util.JsonLoader;
import de.ascendro.f4m.service.util.JsonTestUtil;
import de.ascendro.f4m.service.util.JsonTestUtil.JsonContentVerifier;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
/**
 * Base test to be extended by all integration tests, which start a real web-server and tries to communicate with
 * service to be tested.
 */
public abstract class F4MIntegrationTestBase {
	private static final Logger LOGGER = LoggerFactory.getLogger(F4MIntegrationTestBase.class);
	
	public static final String TEST_CLIENT_SERVICE_NAME = "TestClient";
	public static final Stage DEFAULT_TEST_STAGE = Stage.PRODUCTION;

	@ClassRule
	public static final TemporaryFolder keystoreFolder = new TemporaryFolder();

	public JettyServerRule jettyServerRule = new JettyServerRule(getServiceStartup());

	@Rule //use this to add errors from other threads which would be missing otherwise 
	// - like when processing messages in MockServiceRule  
	public ClearableErrorCollector errorCollector = new ClearableErrorCollector();

	protected String serviceRegistryUris;
	protected JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	protected Injector clientInjector;
	protected ReceivedMessageCollector testClientReceivedMessageCollector;
	protected JsonLoader jsonLoader;
	protected Config config;
	private TestClientStartup clientStartup;

	@BeforeClass
	public static void setUpBeforeClass() throws IOException {
		KeyStoreTestUtil.initKeyStore(keystoreFolder);
		System.setProperty(F4MConfigImpl.SERVICE_REGISTRY_URI, "not_set");
		System.setProperty(F4MConfigImpl.SERVICE_HOST, "localhost"); //register services running as localhost to avoid registering on non-running network interface (VMware, docker etc)

		System.setProperty(F4MConfigImpl.SERVICE_RECONNECT_ON_SERVICE_REGISTRY_CLOSE, "false");
		System.setProperty(F4MConfigImpl.REQUEST_SERVICE_CONNECTION_INFO_ON_CONNECTION_CLOSE, "false");
		System.setProperty(F4MConfigImpl.EVENT_SERVICE_CLIENT_AUTO_RESUBSCRIBE_ON_CLOSE, "false");
		//postpone service discovery to 10 minutes so that not too many SubscribeRequests are made (only 1 is expected):
		System.setProperty(F4MConfigImpl.SERVICE_DISCOVERY_DELAY, Integer.toString(10 * 60 * 1000));
		System.setProperty(F4MConfigImpl.SERVICE_CONNECTION_RETRY_DELAY, "60000");
		
		System.setProperty(F4MConfigImpl.WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_INITIAL_SIZE, "1");
		System.setProperty(F4MConfigImpl.WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_MAX_MESSAGE_QUEUE_SIZE, "2");
		System.setProperty(F4MConfigImpl.WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_MAX_SIZE, "1");
		
		System.setProperty(F4MConfigImpl.MONITORING_START_DELAY, "0"); //execute monitoring immediately if asked
		System.setProperty(F4MConfigImpl.MONITORING_INTERVAL, "10000000"); //but (almost) never repeat
	}
	
	@AfterClass
	public static void afterClass() {
		System.setProperty(F4MConfigImpl.SERVICE_REGISTRY_URI, "not_set");
		System.setProperty(F4MConfigImpl.WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_INITIAL_SIZE,
				String.valueOf(F4MConfigImpl.WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_INITIAL_SIZE_DEFAULT));
		System.setProperty(F4MConfigImpl.WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_MAX_MESSAGE_QUEUE_SIZE,
				String.valueOf(
						F4MConfigImpl.WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_MAX_MESSAGE_QUEUE_SIZE_DEFAULT));
		System.setProperty(F4MConfigImpl.WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_MAX_SIZE,
				String.valueOf(F4MConfigImpl.WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_MAX_SIZE_DEFAULT));
	}

	@Before
	public void setUp() throws Exception {
		startTestClient();
		testClientReceivedMessageCollector = (ReceivedMessageCollector) clientInjector
				.getInstance(Key.get(JsonMessageHandlerProvider.class, ClientMessageHandler.class))
				.get();
		jsonLoader = new JsonLoader(this);
		config = jettyServerRule.getServerStartup()
				.getInjector()
				.getInstance(Config.class);
		config.setProperty(F4MConfigImpl.REQUESTS_CACHE_TIME_TO_LIVE, 5 * 60 * 1000); //very long timeout in tests to ease debugging
	}
	
	@After
	public void closeInjectors() {
		clientStartup.closeInjector();
	}
	
	protected void startTestClient() {
		long time = System.currentTimeMillis();
		clientStartup = new TestClientStartup(getTestInjectionModule());
		clientInjector = clientStartup.getInjector();
		jsonWebSocketClientSessionPool = clientInjector.getInstance(JsonWebSocketClientSessionPool.class);
		Config clientConfig = clientInjector.getInstance(Config.class);
		clientConfig.setProperty(F4MConfigImpl.SERVICE_NAME, TEST_CLIENT_SERVICE_NAME);
		LOGGER.info("Client started up in {} ms", System.currentTimeMillis() - time);
	}

	protected AbstractModule getTestInjectionModule() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Config.class).to(F4MConfigImpl.class).in(Singleton.class);
				bind(JsonMessageTypeMap.class).to(getTestClientInjectionClasses().getJsonMessageTypeMap());
				bind(JsonMessageSchemaMap.class).to(getTestClientInjectionClasses().getJsonMessageSchemaMap()).in(Singleton.class);
				bind(new TypeLiteral<MessageHandlerProvider<String>>(){}).annotatedWith(ClientMessageHandler.class)
					.to(TestClientJsonParallelServiceMessageHandlerProvider.class)
					.in(Singleton.class);
				bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class)
						.to(getTestClientInjectionClasses().getReceivedMessageCollectorProvider()).in(Singleton.class);
			};
		};
	}
	
	protected abstract TestClientInjectionClasses getTestClientInjectionClasses();

	@SuppressWarnings("unchecked")
	protected <T extends JsonMessageContent> T assertMessageContentType(JsonMessage<? extends JsonMessageContent> message,
			Class<T> instanceOfClass) {
		assertNotNull("Message must not be null", message);
		assertNull("Message has errors", message.getError());
		assertNotNull("Message content must not be null", message.getContent());
		assertTrue(message.getContent().getClass().getSimpleName() + " is not instance of "
				+ instanceOfClass.getSimpleName(), instanceOfClass.isInstance(message.getContent()));
		return (T) message.getContent();
	}
	
	protected void assertResponseError(String jsonMessage, ExceptionType errorType, String errorCode) throws URISyntaxException{
		testClientReceivedMessageCollector.clearReceivedMessageList();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), jsonMessage);
		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList()
				.size()));
		final JsonMessageError error = testClientReceivedMessageCollector.getMessage(0).getError();
		assertNotNull("Json message with error expected", error);
		assertEquals(errorType.name().toLowerCase(), error.getType());
		assertEquals(errorCode, error.getCode());
	}

	protected abstract ServiceStartup getServiceStartup();

	public URI getServerURI() throws URISyntaxException {
		return ((F4MConfigImpl) config).getServiceURI();
	}

	public ServiceConnectionInformation getServiceConnectionInformation() throws URISyntaxException{
		return new ServiceConnectionInformation(config.getProperty(F4MConfig.SERVICE_NAME), 
				((F4MConfigImpl)config).getServiceURI().toString(), 
				config.getPropertyAsListOfStrings(F4MConfig.SERVICE_NAMESPACES));
	}

	/**
	 * Method only for backwards compatibility, jsonLoader should be used instead.
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public String getPlainTextJsonFromResources(String path) throws IOException {
		return jsonLoader.getPlainTextJsonFromResources(path);
	}
	
	public String getPlainTextJsonFromResources(String path, ClientInfo clientInfo) throws IOException {
		 return jsonLoader.getPlainTextJsonFromResources(path, clientInfo);
	}
	
	public String getPlainTextJsonFromResources(String path, String userId) throws IOException {
		return jsonLoader.getPlainTextJsonFromResources(path, userId);
	}
	
	public void verifyResponseOnRequest(String requestPath, String expectedResponsePath) throws Exception {
		verifyResponseOnRequest(requestPath, expectedResponsePath, true);
	}
	
	public void verifyResponseOnRequest(String requestPath, String expectedResponsePath, boolean expectContent) throws Exception {
		verifyResponseOnRequest(requestPath, expectedResponsePath, expectContent, 
				(e, a, j) -> JsonTestUtil.assertJsonContentEqualIgnoringSeq(e, a));
	}
	
	public void verifyResponseOnRequest(String requestPath, ClientInfo clientInfo, String expectedResponsePath, boolean expectContent) throws Exception {
		verifyResponseOnRequest(requestPath, clientInfo, expectedResponsePath, expectContent, 
				(e, a, j) -> JsonTestUtil.assertJsonContentEqualIgnoringSeq(e, a));
	}
	
	public void verifyResponseOnRequest(String requestPath, String expectedResponsePath, boolean expectContent,
			JsonContentVerifier verifier) throws Exception {
		String request = jsonLoader.getPlainTextJsonFromResources(requestPath);

		verifyResponseOnRequestJson(request, expectedResponsePath, expectContent, verifier);
	}
	
	public void verifyResponseOnRequest(String requestPath, ClientInfo clientInfo, String expectedResponsePath, boolean expectContent,
			JsonContentVerifier verifier) throws Exception {
		String request = jsonLoader.getPlainTextJsonFromResources(requestPath, clientInfo);

		verifyResponseOnRequestJson(request, expectedResponsePath, expectContent, verifier);
	}

	private void verifyResponseOnRequestJson(String request, String expectedResponsePath, boolean expectContent,
			JsonContentVerifier verifier) throws URISyntaxException, IOException {
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), request);
		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList()
				.size()));
		JsonMessage<? extends JsonMessageContent> getResponse =
				testClientReceivedMessageCollector.getReceivedMessageList()
						.get(0);
		testClientReceivedMessageCollector.clearReceivedMessageList();
		boolean expectError = !expectContent;
		boolean hasError = getResponse.getError() != null;
		assertTrue("Unexpected error returned " + getResponse.getError(), hasError == expectError);

		boolean hasContent = getResponse.getContent() != null;
		assertTrue("Empty message content not expected " + getResponse.getContent(), hasContent == expectContent);

		final String responseJson = clientInjector.getInstance(JsonMessageUtil.class)
				.toJson(getResponse);
		final String expectedGetResponseJson = jsonLoader.getPlainTextJsonFromResources(expectedResponsePath);
		verifier.assertExpected(expectedGetResponseJson, responseJson, getResponse);
	}

	protected void assertServiceStartup(String... serviceNames) {
		ServiceRegistryClient src = jettyServerRule.getServerStartup().getInjector().getInstance(ServiceRegistryClient.class);
		for (String serviceName : serviceNames) {
			RetriedAssert.assertWithWait(() -> assertNotNull("Service not resolved: " + serviceName, src.getServiceConnInfoFromStore(serviceName)));
		}
	}

	protected void assertReceivedMessagesWithWait(MessageType...messageTypes){
		assertReceivedMessagesWithWait(ReceivedMessagesAssertMode.ALL, messageTypes);
	}
	
	protected void assertReceivedMessagesWithWait(ReceivedMessagesAssertMode messagesAssertMode, MessageType...messageTypes){
		if(messagesAssertMode == ReceivedMessagesAssertMode.ALL){			
			Arrays.stream(messageTypes)
				.forEach(mt -> assertWithWait(() -> assertNotNull("Didn't receive: " + mt, testClientReceivedMessageCollector.getMessageByType(mt))));
		}else if(messagesAssertMode == ReceivedMessagesAssertMode.ONE_OF){
			assertWithWait(() -> assertNotEquals(0L, Arrays.stream(messageTypes)
				.map(mt -> testClientReceivedMessageCollector.getMessageByType(mt))
				.filter(mt -> mt != null)
				.count()));
		}
		
	}
	
	protected void assertReceivedMessagesWithWait(int expectedCount, MessageType messageType){
		assertWithWait(() -> F4MAssert.assertSize(expectedCount,
				testClientReceivedMessageCollector.getMessagesByType(messageType)));
	}
	
	protected void assertReceivedMessagesWithWaitingLonger(int expectedCount, MessageType messageType){
		assertWithWait(() -> F4MAssert.assertSize(expectedCount,
				testClientReceivedMessageCollector.getMessagesByType(messageType)),
				2 * RetriedAssert.DEFAULT_TIMEOUT_MS);
	}
	
	protected String getTestClientSessionId() throws URISyntaxException {
		return jsonWebSocketClientSessionPool.getSession(getServiceConnectionInformation()).getSessionId();
	}
	
	protected String getServerSessionId() throws URISyntaxException{
		return changeSessionIdDirection(getTestClientSessionId());
	}
	
	protected String changeSessionIdDirection(String sessionId){
		final String connector = "->";
		final String[] sessionIps = sessionId.split(connector);
		return sessionIps[1] + connector + sessionIps[0];
	}
	
	static class TestClientJsonParallelServiceMessageHandlerProvider extends JsonParallelServiceMessageHandlerProvider {

		@Inject
		public TestClientJsonParallelServiceMessageHandlerProvider(@ClientMessageHandler JsonMessageHandlerProvider jsonMessageHandlerProvider,
				Config config, SessionWrapperFactory sessionWrapperFactory, LoggingUtil loggingUtil) {
			super(jsonMessageHandlerProvider, config, sessionWrapperFactory, loggingUtil);
		}
	}
	
	public static class TestClientInjectionClasses {
		private final Class<? extends JsonMessageTypeMap> jsonMessageTypeMap;
		private final Class<? extends JsonMessageSchemaMap> jsonMessageSchemaMap;
		private final Class<? extends ReceivedMessageCollectorProvider> receivedMessageCollectorProvider;
		
		public TestClientInjectionClasses(Class<? extends JsonMessageTypeMap> jsonMessageTypeMap,
				Class<? extends JsonMessageSchemaMap> jsonMessageSchemaMap) {
			this.jsonMessageTypeMap = jsonMessageTypeMap;
			this.jsonMessageSchemaMap = jsonMessageSchemaMap;
			this.receivedMessageCollectorProvider = ReceivedMessageCollectorProvider.class;
		}
		
		public TestClientInjectionClasses(Class<? extends JsonMessageTypeMap> jsonMessageTypeMap,
				Class<? extends JsonMessageSchemaMap> jsonMessageSchemaMap,
				Class<? extends ReceivedMessageCollectorProvider> receivedMessageCollectorProvider) {
			this.jsonMessageTypeMap = jsonMessageTypeMap;
			this.jsonMessageSchemaMap = jsonMessageSchemaMap;
			this.receivedMessageCollectorProvider = receivedMessageCollectorProvider;
		}

		public Class<? extends JsonMessageTypeMap> getJsonMessageTypeMap() {
			return jsonMessageTypeMap;
		}
		public Class<? extends JsonMessageSchemaMap> getJsonMessageSchemaMap() {
			return jsonMessageSchemaMap;
		}

		public Class<? extends ReceivedMessageCollectorProvider> getReceivedMessageCollectorProvider() {
			return receivedMessageCollectorProvider;
		}
	}
	
	
	protected enum ReceivedMessagesAssertMode{
		ALL,
		ONE_OF		
	}
}
