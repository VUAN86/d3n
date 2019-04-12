package de.ascendro.f4m.service.session.pool;

import java.util.Collection;

import de.ascendro.f4m.service.exception.server.F4MSessionStoreDestroyException;
import de.ascendro.f4m.service.exception.server.F4MSessionStoreInitException;
import de.ascendro.f4m.service.session.SessionWrapper;

public interface SessionPool {
	/**
	 * Get Session store for provided session id
	 * @param sessionId - WS Session id
	 * @return Found Session Store or null if not found
	 */
	SessionStore getSession(String sessionId);

	/**
	 * Remove Session store by WS Session Id
	 * @param sessionId
	 * @return removed session id
	 * @throws F4MSessionStoreDestroyException
	 */
	SessionStore removeSession(String sessionId) throws F4MSessionStoreDestroyException;

	/**
	 * Create new session store for provided session id
	 * @param sessionId - WS session id
	 * @return New session store
	 * @throws F4MSessionStoreInitException
	 */
	SessionStore createSession(String sessionId, SessionWrapper sessionWrapper) throws F4MSessionStoreInitException;

	/**
	 * Lookup session by provided client id
	 * @param clientId - Client id to lookup
	 * @return Found Session for provided client id
	 */
	SessionWrapper getSessionByClientIdServiceName(String clientId, String serviceName);
	
	Collection<SessionStore> allSessionStores();
}
