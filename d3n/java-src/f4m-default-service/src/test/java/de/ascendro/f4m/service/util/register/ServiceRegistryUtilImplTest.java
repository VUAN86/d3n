package de.ascendro.f4m.service.util.register;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.store.ServiceStoreImpl;
import de.ascendro.f4m.service.util.ServiceUriUtil;
import static org.mockito.Mockito.*;

public class ServiceRegistryUtilImplTest {

	private final String[] SERVICE_REGISTRY_LINKS = new String[] { "wss://127.0.0.1:8443", "WSS://127.0.0.1:8444",
			"wss://127.0.0.1:8445/", "wsS://127.0.0.1:8446", "Wss://127.0.0.8:8447" };

	@Mock
	private F4MConfigImpl config;

	@Mock
	private JsonWebSocketClientSessionPool webSocketClient;

	@Mock
	private JsonMessageUtil jsonUtil;

	@Mock
	private ServiceStoreImpl serviceStore;

	private ServiceUriUtil serviceUriUtil;
	@Mock
	private LoggingUtil loggingUtil;

	private ServiceRegistryClient serviceRegistryClient;

	@Before
	public void setUp() throws Exception {
		serviceUriUtil = new ServiceUriUtil();

		MockitoAnnotations.initMocks(this);
		when(config.getPropertyAsListOfStrings(F4MConfigImpl.SERVICE_REGISTRY_URI)).thenReturn(
				Arrays.asList(SERVICE_REGISTRY_LINKS));

		serviceRegistryClient = new ServiceRegistryClientImpl(config, webSocketClient, jsonUtil, serviceUriUtil,
				serviceStore, loggingUtil);
	}

	@Test
	public void testContainsServiceRegistryUri() {
		final int randomUriIndex = new Random().nextInt(SERVICE_REGISTRY_LINKS.length);

		assertTrue(serviceRegistryClient.containsServiceRegistryUri(URI.create(SERVICE_REGISTRY_LINKS[randomUriIndex])));

		assertFalse(serviceRegistryClient.containsServiceRegistryUri(URI.create("wss://127.0.0.1:8743")));
	}

}
