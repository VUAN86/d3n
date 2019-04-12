package de.ascendro.f4m.service.registry;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;

import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Key;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.config.F4MConfig;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.publish.PublishMessageContent;
import de.ascendro.f4m.service.exception.ExceptionCodes;
import de.ascendro.f4m.service.integration.F4MAssert;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.rule.JettyServerRule;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.registry.model.ServiceRegistryGetResponse;
import de.ascendro.f4m.service.registry.model.monitor.InfrastructureStatisticsResponse;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringServiceConnectionInfo;
import de.ascendro.f4m.service.registry.model.monitor.ServiceStatistics;
import de.ascendro.f4m.service.registry.store.ServiceRegistry;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.JsonTestUtil;

public class ServiceRegistryStartupTest extends ServiceRegistryStartupTestBase {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistryStartupTest.class);

	//Fake Event Service
	protected static final int EVENT_REGISTRY_JETTY_PORT = 8432;
	@Rule
	public JettyServerRule eventServiceJetty = new JettyServerRule(getEventServiceStartup(), EVENT_REGISTRY_JETTY_PORT);
	protected final URI eventServiceURI = URI.create("wss://localhost:" + EVENT_REGISTRY_JETTY_PORT);
    protected ReceivedMessageCollectorWithHeartbeat eventMessageCollector;
    
    @BeforeClass
    public static void setUpServiceRegistryWorkaround() {
    	System.setProperty(F4MConfigImpl.SERVICE_REGISTRY_URI, "wss://localhost:8443");
    	//will be cleaned up in F4MIntegrationTestBase.afterClass()
    }
    
	@Override
	public void setUp() throws Exception {
		super.setUp();

		MockitoAnnotations.initMocks(this);
		eventMessageCollector = (ReceivedMessageCollectorWithHeartbeat) eventServiceJetty.getServerStartup()
				.getInjector().getInstance(Key.get(JsonMessageHandlerProvider.class, ServerMessageHandler.class)).get();
		
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(2, eventMessageCollector.getReceivedMessageList()));
		JsonMessage<JsonMessageContent> registerResponse = eventMessageCollector.getMessageByType(ServiceRegistryMessageTypes.REGISTER_RESPONSE);
		assertNotNull(registerResponse);
		JsonMessage<PublishMessageContent> publishMsg = eventMessageCollector.getMessageByType(EventMessageTypes.PUBLISH);
		assertEquals("serviceRegistry/register/event", publishMsg.getContent().getTopic());
		
		eventMessageCollector.clearReceivedMessageList(); //wait for registration to happen and then clear
	}

	private void assertUnregisterEventWasPublished(String serviceName) {
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, eventMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> unregisterPublishMessage = eventMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(unregisterPublishMessage, PublishMessageContent.class);
		assertEquals("serviceRegistry/unregister/" + serviceName,
				((PublishMessageContent) unregisterPublishMessage.getContent()).getTopic());
	}

	private void assertRegisterEventWasPublished() {
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, eventMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> unregisterPublishMessage = eventMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(unregisterPublishMessage, PublishMessageContent.class);
		assertEquals("serviceRegistry/register/TestService",
				((PublishMessageContent) unregisterPublishMessage.getContent()).getTopic());
	}

	@Test
	public void testUnregisterOnDisconnectCycle() throws Exception {
		eventMessageCollector.clearReceivedMessageList();

		//Register
		String registerRequestJson = getPlainTextJsonFromResources("registerRequest.json");
		String serviceName = "ServiceTestUnregisterOnDisconnectCycle";
		registerRequestJson = registerRequestJson.replace("TestService", serviceName);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), registerRequestJson);
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		receivedMessageCollector.clearReceivedMessageList();
		
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, eventMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> registerPublishMessage = eventMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(registerPublishMessage, PublishMessageContent.class);
		assertEquals("serviceRegistry/register/" + serviceName,
				((PublishMessageContent) registerPublishMessage.getContent()).getTopic());
		eventMessageCollector.clearReceivedMessageList();

		//Session.close
		final SessionWrapper session = jsonWebSocketClientSessionPool.getSession(getServiceConnectionInformation());
		session.getSession().close(new CloseReason(CloseCodes.GOING_AWAY, "Disconnect"));
		assertUnregisterEventWasPublished(serviceName);

		//Get
		final String getRequestJson = getPlainTextJsonFromResources("getRequest.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getRequestJson);

		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> getResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);

		assertMessageContentType(getResponseMessage, ServiceRegistryGetResponse.class);
		final ServiceRegistryGetResponse serviceRegistryGetResponse = (ServiceRegistryGetResponse) getResponseMessage
				.getContent();
		assertNotNull(serviceRegistryGetResponse.getService());
		assertEquals(serviceName, serviceRegistryGetResponse.getService().getServiceName());
		assertNull(serviceRegistryGetResponse.getService().getUri());
	}

	@Test
	public void testRegisterWithEventPublishCycle() throws Exception {
		final String registerRequestJson = getPlainTextJsonFromResources("registerRequest.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), registerRequestJson);
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		receivedMessageCollector.clearReceivedMessageList();

		final String getRequestJson = getPlainTextJsonFromResources("getRequest.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getRequestJson);

		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> getResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);

		assertMessageContentType(getResponseMessage, ServiceRegistryGetResponse.class);
		final ServiceRegistryGetResponse serviceRegistryGetResponse = (ServiceRegistryGetResponse) getResponseMessage
				.getContent();
		assertNotNull(serviceRegistryGetResponse.getService());
		final ServiceConnectionInformation serviceConnectionInformation = serviceRegistryGetResponse.getService();
		assertEquals("TestService", serviceConnectionInformation.getServiceName());
		assertEquals("wss://1.2.3.4:5678", serviceConnectionInformation.getUri());

		assertRegisterEventWasPublished();
	}

	@Test
	public void testUnregisterWithEventPublishCycle() throws Exception {
		final String registerRequestJson = getPlainTextJsonFromResources("registerRequest.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), registerRequestJson);
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		receivedMessageCollector.clearReceivedMessageList();
		assertRegisterEventWasPublished();
		eventMessageCollector.clearReceivedMessageList();

		final String unregisterRequestJson = getPlainTextJsonFromResources("unregisterRequest.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), unregisterRequestJson);
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		receivedMessageCollector.clearReceivedMessageList();
		
		final String getRequestJson = getPlainTextJsonFromResources("getRequest.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getRequestJson);

		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		final JsonMessage<? extends JsonMessageContent> getResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);

		assertMessageContentType(getResponseMessage, ServiceRegistryGetResponse.class);
		final ServiceRegistryGetResponse serviceRegistryGetResponse = (ServiceRegistryGetResponse) getResponseMessage
				.getContent();
		assertNotNull(serviceRegistryGetResponse.getService());
		String serviceName = "TestService";
		assertEquals(serviceName, serviceRegistryGetResponse.getService().getServiceName());
		assertNull(serviceRegistryGetResponse.getService().getUri());
		assertUnregisterEventWasPublished(serviceName);
	}

	@Test
	public void testRegisterGetUnregisterCycle() throws Exception {
		LOGGER.debug("Sending registerRequest JSON message to {}", getServerURI());
		// message validated in http://jsonlint.com/
		String registerRequest = getPlainTextJsonFromResources("registerRequest.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), registerRequest);
		//Register & Heartbeat
		RetriedAssert.assertWithWait(() -> assertTrue(receivedMessageCollector.isHeartbeatExecuted()));
		assertRegisterEventWasPublished();
		eventMessageCollector.clearReceivedMessageList();

		//Get&List
		receivedMessageCollector.clearReceivedMessageList();
		verifyResponseOnRequest("getRequest.json", "getResponse.json");
		verifyResponseOnRequest("listRequest.json", "listResponse.json");

		//Unregister
		String unregisterRequest = getPlainTextJsonFromResources("unregisterRequest.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), unregisterRequest);

		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		assertMessageContentType(receivedMessageCollector.getMessage(0), EmptyJsonMessageContent.class);
		receivedMessageCollector.clearReceivedMessageList();
		
		//Unregistered get&list
		verifyResponseOnRequest("getRequest.json", "getResponseEmptyUri.json");
		verifyResponseOnRequest("listRequest.json", "listResponseEmpty.json");
		assertUnregisterEventWasPublished("TestService");
	}

	@Test
	public void testEmptyRegisterContentNotAllowed() throws Exception {
		LOGGER.debug("Sending empty JSON message to {}", getServerURI());
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), "");
		// XXX: consider, how to implement this functionality using lock,
		// without retrying every X milliseconds.
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, receivedMessageCollector.getReceivedMessageList()));
		JsonMessage<? extends JsonMessageContent> resultMessage = receivedMessageCollector.getReceivedMessageList()
				.get(0);
		assertNull(resultMessage.getContent());
		assertNotNull(resultMessage.getError());
		assertEquals(ExceptionCodes.ERR_VALIDATION_FAILED, resultMessage.getError().getCode());
	}

	@Test
	public void testMonitoringCalls() throws Exception {
		String pushServiceStatistics = getPlainTextJsonFromResources("pushServiceStatistics.json");
		JsonWebSocketClientSessionPool eventSessionPool = eventServiceJetty.getServerStartup().getInjector()
				.getInstance(JsonWebSocketClientSessionPool.class);
		eventSessionPool.sendAsyncText(getServiceConnectionInformation(), pushServiceStatistics);
		ServiceRegistry serviceRegistry = jettyServerRule.getServerStartup().getInjector().getInstance(ServiceRegistry.class);
		RetriedAssert.assertWithWait(() -> assertThat(serviceRegistry.getServiceStatisticsList(), Matchers.hasSize(1)));
		
		verifyResponseOnRequest("getInfrastructureStatistics.json", "getInfrastructureStatisticsResponse.json", true,
				this::verifyMonitoringResponse);
	}
	
	private void verifyMonitoringResponse(String expected, String actualAsString, JsonMessage<? extends JsonMessageContent> actualAsJsonMessage) {
		assertThat(actualAsJsonMessage.getContent(), instanceOf(InfrastructureStatisticsResponse.class));
		List<ServiceStatistics> statisticsList = ((InfrastructureStatisticsResponse)actualAsJsonMessage.getContent()).getStatisticsList();
		assertThat(statisticsList, Matchers.hasSize(2));
		ServiceStatistics srStatistics = statisticsList.get(1);
		assertThat(srStatistics.getServiceName(), equalTo(ServiceRegistryMessageTypes.SERVICE_NAME));
		expected = expected.replace("123456.1", srStatistics.getUptime().toString());
		expected = expected.replace("123456.2", srStatistics.getMemoryUsage().toString());
		if (srStatistics.getCpuUsage() != null) {
			expected = expected.replace("123456.3", srStatistics.getCpuUsage().toString());
		} else {
			expected = expected.replace("\"cpuUsage\": 123456.3,", "");
		}
		List<MonitoringServiceConnectionInfo> connectionsToService = srStatistics.getConnectionsToService();
		assertThat(connectionsToService, Matchers.hasSize(2));
		assertThat(connectionsToService.stream().map(c -> c.getServiceName()).toArray(),
				Matchers.arrayContainingInAnyOrder(EventMessageTypes.SERVICE_NAME, TEST_CLIENT_SERVICE_NAME));
		
		MonitoringServiceConnectionInfo connectionInfo1 = connectionsToService.get(0);
		expected = expected.replace("1st_connection_placeholder", connectionInfo1.getServiceName());
		expected = expected.replace("123456.4", connectionInfo1.getPort().toString());
		MonitoringServiceConnectionInfo connectionInfo2 = connectionsToService.get(1);
		expected = expected.replace("2nd_connection_placeholder", connectionInfo2.getServiceName());
		expected = expected.replace("123456.5", connectionInfo2.getPort().toString());

		JsonTestUtil.assertJsonContentEqualIgnoringSeq(expected, actualAsString);
	}
	
	@Override
	public ServiceConnectionInformation getServiceConnectionInformation() throws URISyntaxException {
		//return super.getServiceConnectionInformation();
		//for monitoring calls with service registry it is mandatory to use the same connection that was made for registering in service registry
		String serviceRegistryUri = config.getProperty(F4MConfigImpl.SERVICE_REGISTRY_URI);
		return new ServiceConnectionInformation(config.getProperty(F4MConfig.SERVICE_NAME), 
				serviceRegistryUri,
				config.getPropertyAsListOfStrings(F4MConfig.SERVICE_NAMESPACES));
	}
}
