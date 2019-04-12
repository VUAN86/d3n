package de.ascendro.f4m.service.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.registry.config.ServiceRegistryConfig;
import de.ascendro.f4m.service.registry.heartbeat.HeartbeatTimer;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.registry.model.ServiceRegistryListRequest;
import de.ascendro.f4m.service.registry.model.ServiceRegistryListResponse;
import de.ascendro.f4m.service.registry.model.ServiceRegistryRegisterRequest;
import de.ascendro.f4m.service.registry.util.ServiceRegistryEventServiceUtil;
import de.ascendro.f4m.service.util.EventServiceClient;

public class ServiceRegistryWithMultipleServicesTest extends ServiceRegistryStartupTestBase {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistryWithMultipleServicesTest.class);
	private static final String TEST_SERVICE_NAME = "TestService";
	private static final String OTHER_TEST_SERVICE_NAME = "OtherService";

	@Mock
	private ServiceRegistryEventServiceUtil serviceRegistryEventServiceUtil;
	
	@Rule
	public HeartbeatRule heartbeatRule = new HeartbeatRule(jettyServerRule.getServerStartup().getInjector()
			.getInstance(HeartbeatTimer.class)) {
		@Override
		protected void before() throws Throwable {
			config = jettyServerRule.getServerStartup().getInjector().getInstance(Config.class);
			// since we'll not be actually starting other service, it may get disconnected otherwise
			config.setProperty(ServiceRegistryConfig.HEARTBEAT_DISCONNECT_INTERVAL, "");
			super.before();
		};
	};
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
	};

	@Test
	public void testRegisterGetUnregisterCycleWithMultipleServices() throws Exception {
		LOGGER.debug("Sending registerRequest JSON message to {}", getServerURI());
		registerOtherService();
		registerTestService();
		verify(serviceRegistryEventServiceUtil, times(2)).publishRegisterEvent(anyString(), any());
		
		JsonMessage<ServiceRegistryListRequest> listRequest = jsonUtil
				.createNewMessage(ServiceRegistryMessageTypes.LIST, new ServiceRegistryListRequest());
		jsonWebSocketClientSessionPool.sendAsyncMessage(getServiceConnectionInformation(), listRequest);
		RetriedAssert.assertWithWait(() -> {
			ServiceRegistryListResponse listResponse = findListResponse();
			int numberOfResponses = 0;
			if (listResponse != null) {
				numberOfResponses = listResponse.getServices().size();
			}
			// Wait for both services to reply
			assertEquals(2, numberOfResponses);
		});
		ServiceRegistryListResponse listResponse = findListResponse();
		LOGGER.info("Got list response {}", listResponse);
		boolean testServiceFound = false;
		boolean otherTestServiceFound = false;
		for (ServiceConnectionInformation info : listResponse.getServices()) {
			if (TEST_SERVICE_NAME.equals(info.getServiceName())) {
				testServiceFound = true;
			} else if (OTHER_TEST_SERVICE_NAME.equals(info.getServiceName())) {
				otherTestServiceFound = true;
			}
		}
		assertTrue("Both services should have been returned in list response, testServiceFound = "
				+ testServiceFound + ", otherTestServiceFound = " + otherTestServiceFound,
				testServiceFound && otherTestServiceFound);
		
		final String unregisterRequestJson = getPlainTextJsonFromResources("unregisterRequest.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), unregisterRequestJson);
		RetriedAssert.assertWithWait(() -> verify(serviceRegistryEventServiceUtil).publishUnregisterEvent(any()));
	}

	private void registerOtherService() throws Exception {
		receivedMessageCollector.clearReceivedMessageList();
		String registerRequestString = getPlainTextJsonFromResources("registerRequest.json");
		JsonMessage<ServiceRegistryRegisterRequest> registerRequestJson = jsonUtil.fromJson(registerRequestString);
		registerRequestJson.getContent().setServiceName(OTHER_TEST_SERVICE_NAME);
		registerRequestJson.getContent().setServiceNamespaces(Collections.singletonList("namespace3"));
		jsonWebSocketClientSessionPool.sendAsyncMessage(getServiceConnectionInformation(), registerRequestJson);
		// Register
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		receivedMessageCollector.clearReceivedMessageList();
	}

	private void registerTestService() throws Exception {
		receivedMessageCollector.clearReceivedMessageList();
		String registerRequestString = getPlainTextJsonFromResources("registerRequest.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), registerRequestString);
		// Register
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		receivedMessageCollector.clearReceivedMessageList();
	}

	private ServiceRegistryListResponse findListResponse() {
		ServiceRegistryListResponse listResponse = null;
		for (JsonMessage<? extends JsonMessageContent> json : receivedMessageCollector.getReceivedMessageList()) {
			if (json.getContent() instanceof ServiceRegistryListResponse) {
				listResponse = (ServiceRegistryListResponse) json.getContent();
			}
		}
		return listResponse;
	}
	
	@Override
	protected ServiceStartup getServiceStartup() {
		return new RegistryServiceStartup(DEFAULT_TEST_STAGE) {
			@Override
			public Injector createInjector(Stage stage) {
				final Module superModule = Modules.override(getBaseModules()).with(super.getModules());
				return Guice.createInjector(Modules.override(superModule).with(getMockModules()));
			}

			protected Iterable<? extends Module> getMockModules() {
				return Arrays.asList(new AbstractModule() {
					@Override
					protected void configure() {
						serviceRegistryEventServiceUtil = mock(ServiceRegistryEventServiceUtil.class);
						bind(EventServiceClient.class).toInstance(serviceRegistryEventServiceUtil);
						bind(ServiceRegistryEventServiceUtil.class).toInstance(serviceRegistryEventServiceUtil);
						
					}
				});
			}
		};
	}
}
