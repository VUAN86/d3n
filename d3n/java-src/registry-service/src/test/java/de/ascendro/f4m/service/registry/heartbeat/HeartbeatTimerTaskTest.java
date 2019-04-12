package de.ascendro.f4m.service.registry.heartbeat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import javax.websocket.CloseReason;
import javax.websocket.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.json.JsonMessageDeserializer;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;
import de.ascendro.f4m.service.json.validator.JsonMessageValidator;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.config.ServiceRegistryConfig;
import de.ascendro.f4m.service.registry.server.ServiceRegistrySessionStore;
import de.ascendro.f4m.service.registry.store.ServiceData;
import de.ascendro.f4m.service.registry.store.ServiceRegistry;
import de.ascendro.f4m.service.registry.store.ServiceRegistryImpl;
import de.ascendro.f4m.service.registry.util.ServiceRegistryEventServiceUtil;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.ServiceUtil;

public class HeartbeatTimerTaskTest {

	private static final String TEST_SERVICE = "TestService";

	private static final String TEST_SERVICE_URI1 = "TEST_SERVICE_URI1";
	private static final String TEST_SERVICE_URI2 = "TEST_SERVICE_URI2";
	private static final String TEST_SERVICE_URI3 = "TEST_SERVICE_URI3";

	@Mock
	private Config config;
	
	@Mock
	private LoggingUtil loggingUtil;

	@Mock
	private ServiceRegistry serviceRegistry;
	
	@Mock
	private ServiceRegistrySessionStore serviceRegistrySessionStore;

	@Mock
	private ServiceRegistryEventServiceUtil eventServiceUtil;

	@Mock
	private SessionWrapper testSession1;

	@Mock
	private SessionWrapper testSession2;

	@Mock
	private SessionWrapper testSession3;

	@SuppressWarnings({ "serial", "unused" })
	private final GsonProvider gsonProvider = new GsonProvider(new JsonMessageDeserializer(
			new JsonMessageTypeMapImpl() {
				protected void init() {
				};
			}));

	private final JsonMessageSchemaMap jsonMessageSchemaMap = new DefaultMessageSchemaMapper();
	private final JsonMessageValidator jsonMessageValdiator = new JsonMessageValidator(jsonMessageSchemaMap, config);

	private final JsonMessageUtil jsonUtil = new JsonMessageUtil(gsonProvider, new ServiceUtil(), jsonMessageValdiator);

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRun() throws Exception {
		final int heartbeatDisconnectInterval = 100;
		when(config.getPropertyAsInteger(ServiceRegistryConfig.HEARTBEAT_DISCONNECT_INTERVAL)).thenReturn(
				heartbeatDisconnectInterval);

		prepareTestSession(testSession1, true);
		prepareTestSession(testSession2, true);
		prepareTestSession(testSession3, false);

		final Map<String, ServiceData> testServices = new HashMap<>();

		testServices.put(TEST_SERVICE_URI1, createServiceData(TEST_SERVICE, TEST_SERVICE_URI1, testSession1, DateTimeUtil.getCurrentDateTime()));
		testServices.put(TEST_SERVICE_URI2, createServiceData(TEST_SERVICE, TEST_SERVICE_URI2, testSession2, null));
		testServices.put(TEST_SERVICE_URI3, createServiceData(TEST_SERVICE, TEST_SERVICE_URI3, testSession3, null));

		final Map<String, Map<String, ServiceData>> registeredServices = new HashMap<>();
		registeredServices.put(TEST_SERVICE, testServices);
		when(serviceRegistry.getRegisteredServices()).thenReturn(registeredServices);
		when(serviceRegistry.unregister(any())).thenReturn(true);

		final HeartbeatTimerTask heartbeatTimerTask = new HeartbeatTimerTask(serviceRegistry, jsonUtil,
				config, eventServiceUtil, loggingUtil);

		heartbeatTimerTask.run();

		verify(testSession1, times(1)).sendAsynMessage(any());

		verify(eventServiceUtil, times(0)).publishUnregisterEvent(testServices.get(TEST_SERVICE_URI1));
		verify(serviceRegistry, times(0)).unregister(testServices.get(TEST_SERVICE_URI1));

		// Assumed that this will cause unregister to be sent
		verify(testSession2.getSession(), times(1)).close(any(CloseReason.class));

		verify(eventServiceUtil, times(1)).publishUnregisterEvent(testServices.get(TEST_SERVICE_URI3));
		verify(serviceRegistry, times(1)).unregister(testServices.get(TEST_SERVICE_URI3));
		
		verifyNoMoreInteractions(eventServiceUtil);
	}

	private void prepareTestSession(SessionWrapper sessionWrapper, boolean connected) {
		when(sessionWrapper.isOpen()).thenReturn(connected);
		Session session = Mockito.mock(Session.class);
		when(session.isOpen()).thenReturn(connected);
		when(sessionWrapper.getSession()).thenReturn(session);
	}

