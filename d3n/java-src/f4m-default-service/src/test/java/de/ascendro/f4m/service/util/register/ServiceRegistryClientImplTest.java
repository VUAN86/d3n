package de.ascendro.f4m.service.util.register;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.config.F4MConfig;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypes;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.registry.store.ServiceStoreImpl;
import de.ascendro.f4m.service.util.RetriedAssertForDefaultService;
import de.ascendro.f4m.service.util.ServiceUriUtil;

public class ServiceRegistryClientImplTest {

	private static final String SERVICE_NAME = "testService";
	private static final String SERVICE_URI = "wss://localhost:8797";
	private static final String SERVICE_NAMESPACE = "testNamespace";
	private static final ServiceConnectionInformation SERVICE_INFO =
			new ServiceConnectionInformation(SERVICE_NAME, SERVICE_URI, SERVICE_NAMESPACE);

	@Mock
	private JsonWebSocketClientSessionPool webSocketClient;

	@Mock
	private JsonMessageUtil jsonUtil;

	@Mock
	private ServiceStoreImpl serviceStoreImpl;

	@Mock
	private F4MConfigImpl config;
	@Mock
	private LoggingUtil loggingUtil;
	private ServiceUriUtil serviceUriUtil;

	private ServiceRegistryClient serviceRegistryClient;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		serviceUriUtil = new ServiceUriUtil();

		when(config.getServiceURI()).thenReturn(URI.create(SERVICE_URI));
		when(config.getPropertyAsListOfStrings(F4MConfigImpl.SERVICE_REGISTRY_URI))
				.thenReturn(Arrays.asList("wss://service.registry:12345"));

		serviceRegistryClient = new ServiceRegistryClientImpl(config, webSocketClient, jsonUtil, serviceUriUtil,
				serviceStoreImpl, loggingUtil);
	}

	@Test
	public void testAddService() {
		serviceRegistryClient.scheduledServiceInfoRequest(SERVICE_NAME, Integer.MAX_VALUE);
		assertTrue(serviceRegistryClient.hasScheduledServiceConnInfoRequests());

		serviceRegistryClient.addService(SERVICE_INFO);

		verify(serviceStoreImpl, times(1)).addService(SERVICE_INFO);
		assertFalse(serviceRegistryClient.hasScheduledServiceConnInfoRequests());
	}

	@Test
	public void verifyRegisterIsNotCalledMultipleTimes() throws Exception {
		when(config.getPropertyAsListOfStrings(F4MConfig.SERVICE_NAMESPACES)).thenReturn(Arrays.asList("ns"));
		when(config.getProperty(F4MConfig.SERVICE_NAME)).thenReturn("test_service");
		when(jsonUtil.createNewMessage(ServiceRegistryMessageTypes.REGISTER)).thenReturn(new JsonMessage<>(ServiceRegistryMessageTypes.REGISTER));
		
		doThrow(new F4MIOException("Excpected")).doNothing().when(webSocketClient).sendAsyncMessage(any(), any());
		
		serviceRegistryClient.register();
		RetriedAssertForDefaultService.assertWithWait(
				() -> verify(webSocketClient, times(2)).sendAsyncMessage(any(), any()));
		verify(config, times(1)).getPropertyAsLong(F4MConfigImpl.SERVICE_CONNECTION_RETRY_DELAY);
	}
}
