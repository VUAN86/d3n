package de.ascendro.f4m.service.session.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.websocket.Session;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.server.F4MSessionStoreDestroyException;
import de.ascendro.f4m.service.exception.server.F4MSessionStoreInitException;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.session.SessionWrapper;

public class SessionPoolImplTest {
	private static final String SESSION_ID = UUID.randomUUID().toString();
	private static final String CLIENT_ID = UUID.randomUUID().toString();

	@Mock
	private SessionStoreCreator sessionStoreCreator;

	@Mock
	private Config config;

	@Mock
	private LoggingUtil loggingUtil;
	
	@Mock
	private SessionStoreImpl sessionStore;

	@Mock
	private Session session;

	@Mock
	private SessionWrapper sessionWrapper;

	private SessionPool sessionPool;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		when(sessionStore.getSessionWrapper()).thenReturn(sessionWrapper);
		when(sessionStoreCreator.createSessionStore(config, loggingUtil, sessionWrapper)).thenReturn(sessionStore);
		when(sessionStoreCreator.createSessionStore(config, loggingUtil, sessionWrapper)).thenReturn(sessionStore);

		sessionPool = new SessionPoolImpl(sessionStoreCreator, config, loggingUtil);
	}

	@Test
	public void testRemoveSession() throws F4MSessionStoreDestroyException, F4MSessionStoreInitException {
		assertNotNull(sessionPool.createSession(SESSION_ID, sessionWrapper));
		sessionPool.removeSession(SESSION_ID);

		verify(sessionStore, times(1)).destroy();
	}

	@Test
	public void testCreateSession() throws F4MSessionStoreInitException {
		assertNotNull(sessionPool.createSession(SESSION_ID, sessionWrapper));

		verify(sessionStore, times(1)).init();
	}

	@Test
	public void testGetClientSession() {
		String serviceName = "service";
		when(sessionWrapper.getConnectedClientServiceName()).thenReturn(serviceName);
		when(sessionStore.hasClient(CLIENT_ID)).thenReturn(true);

		sessionPool.createSession(SESSION_ID, sessionWrapper);

		assertEquals(sessionWrapper, sessionPool.getSessionByClientIdServiceName(CLIENT_ID, serviceName));
	}

	@Test
	public void testGetClientSessionWithMissingSession() {
		String serviceName = "service";
		when(sessionWrapper.getConnectedClientServiceName()).thenReturn(serviceName);
		when(sessionStore.hasClient(CLIENT_ID)).thenReturn(false);

		sessionPool.createSession(SESSION_ID, sessionWrapper);
		assertNull(sessionPool.getSessionByClientIdServiceName(CLIENT_ID, serviceName));
	}

	@Test
	public void testGetClientSessionWithMissingService() {
		sessionPool.createSession(SESSION_ID, sessionWrapper);
		assertNull(sessionPool.getSessionByClientIdServiceName(CLIENT_ID, "anotherservice"));
	}

}
