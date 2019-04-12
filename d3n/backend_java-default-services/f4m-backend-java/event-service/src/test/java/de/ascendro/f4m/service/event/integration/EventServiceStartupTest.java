package de.ascendro.f4m.service.event.integration;

import static de.ascendro.f4m.service.integration.RetriedAssert.DEFAULT_TIMEOUT_MS;
import static de.ascendro.f4m.service.integration.RetriedAssert.assertWithWait;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.inject.Singleton;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.event.EventMessageTypeMapper;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.EventServiceStartup;
import de.ascendro.f4m.service.event.model.NotifySubscriberMessageContent;
import de.ascendro.f4m.service.event.model.info.InfoResponse;
import de.ascendro.f4m.service.event.model.publish.PublishMessageContent;
import de.ascendro.f4m.service.event.model.resubscribe.ResubscribeResponse;
import de.ascendro.f4m.service.event.model.subscribe.SubscribeResponse;
import de.ascendro.f4m.service.event.model.unsubscribe.UnsubscribeRequestResponse;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.RetriedAssert.Assertion;
import de.ascendro.f4m.service.integration.collector.ReceivedMessageCollectorProvider;
import de.ascendro.f4m.service.integration.collector.ReceivedMessageSessionCollector;
import de.ascendro.f4m.service.integration.collector.ReceivedMessageSessionCollectorProvider;
import de.ascendro.f4m.service.integration.rule.JettyServerRule;
import de.ascendro.f4m.service.integration.test.F4MIntegrationTestBase;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.registry.EventServiceStore;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypeMapper;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.registry.store.ServiceStore;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class EventServiceStartupTest extends F4MIntegrationTestBase {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventServiceStartupTest.class);
	private static final String RUN_LONG_TESTS_PARAM_NAME = "runLongTests";
	private static final Random SECURE_RANDOM = new Random();
	private static final long RESUBSCRIPTION1 = SECURE_RANDOM.nextInt(Integer.MAX_VALUE),
			RESUBSCRIPTION2 = SECURE_RANDOM.nextInt(Integer.MAX_VALUE);

	// Fake Service Registry
	private static final int SERVICE_REGISTRY_JETTY_PORT = 8432;
	public JettyServerRule serviceRegistryJetty =
			new JettyServerRule(getServiceRegistryStartup(), SERVICE_REGISTRY_JETTY_PORT);
	private final URI serviceRegistryURI = URI.create("wss://localhost:" + SERVICE_REGISTRY_JETTY_PORT);
	private ReceivedMessageSessionCollector serviceRegistryMessageSessionCollector;

	// Event Service - instance #1
	public ActiveMqServerRule eventService1ActiveMqRule =
			new ActiveMqServerRule((EventServiceStartup) jettyServerRule.getServerStartup(), 9555, "localhost");
	public DiscoveryRule eventService1DiscoveryRule =
			new DiscoveryRule((EventServiceStartup) jettyServerRule.getServerStartup(), serviceRegistryURI);
	public RuleChain eventService1Rule = RuleChain.outerRule(jettyServerRule)
			.around(eventService1DiscoveryRule).around(eventService1ActiveMqRule);
	
	// Event Service - instance #2

	public JettyServerRule eventService2JettyServerRule = new JettyServerRule(getServiceStartup2(), 8444);
	private final URI eventService2JettyServerURI =
			URI.create("wss://localhost:" + eventService2JettyServerRule.getJettySslPort());

	public DiscoveryRule eventService2DiscoveryRule = new DiscoveryRule(
			(EventServiceStartup) eventService2JettyServerRule.getServerStartup(), serviceRegistryURI);

	public ActiveMqServerRule eventService2ActiveMqRule = new ActiveMqServerRule(
			(EventServiceStartup) eventService2JettyServerRule.getServerStartup(), 9556, "localhost");
	public RuleChain eventService2Rule = RuleChain.outerRule(eventService2JettyServerRule)
			.around(eventService2DiscoveryRule).around(eventService2ActiveMqRule);

	@Rule
	public RuleChain wholeServiceSetupRule = RuleChain.outerRule(serviceRegistryJetty)
			.around(eventService1Rule).around(eventService2Rule);
	
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		serviceRegistryMessageSessionCollector =
				(ReceivedMessageSessionCollector) serviceRegistryJetty.getServerStartup()
						.getInjector()
						.getInstance(Key.get(JsonMessageHandlerProvider.class, ServerMessageHandler.class))
						.get();
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new EventServiceStartup(DEFAULT_TEST_STAGE);
	}

	protected ServiceStartup getServiceStartup2() {
		return new EventServiceStartup(DEFAULT_TEST_STAGE);
	}

	protected ServiceStartup getServiceRegistryStartup() {
		return new ServiceStartup(DEFAULT_TEST_STAGE) {
			@Override
			public Injector createInjector(Stage stage) {
				return Guice.createInjector(stage, Modules.override(super.getModules())
						.with(getModules()));
			}

			@Override
			protected Iterable<? extends Module> getModules() {
				return Arrays.asList(getServiceRegitryInjectionModule());
			}

			@Override
			protected String getServiceName() {
				return "SERVICE-REGISTRY-MOCK";
			}
		};
	}

	protected AbstractModule getServiceRegitryInjectionModule() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(JsonMessageTypeMap.class).to(ServiceRegistryMessageTypeMapper.class);

				bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class)
						.to(ReceivedMessageCollectorProvider.class)
						.in(Singleton.class);
				bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class)
						.to(ReceivedMessageSessionCollectorProvider.class)
						.in(Singleton.class);
			};
		};
	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(EventMessageTypeMapper.class, DefaultMessageSchemaMapper.class);
	}

	@Test
	public void testSubscribeCycle() throws Exception {
		LOGGER.debug("Sending subscribe JSON message to {}", getServerURI());
		// message validated in http://jsonlint.com/
		final String subscribeJson = getPlainTextJsonFromResources("subscribe.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), subscribeJson);

		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList()
				.size()));

		final JsonMessage<?> subscribeResponseMessage = testClientReceivedMessageCollector.getReceivedMessageList()
				.get(0);
		assertMessageContentType(subscribeResponseMessage, SubscribeResponse.class);

		// Test content
		final JsonMessageContent subscriptionResponse = subscribeResponseMessage.getContent();
		assertNotNull(((SubscribeResponse) subscriptionResponse).getSubscription());
		assertTrue(((SubscribeResponse) subscriptionResponse).getSubscription() > 0);

		// Test seq <--> act
		assertNotNull(subscribeResponseMessage.getAck());
		assertEquals(1, subscribeResponseMessage.getAck().length);
		assertEquals(100, subscribeResponseMessage.getAck()[0]);

		assertNotNull(subscribeResponseMessage.getContent());
	}

	@Test
	public void testResubscribeWithoutPreviousSubscribsionCycle() throws Exception {
		LOGGER.debug("Sending resubscribe JSON message to {}", getServerURI());

		jsonWebSocketClientSessionPool
				.sendAsyncText(getServiceConnectionInformation(), getResubscriptionJson(RESUBSCRIPTION1, RESUBSCRIPTION2));

		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList()
				.size()));

		final JsonMessage<?> subscribeResponseMessage = testClientReceivedMessageCollector.getReceivedMessageList()
				.get(0);
		assertMessageContentType(subscribeResponseMessage, ResubscribeResponse.class);

		// Test content
		final ResubscribeResponse resubscribeResponse = (ResubscribeResponse) subscribeResponseMessage.getContent();
		assertNotNull(resubscribeResponse.getSubscriptions());

		assertEquals(2, resubscribeResponse.getSubscriptions().length);
		assertEquals(RESUBSCRIPTION1, resubscribeResponse.getSubscriptions()[0].getSubscription());
		assertEquals(RESUBSCRIPTION2, resubscribeResponse.getSubscriptions()[1].getSubscription());

		// Test seq <--> act
		assertNotNull(subscribeResponseMessage.getAck());
		assertEquals(1, subscribeResponseMessage.getAck().length);
		assertEquals(100, subscribeResponseMessage.getAck()[0]);

		assertNotNull(subscribeResponseMessage.getContent());
	}

	@Test
	public void testPublishNotifyCycle() throws Exception {
		final long subscription = subscribe().getContent()
				.getSubscription();

		LOGGER.debug("Sending publish JSON message to {}", getServerURI());

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getPublishJson("Game/ABC"));

		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList()
				.size()));

		final JsonMessage<?> notificationMessage = testClientReceivedMessageCollector.getReceivedMessageList()
				.get(0);

		assertMessageContentType(notificationMessage, NotifySubscriberMessageContent.class);
		final NotifySubscriberMessageContent notificationContent =
				(NotifySubscriberMessageContent) notificationMessage.getContent();

		assertEquals("Game/ABC", notificationContent.getTopic());
		assertEquals(subscription, notificationContent.getSubscription());

		final JsonObject notificationContentJsonObject = notificationContent.getNotificationContent()
				.getAsJsonObject();
		assertEquals(100, notificationContentJsonObject.get("score")
				.getAsInt());
		assertEquals(898L, notificationContentJsonObject.get("status")
				.getAsLong());

		testClientReceivedMessageCollector.clearReceivedMessageList();
		unsubscribe(subscription);

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getPublishJson("Game/ABC"));

		RetriedAssert.assertWithWait(() -> assertEquals(0, testClientReceivedMessageCollector.getReceivedMessageList()
				.size()));
	}

	@Test
	public void testUnsubscribeCycle() throws Exception {
		LOGGER.debug("Sending unsubscribe JSON message to {}", getServerURI());
		// message validated in http://jsonlint.com/
		final long subscriptionId = 9999;

		String unsubscribeJson = getPlainTextJsonFromResources("unsubscribe.json");
		unsubscribeJson = unsubscribeJson.replace("{subscription}", Long.toString(subscriptionId));

		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), unsubscribeJson);

		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList()
				.size()));

		final JsonMessage<?> unsubscribeResponseMessage = testClientReceivedMessageCollector.getReceivedMessageList()
				.get(0);

		assertMessageContentType(unsubscribeResponseMessage, UnsubscribeRequestResponse.class);
		assertEquals(subscriptionId, ((UnsubscribeRequestResponse) unsubscribeResponseMessage.getContent())
				.getSubscription());

		// Test seq <--> act
		assertNotNull(unsubscribeResponseMessage.getAck());
		assertEquals(1, unsubscribeResponseMessage.getAck().length);
		assertEquals(100, unsubscribeResponseMessage.getAck()[0]);

		assertNotNull(unsubscribeResponseMessage.getContent());
	}

	@Test
	public void testResubscribeSessionDestroyCycle() throws Exception {
		final long subscription = subscribe().getContent()
				.getSubscription();

		final SessionWrapper jettySession = jsonWebSocketClientSessionPool.getOpenSessions()
				.get(0);
		jettySession.getSession()
				.close(new CloseReason(CloseCodes.SERVICE_RESTART, "Testing subscriber service restart"));

		LOGGER.debug("Sending resubscribe JSON message to {}", getServerURI());
		final String resubscribeJson = getResubscriptionJson(subscription, RESUBSCRIPTION2);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), resubscribeJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList()
				.size()));

		final JsonMessage<?> resubscribeResponseMessage = testClientReceivedMessageCollector.getReceivedMessageList()
				.get(0);

		// Test content
		final ResubscribeResponse resubscriptionResponse =
				(ResubscribeResponse) resubscribeResponseMessage.getContent();
		assertEquals(2, resubscriptionResponse.getSubscriptions().length);

		assertEquals(subscription, resubscriptionResponse.getSubscriptions()[0].getSubscription());
		assertEquals(subscription, resubscriptionResponse.getSubscriptions()[0].getResubscription());

		assertEquals(RESUBSCRIPTION2, resubscriptionResponse.getSubscriptions()[1].getSubscription());
		assertNotEquals(subscription, resubscriptionResponse.getSubscriptions()[1].getResubscription());
		assertTrue(resubscriptionResponse.getSubscriptions()[1].getResubscription() > 0);

		testClientReceivedMessageCollector.clearReceivedMessageList();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getPublishJson("Game/ABC"));
		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList()
				.size()));

		final JsonMessage<?> notifySubscriberAbcMessage = testClientReceivedMessageCollector.getReceivedMessageList()
				.get(0);
		assertMessageContentType(notifySubscriberAbcMessage, NotifySubscriberMessageContent.class);
		assertEquals(((NotifySubscriberMessageContent) notifySubscriberAbcMessage.getContent()).getTopic(), "Game/ABC");

		testClientReceivedMessageCollector.clearReceivedMessageList();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getPublishJson("Game/DEC"));
		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList()
				.size()));

		final JsonMessage<?> notifySubscriberDecMessage = testClientReceivedMessageCollector.getReceivedMessageList()
				.get(0);

		assertMessageContentType(notifySubscriberDecMessage, NotifySubscriberMessageContent.class);
		assertEquals(((NotifySubscriberMessageContent) notifySubscriberDecMessage.getContent()).getTopic(), "Game/DEC");
	}

	private void joinEventService1WithEventService2() throws IOException, InterruptedException, URISyntaxException {
		RetriedAssert.assertWithWait(() -> assertEquals(2, serviceRegistryMessageSessionCollector
				.getReceivedMessageSessionSet()
				.size()));

		// Event Service 1
		final Injector eventService1Injector = jettyServerRule.getServerStartup()
				.getInjector();
		final SessionWrapper serviceRegistryEventService1Session =
				getServiceRegistryEventServiceSession(eventService1Injector);

		// Event Service 2
		final Injector eventService2Injector = eventService2JettyServerRule.getServerStartup()
				.getInjector();
		final SessionWrapper serviceRegistryEventService2Session =
				getServiceRegistryEventServiceSession(eventService2Injector);

		//Send Event Service 2 info to Event Service 1 as ServiceRegistry mock
		final String registerEventService2ForService1 =
				getEventServiceListReponseJson(EventMessageTypes.SERVICE_NAME, eventService2JettyServerURI.toString());
		serviceRegistryEventService1Session.sendAsynText(registerEventService2ForService1);
		checkIfMQConnectionsHaveBeenEstablished(eventService1Injector);

		//Send Event Service 1 info to Event Service 2 as ServiceRegistry mock
		final String registerEventService1ForService2 =
				getEventServiceListReponseJson(EventMessageTypes.SERVICE_NAME, getServerURI().toString());
		serviceRegistryEventService2Session.sendAsynText(registerEventService1ForService2);
		checkIfMQConnectionsHaveBeenEstablished(eventService2Injector);
	}

	private void checkIfMQConnectionsHaveBeenEstablished(final Injector eventService1Injector) {
		EventServiceStore eventServiceStore = (EventServiceStore) eventService1Injector.getInstance(ServiceStore.class);
		RetriedAssert
				.assertWithWait(() -> assertTrue("Event service MQ discovery has not been successful", eventServiceStore
						.allKnownEventServicesAreConnected()));
	}

	private SessionWrapper getServiceRegistryEventServiceSession(final Injector eventService1Injector)
			throws URISyntaxException {
		final JsonWebSocketClientSessionPool eventService1WebSocketClient =
				eventService1Injector.getInstance(JsonWebSocketClientSessionPool.class);
		assertEquals(1, eventService1WebSocketClient.getOpenSessions()
				.size());
		final SessionWrapper eventService1ClientSession = eventService1WebSocketClient.getOpenSessions()
				.get(0);

		final SessionWrapper serviceRegistryEventService1Session =
				getServiceRegistryServerSession(eventService1WebSocketClient);
		assertNotNull(serviceRegistryEventService1Session);

		LOGGER.info("Found Service Registry session[{}] for Event Service at Jetty [{}] via Client Session[{}]", serviceRegistryEventService1Session
				.getSessionId(), getServerURI(), eventService1ClientSession.getSessionId());
		return serviceRegistryEventService1Session;
	}

	@Test
	public void testResubscribeSessionDestroyWithTimeoutResubscribeCycle() throws Exception {
		joinEventService1WithEventService2();

		final long subscription = subscribe().getContent()
				.getSubscription();

		final SessionWrapper jettySession = jsonWebSocketClientSessionPool.getOpenSessions()
				.get(0);
		jettySession.getSession()
				.close(new CloseReason(CloseCodes.SERVICE_RESTART, "Testing subscriber service restart"));

		LOGGER.debug("Publish few messages without Jetty session");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getPublishJson("Game/ABC"));
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getPublishJson("Game.DEC"));
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getPublishJson("Game/ABC"));

		RetriedAssert.assertWithWait(() -> assertEquals(0, testClientReceivedMessageCollector.getReceivedMessageList()
				.size()));

		LOGGER.debug("Resubscribe for topic[Game/ABC] by subcription[{}] and topic[Game.DEC] without previous subscriptions", subscription);
		final String resubscribeJson = getResubscriptionJson(subscription, RESUBSCRIPTION2);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), resubscribeJson);

		RetriedAssert.assertWithWait(() -> assertEquals(3, testClientReceivedMessageCollector.getReceivedMessageList()
				.size()));

		for (JsonMessage<?> receivedMessage : testClientReceivedMessageCollector.getReceivedMessageList()) {
			assertNotNull(receivedMessage);
			assertNotNull(receivedMessage.getContent());

			if (receivedMessage.getContent() instanceof NotifySubscriberMessageContent) {
				assertEquals(((NotifySubscriberMessageContent) receivedMessage.getContent()).getTopic(), "Game/ABC");
			} else if (receivedMessage.getContent() instanceof ResubscribeResponse) {
				assertEquals(2, ((ResubscribeResponse) receivedMessage.getContent()).getSubscriptions().length);
			} else {
				fail("Unexpected message content type: " + receivedMessage.getContent()
						.getClass()
						.getSimpleName());
			}
		}
	}

	@Test
	public void testSimpleInfoCycle() throws Exception {
		final String infoRequestJson = getPlainTextJsonFromResources("info.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), infoRequestJson);

		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList()
				.size()));

		final JsonMessage<? extends JsonMessageContent> infoResponseMessage =
				testClientReceivedMessageCollector.getReceivedMessageList()
						.get(0);
		assertMessageContentType(infoResponseMessage, InfoResponse.class);

		final InfoResponse infoResponse = (InfoResponse) infoResponseMessage.getContent();
		assertNull(infoResponseMessage.getError());
		assertEquals(eventService1ActiveMqRule.getActiveMqPort(), infoResponse.getJmsPort());
	}

	private SessionWrapper getServiceRegistryServerSession(JsonWebSocketClientSessionPool serviceWebSocketClient) {
		final List<SessionWrapper> serviceClientOpenSessions = serviceWebSocketClient.getOpenSessions();
		assertNotNull(serviceClientOpenSessions);
		assertEquals(1, serviceClientOpenSessions.size());

		final String eventServiceClientSessionId = serviceClientOpenSessions.get(0)
				.getSessionId();
		assertNotNull(eventServiceClientSessionId);

		final Iterator<SessionWrapper> messageSessionIterator =
				serviceRegistryMessageSessionCollector.getReceivedMessageSessionSet()
						.iterator();
		SessionWrapper serviceRegistrySession = null;
		while (messageSessionIterator.hasNext()) {
			final SessionWrapper sessionWrapper = messageSessionIterator.next();
			assertNotNull(sessionWrapper);
			assertNotNull(sessionWrapper.getSession());
			assertTrue(sessionWrapper.getSession()
					.isOpen());

			final String sessionid = sessionWrapper.getSession()
					.getId();
			assertNotNull(sessionid);

			if (isEqualSessions(sessionid, eventServiceClientSessionId)) {
				serviceRegistrySession = sessionWrapper;
				break;
			}
		}

		return serviceRegistrySession;
	}

	//TODO: create test, which verifies, that if two event service instances are started sequentially, published messages will be delivered in both ways!

	@Test
	public void testFullDiscoveryCycle() throws Exception {
		// Subscribe at Event Service 1 for topic "Game/ABC"
		final JsonMessage<SubscribeResponse> subscribeResponse = subscribe();
		assertNotNull(subscribeResponse);
		assertNotNull(subscribeResponse.getContent());
		assertTrue(subscribeResponse.getContent()
				.getSubscription() > 0);

		joinEventService1WithEventService2();

		// Publish event via Event Service 2 for topic "Game/ABC"
		String publishJson = getPublishJson("Game/ABC");
		LOGGER.debug("Sending to {} JSON: {}", eventService2JettyServerURI, publishJson);
		final ServiceConnectionInformation serviceConnectionInformation =
				new ServiceConnectionInformation(EventMessageTypes.SERVICE_NAME, eventService2JettyServerURI,
						EventMessageTypes.NAMESPACE);
		jsonWebSocketClientSessionPool.sendAsyncText(serviceConnectionInformation, publishJson);

		//Expect to receive notification form Event Service 1 subscription which is sent via Event Service ActiveMQ Broker Network
		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<?> notificationMessage = testClientReceivedMessageCollector.getReceivedMessageList()
				.get(0);
		assertMessageContentType(notificationMessage, NotifySubscriberMessageContent.class);
	}

	@Test
	public void testWildcards() throws Exception {
		final String subscribeJson = getPlainTextJsonFromResources("subscribeWithWildcard.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), subscribeJson);

		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList()
				.size()));

		final JsonMessage<? extends JsonMessageContent> subscribeResponseMessage =
				testClientReceivedMessageCollector.getReceivedMessageList()
						.get(0);
		assertMessageContentType(subscribeResponseMessage, SubscribeResponse.class);
		final SubscribeResponse subscribeResponse = (SubscribeResponse) subscribeResponseMessage.getContent();
		final long subscription = subscribeResponse.getSubscription();

		testClientReceivedMessageCollector.clearReceivedMessageList();
		jsonWebSocketClientSessionPool
				.sendAsyncText(getServiceConnectionInformation(), getPublishJson("Game/ABC/SUB-1"));

		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList()
				.size()));

		final JsonMessage<?> notificationMessage = testClientReceivedMessageCollector.getReceivedMessageList()
				.get(0);

		assertMessageContentType(notificationMessage, NotifySubscriberMessageContent.class);
		final NotifySubscriberMessageContent notificationContent =
				(NotifySubscriberMessageContent) notificationMessage.getContent();

		assertEquals("Game/ABC/*", notificationContent.getTopic());
		assertEquals(subscription, notificationContent.getSubscription());

		final JsonObject notificationContentJsonObject = notificationContent.getNotificationContent()
				.getAsJsonObject();
		assertEquals(100, notificationContentJsonObject.get("score")
				.getAsInt());
		assertEquals(898L, notificationContentJsonObject.get("status")
				.getAsLong());
	}

	@Test
	public void testPublishNotifyWithDelayedSending() throws Exception {
		assumeLongTestsShouldRun();
		final long subscription = subscribe().getContent().getSubscription();

		PublishMessageContent content = new PublishMessageContent("Game/ABC");
		int delaySeconds = 10;
		content.setPublishDate(DateTimeUtil.getCurrentDateTime().plusSeconds(delaySeconds));
		LOGGER.debug("now {} - delay {}", DateTimeUtil.getCurrentDateTime(), content.getPublishDate());
		jsonWebSocketClientSessionPool.sendAsyncMessage(getServiceConnectionInformation(),
				new JsonMessage<JsonMessageContent>(EventMessageTypes.PUBLISH, content));

		Assertion messageReceivedCheck = () -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size());
		boolean immediateReceptionSuccess;
		try {
			assertWithWait(messageReceivedCheck, DEFAULT_TIMEOUT_MS);
			immediateReceptionSuccess = true;
		} catch (AssertionError e) {
			immediateReceptionSuccess = false;
		}
		assertFalse("Published message should have been received only after " + delaySeconds + " seconds", immediateReceptionSuccess);
		assertWithWait(messageReceivedCheck, DEFAULT_TIMEOUT_MS + delaySeconds * 1000);
		JsonMessage<NotifySubscriberMessageContent> notificationMessage = testClientReceivedMessageCollector.getMessageByType(EventMessageTypes.NOTIFY_SUBSCRIBER);
		assertEquals("Game/ABC", notificationMessage.getContent().getTopic());
		assertEquals(subscription, notificationMessage.getContent().getSubscription());

		unsubscribe(subscription);
	}

	private void assumeLongTestsShouldRun() {
		Assume.assumeTrue("Not running unstable tests, enable them using -D" + RUN_LONG_TESTS_PARAM_NAME + "=true",
				Boolean.valueOf(System.getProperty(RUN_LONG_TESTS_PARAM_NAME, Boolean.FALSE.toString())));
	}

	private boolean isEqualSessions(String serverSessionId, String clientSessionId) {
		final String[] serverSessionIdUris = serverSessionId.split("->");
		final String[] clientSessionIdUris = clientSessionId.split("->");
		return serverSessionIdUris[0].trim()
				.equals(clientSessionIdUris[1].trim())
				&& serverSessionIdUris[1].trim()
						.equals(clientSessionIdUris[0].trim());
	}

	private String getResubscriptionJson(long resubscription1, long resubscription2) throws IOException {
		String resubscribeJson = getPlainTextJsonFromResources("resubscribe.json");

		resubscribeJson = resubscribeJson.replace("<<resubscribe-1>>", Long.toString(resubscription1));
		resubscribeJson = resubscribeJson.replace("<<resubscribe-2>>", Long.toString(resubscription2));

		return resubscribeJson;
	}

	private String getPublishJson(String topic) throws IOException {
		String publishJson = getPlainTextJsonFromResources("publish.json");

		publishJson = publishJson.replace("{topic}", topic);

		return publishJson;
	}

	private String getEventServiceListReponseJson(String serviceName, String uri) throws IOException {
		String eventServiceListResponseJson = getPlainTextJsonFromResources("listEventServiceResponse.json");

		eventServiceListResponseJson = eventServiceListResponseJson.replace("{uri}", uri);

		return eventServiceListResponseJson;
	}

	private JsonMessage<SubscribeResponse> subscribe() throws Exception {
		testClientReceivedMessageCollector.clearReceivedMessageList();

		LOGGER.debug("Sending subscribe JSON message to {} ", getServerURI());

		final String subscribeJson = getPlainTextJsonFromResources("subscribe.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), subscribeJson);

		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList()
				.size()));

		@SuppressWarnings("unchecked")
		final JsonMessage<SubscribeResponse> subscribeResponseMessage =
				(JsonMessage<SubscribeResponse>) testClientReceivedMessageCollector.getReceivedMessageList()
						.get(0);
		testClientReceivedMessageCollector.clearReceivedMessageList();

		return subscribeResponseMessage;
	}

	private JsonMessage<UnsubscribeRequestResponse> unsubscribe(long subscription) throws Exception {
		testClientReceivedMessageCollector.clearReceivedMessageList();

		LOGGER.debug("Sending subscribe JSON message to {} for PUBLISH test", getServerURI());

		String unsubscribeJson = getPlainTextJsonFromResources("unsubscribe.json");
		unsubscribeJson = unsubscribeJson.replace("{subscription}", Long.toString(subscription));
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), unsubscribeJson);

		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList()
				.size()));

		@SuppressWarnings("unchecked")
		final JsonMessage<UnsubscribeRequestResponse> unsubscribeResponseMessage =
				(JsonMessage<UnsubscribeRequestResponse>) testClientReceivedMessageCollector.getReceivedMessageList()
						.get(0);
		testClientReceivedMessageCollector.getReceivedMessageList()
				.remove(testClientReceivedMessageCollector.getReceivedMessageList()
						.size() - 1);

		return unsubscribeResponseMessage;
	}
}
