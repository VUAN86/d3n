package de.ascendro.f4m.service.session.pool;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.exception.server.F4MSessionStoreDestroyException;
import de.ascendro.f4m.service.exception.server.F4MSessionStoreInitException;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.RetriedAssertForDefaultService;

public class SessionStoreImplTest {

	private final String CLIENT_ID = UUID.randomUUID().toString();
	private final long SEQUENCE = new SecureRandom().nextLong();

	private SessionStore sessionStore;

	@Mock
	private Config config;
	@Mock
	private LoggingUtil loggingUtil;
	
	@Mock
	private SessionWrapper sessionWrapper;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		when(config.getPropertyAsLong(F4MConfigImpl.CACHE_MANAGER_CLEAN_UP_INTERVAL)).thenReturn(1L);

		sessionStore = new SessionStoreImpl(config, loggingUtil, sessionWrapper);
	}

	@Test
	public void testRegisterClient() {
		when(config.getPropertyAsLong(F4MConfigImpl.USERS_CACHE_TIME_TO_LIVE)).thenReturn(10_000L);
		
		final String clientId = UUID.randomUUID().toString();

		//Register
		sessionStore.registerClient(clientId);
		assertTrue(sessionStore.hasClient(clientId));

		//Remove
		sessionStore.removeClient(clientId);
		assertFalse(sessionStore.hasClient(clientId));

		//Register
		sessionStore.registerClient(clientId);
		assertTrue(sessionStore.hasClient(clientId));
	}

	@Test
	public void testInit() throws F4MSessionStoreInitException, F4MSessionStoreDestroyException {
		when(config.getPropertyAsLong(F4MConfigImpl.USERS_CACHE_TIME_TO_LIVE)).thenReturn(1L);
		final RequestInfo requestInfo = new RequestInfoImpl(0);

		sessionStore.registerClient(CLIENT_ID);
		sessionStore.registerRequest(SEQUENCE, requestInfo);

		assertNotNull(sessionStore.popRequest(SEQUENCE));
		assertTrue(sessionStore.hasClient(CLIENT_ID));

		sessionStore.init();

		RetriedAssertForDefaultService.assertWithWait(() -> assertNull(sessionStore.popRequest(SEQUENCE)));
		RetriedAssertForDefaultService.assertWithWait(() -> assertFalse(sessionStore.hasClient(CLIENT_ID)));
	}

	@Test
	public void testDestroy() throws F4MSessionStoreDestroyException, F4MSessionStoreInitException {
		when(config.getPropertyAsLong(F4MConfigImpl.USERS_CACHE_TIME_TO_LIVE)).thenReturn(Long.MAX_VALUE);
		
		final RequestInfo requestInfo = new RequestInfoImpl(Integer.MAX_VALUE);

		sessionStore.registerClient(CLIENT_ID);
		sessionStore.registerRequest(SEQUENCE, requestInfo);

		assertNotNull(sessionStore.popRequest(SEQUENCE));
		assertTrue(sessionStore.hasClient(CLIENT_ID));

		sessionStore.init();
		sessionStore.destroy();

		assertNull(sessionStore.popRequest(SEQUENCE));
		assertFalse(sessionStore.hasClient(CLIENT_ID));
	}

}