	@Test
	public void testMultithreadRun() {
		when(config.getPropertyAsInteger(ServiceRegistryConfig.HEARTBEAT_DISCONNECT_INTERVAL)).thenReturn(
				Integer.MAX_VALUE);
		when(testSession1.getSessionStore()).thenReturn(serviceRegistrySessionStore);
		
		final ServiceRegistry realServiceRegistry = new ServiceRegistryImpl();

		final String serviceName = UUID.randomUUID().toString();

		IntStream.range(1, new SecureRandom().nextInt(50) + 25).forEach(
				i -> realServiceRegistry.register(serviceName, "wss://localhost:123456", Collections.singletonList(serviceName), testSession1));

		final HeartbeatTimerTask heartbeatTimerTask = new HeartbeatTimerTask(realServiceRegistry,
				jsonUtil, config, eventServiceUtil, loggingUtil);

		final int threadCount = 10;
		final HeartbeatTimerTaskThread[] heartbeatTimerTaskExecutorThreads = new HeartbeatTimerTaskThread[threadCount];
		IntStream.range(0, heartbeatTimerTaskExecutorThreads.length).forEach(
				i -> heartbeatTimerTaskExecutorThreads[i] = new HeartbeatTimerTaskThread(
						new HeartbeatTimerTaskRunnable(heartbeatTimerTask)));

		Arrays.stream(heartbeatTimerTaskExecutorThreads).parallel().forEach(t -> t.start());

		Arrays.stream(heartbeatTimerTaskExecutorThreads).forEach(t -> t.joinSilently());

		for (HeartbeatTimerTaskThread t : heartbeatTimerTaskExecutorThreads) {
			assertFalse(t.getErrorsAsString(), t.hasErrors());
		}
	}

	private ServiceData createServiceData(String serviceName, String uri, SessionWrapper session,
			ZonedDateTime lastHeartbeatResponse) {
		final ServiceData serviceData = new ServiceData(serviceName, uri, Collections.singletonList(serviceName), session);
		serviceData.setLastHeartbeatResponse(lastHeartbeatResponse);

		return serviceData;
	}

	@Test
	public void testIsReceivedHeartbeatResponseInLastHeartbeatInterval() {
		final HeartbeatTimerTask heartbeatTimerTask = new HeartbeatTimerTask(serviceRegistry, jsonUtil,
				config, null, loggingUtil);

		final int heartbeatDisconnectInterval = 100;
		when(config.getPropertyAsInteger(ServiceRegistryConfig.HEARTBEAT_DISCONNECT_INTERVAL)).thenReturn(
				heartbeatDisconnectInterval);

		assertTrue(heartbeatTimerTask.isReceivedHeartbeatResponseInLastHeartbeatInterval(createServiceData(DateTimeUtil.getCurrentDateTime())));
		assertFalse(heartbeatTimerTask.isReceivedHeartbeatResponseInLastHeartbeatInterval(createServiceData(ZonedDateTime.of(0, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))));

		assertTrue(heartbeatTimerTask.isReceivedHeartbeatResponseInLastHeartbeatInterval(createServiceData(
				DateTimeUtil.getCurrentDateTime().minusNanos(heartbeatDisconnectInterval * DateTimeUtil.MILLI_TO_NANOSECONDS / 2))));
		assertFalse(heartbeatTimerTask.isReceivedHeartbeatResponseInLastHeartbeatInterval(createServiceData(
				DateTimeUtil.getCurrentDateTime().minusNanos(heartbeatDisconnectInterval * DateTimeUtil.MILLI_TO_NANOSECONDS))));
	}

	private ServiceData createServiceData(ZonedDateTime date) {
		ServiceData data = new ServiceData("serviceName", "uri", Collections.emptyList(), testSession1);
		data.setLastHeartbeatResponse(date);
		return data;
	}

	static class HeartbeatTimerTaskRunnable implements Runnable {
		private final List<Exception> errors = new ArrayList<>();

		private final HeartbeatTimerTask heartbeatTimerTask;

		public HeartbeatTimerTaskRunnable(HeartbeatTimerTask heartbeatTimerTask) {
			this.heartbeatTimerTask = heartbeatTimerTask;
		}

		@Override
		public void run() {
			try {
				heartbeatTimerTask.run();
			} catch (Exception e) {
				errors.add(e);
			}
		}

		public List<Exception> getErrors() {
			return errors;
		}

		public void addError(Exception e) {
			errors.add(e);
		}

		public boolean hasErrors() {
			return !errors.isEmpty();
		}

		public String getErrorsAsString() {
			final StringBuilder errorMessage = new StringBuilder("Errors[");

			errors.forEach(e -> errorMessage.append(e.getClass().getSimpleName() + " -> " + e.getMessage()).append(','));

			errorMessage.append("]");

			return errorMessage.toString();
		}
	}

	static class HeartbeatTimerTaskThread extends Thread {
		private final HeartbeatTimerTaskRunnable heartbeatTimerTaskRunnable;

		public HeartbeatTimerTaskThread(HeartbeatTimerTaskRunnable heartbeatTimerTaskRunnable) {
			super(heartbeatTimerTaskRunnable);
			this.heartbeatTimerTaskRunnable = heartbeatTimerTaskRunnable;
		}

		public void joinSilently() {
			try {
				super.join();
			} catch (InterruptedException e) {
				heartbeatTimerTaskRunnable.addError(e);
			}
		}

		public List<Exception> getErrors() {
			return heartbeatTimerTaskRunnable.getErrors();
		}

		public String getErrorsAsString() {
			return toString() + ':' + heartbeatTimerTaskRunnable.getErrorsAsString();
		}

		public boolean hasErrors() {
			return heartbeatTimerTaskRunnable.hasErrors();
		}
	}
}
