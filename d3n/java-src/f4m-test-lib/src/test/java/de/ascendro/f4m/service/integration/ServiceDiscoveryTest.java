package de.ascendro.f4m.service.integration;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import de.ascendro.f4m.client.WebSocketClientSessionPoolImplTest;
import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.EmbeddedJettyServer;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.integration.rule.CompleteServiceStartupRule;
import de.ascendro.f4m.service.integration.rule.MockServiceRule;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypes;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.registry.model.ServiceRegistryGetRequest;
import de.ascendro.f4m.service.registry.model.ServiceRegistryGetResponse;
import de.ascendro.f4m.service.registry.model.ServiceRegistryListRequest;
import de.ascendro.f4m.service.registry.model.ServiceRegistryListResponse;
import de.ascendro.f4m.service.registry.model.ServiceRegistryRegisterRequest;
import de.ascendro.f4m.service.registry.model.ServiceRegistryUnregisterEvent;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

/**
 * Service dependencies: -- Service registry 8443: no dependencies -- Service 8444: depends on Service registry --
 * Service 8445: depends on Service registry and Service Registry
 */
public class ServiceDiscoveryTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketClientSessionPoolImplTest.class);

	private static final String SERVICE_REGISTRY_NAME = "SERVICE-REGISTRY";
	private static final int SERVICE_REGISTRY_PORT = 8443;
	private static final URI DUMMY_SERVICE_REGISTRY_URI = URI.create("wss://localhost:" + SERVICE_REGISTRY_PORT);

	private static final String SERVICE_8444_NAME = "SERVICE_8444";
	private static final int SERVICE_8444_PORT = 8444;

	private static final String SERVICE_8445_NAME = "SERVICE_8445";
	private static final int SERVICE_8445_PORT = 8445;

	@ClassRule
	public static final TemporaryFolder keystoreFolder = new TemporaryFolder();

	public MockServiceRule service8444 = new CompleteServiceStartupRule(SERVICE_8444_PORT, new ErrorCollector(),
			SERVICE_8444_NAME, Collections.emptyList(), true) {
		@Override
		protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandler() {
			return originalMessageDecoded -> null;
		}
	};

	public MockServiceRule service8445 = new CompleteServiceStartupRule(SERVICE_8445_PORT, new ErrorCollector(),
			SERVICE_8445_NAME, Arrays.asList(SERVICE_8444_NAME), true) {

		@Override
		protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandler() {
			return originalMessageDecoded -> null;
		}
	};

	public MockServiceRule serviceRegistry = new CompleteServiceStartupRule(SERVICE_REGISTRY_PORT,
			new ErrorCollector(), SERVICE_REGISTRY_NAME, Collections.emptyList(), false) {

		@Override
		protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandler() {
			return ctx -> onServiceRegsitryReceivedMessage(ctx.getMessage());
		}
	};
	
	@Rule
	public RuleChain serviceStartupChain = RuleChain.outerRule(serviceRegistry).around(service8444).around(service8445);

	private final ConcurrentHashMap<String, String> services = new ConcurrentHashMap<>();
	private final Set<String> successfulServiceRegistryGetNames = new ConcurrentHashSet<>();
	private final Set<String> allServiceRegistryGetNames = new ConcurrentHashSet<>();

	private String service8444UriAsString;
	private ServiceRegistryClient service8444ServiceRegistryClient;
	
	private String service8445UriAsString;
	private ServiceRegistryClient service8445ServiceRegistryClient;

	@BeforeClass
	public static void setUpClass() throws IOException {
		System.setProperty(F4MConfigImpl.SERVICE_DISCOVERY_DELAY, "100");//Re-discover each 100ms
		System.setProperty(F4MConfigImpl.SERVICE_CONNECTION_RETRY_DELAY, "100");//re-register each 100ms
		System.setProperty(F4MConfigImpl.SERVICE_RECONNECT_ON_SERVICE_REGISTRY_CLOSE, "false");//re-connect on close
		System.setProperty(F4MConfigImpl.REQUEST_SERVICE_CONNECTION_INFO_ON_CONNECTION_CLOSE, "true");

		System.setProperty(F4MConfigImpl.SERVICE_REGISTRY_URI, DUMMY_SERVICE_REGISTRY_URI.toString());
		KeyStoreTestUtil.initKeyStore(keystoreFolder);
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		//8444
		final Injector service8444Injector = service8444.getServerStartup().getInjector();
		service8444UriAsString = service8444Injector.getInstance(F4MConfigImpl.class)
				.getServiceURI().toString();
		service8444ServiceRegistryClient = service8444Injector.getInstance(ServiceRegistryClient.class);
		
		//8445
		final Injector service8445Injector = service8445.getServerStartup().getInjector();
		service8445UriAsString = service8445Injector.getInstance(F4MConfigImpl.class)
				.getServiceURI().toString();
		service8445ServiceRegistryClient = service8445Injector.getInstance(ServiceRegistryClient.class);
	}

	@After
	public void tearDown() {
		services.clear();
		successfulServiceRegistryGetNames.clear();
	}

	@Test
	public void testServiceRegistration() throws InterruptedException, IOException, URISyntaxException {
		RetriedAssert.assertWithWait(() -> assertEquals(2, services.size()));

		assertThat(services.keySet(), hasItems(SERVICE_8444_NAME, SERVICE_8445_NAME));
		assertThat(services.values(), hasItems(service8444UriAsString, service8445UriAsString.toString()));
		
		stopService8444(); 	
		assertThat(services.keySet(), contains(SERVICE_8445_NAME));
	}
	
	@Test
	public void testServiceDiscovery() throws InterruptedException, IOException {
		RetriedAssert.assertWithWait(() -> assertThat(successfulServiceRegistryGetNames, hasItems(SERVICE_8444_NAME)));
		
		RetriedAssert.assertWithWait(() -> assertFalse(service8444ServiceRegistryClient.hasScheduledServiceConnInfoRequests()));
		RetriedAssert.assertWithWait(() -> assertFalse(service8445ServiceRegistryClient.hasScheduledServiceConnInfoRequests()));
		
		//Establish connection
		service8445.getServerStartup().getInjector()
			.getInstance(JsonWebSocketClientSessionPool.class)
			.getSession(new ServiceConnectionInformation(SERVICE_8444_NAME, service8444UriAsString, SERVICE_8444_NAME));
		
		allServiceRegistryGetNames.clear();//Clear get request cache
		services.remove(SERVICE_8444_NAME);
		stopService8444();
		RetriedAssert.assertWithWait(() -> assertEquals(1, services.size()));
		
		//Await for re-discovery on connection close
		RetriedAssert.assertWithWait(() -> assertThat(allServiceRegistryGetNames, hasItems(SERVICE_8444_NAME)));
		allServiceRegistryGetNames.clear();//Clear get request cache
		RetriedAssert.assertWithWait(() -> assertThat(allServiceRegistryGetNames, hasItems(SERVICE_8444_NAME)));
		
		RetriedAssert.assertWithWait(() -> assertFalse(service8444ServiceRegistryClient.hasScheduledServiceConnInfoRequests()));
		RetriedAssert.assertWithWait(() -> assertTrue(service8445ServiceRegistryClient.hasScheduledServiceConnInfoRequests()));
	}
	
	private void stopService8444(){
		try {
			service8444.getServerStartup().stop();
			final EmbeddedJettyServer jetty = service8444.getServerStartup().getInjector().getInstance(EmbeddedJettyServer.class);
			RetriedAssert.assertWithWait(() -> assertTrue(jetty.isStopped()));			
		} catch (Exception e) {
			LOGGER.error("Failed to stop service {}", SERVICE_8444_NAME, e);
		}
	}

	protected JsonMessageContent onServiceRegsitryReceivedMessage(
			JsonMessage<? extends JsonMessageContent> originalMessageDecoded) throws Exception {
		LOGGER.info("Service registry received message:" + originalMessageDecoded);

		final ServiceRegistryMessageTypes type = originalMessageDecoded.getType(ServiceRegistryMessageTypes.class);

		final JsonMessageContent result;
		switch (type) {
		case REGISTER:
			final ServiceRegistryRegisterRequest registerRequest = (ServiceRegistryRegisterRequest) originalMessageDecoded
					.getContent();
			ConcurrentUtils.putIfAbsent(services, registerRequest.getServiceName(), registerRequest.getUri());
			result = new EmptyJsonMessageContent();
			break;
		case UNREGISTER:
			final ServiceRegistryUnregisterEvent unregisterEvent = (ServiceRegistryUnregisterEvent)originalMessageDecoded.getContent();
			services.remove(unregisterEvent.getServiceName());
			result = new EmptyJsonMessageContent();
			break;
		case GET:
			result = onGet(originalMessageDecoded);
			break;
		case LIST:
			result = onList(originalMessageDecoded);
			break;
		default:
			result = null;
		}
		return result;
	}

	/**
	 * @param originalMessageDecoded
	 */
	private ServiceRegistryGetResponse onGet(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		final ServiceRegistryGetRequest getRequest = (ServiceRegistryGetRequest) originalMessageDecoded.getContent();
		allServiceRegistryGetNames.add(getRequest.getServiceName());
		LOGGER.info("GET:no uri:{}", getRequest.toString());

		final String uri = services.get(getRequest.getServiceName());
		final ServiceRegistryGetResponse serviceRegistryGetResponse;
		if (uri != null) {
			LOGGER.info("GET:has uri[{}]:{}", uri, getRequest.toString());
			successfulServiceRegistryGetNames.add(getRequest.getServiceName());	
			serviceRegistryGetResponse = new ServiceRegistryGetResponse(new ServiceConnectionInformation(getRequest.getServiceName(), uri, Collections.singletonList(getRequest.getServiceName())));
		}else{//reply only if record present - emulate behaviour, when Service Registry is not responding for some reason (usually it should) 
			serviceRegistryGetResponse = null;
		}
		return serviceRegistryGetResponse;
	}

	/**
	 * @param originalMessageDecoded
	 * @return
	 */
	private ServiceRegistryListResponse onList(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		final ServiceRegistryListRequest listRequest = (ServiceRegistryListRequest) originalMessageDecoded.getContent();

		LOGGER.info("LIST:", listRequest.toString());
		final String uri = services.get(listRequest.getServiceName());
		final ServiceRegistryListResponse listRespoonse = new ServiceRegistryListResponse();
		listRespoonse.setServices(Collections.singletonList(new ServiceConnectionInformation(listRequest
				.getServiceName(), uri, Collections.singletonList("testNS"))));
		return listRespoonse;
	}
}
