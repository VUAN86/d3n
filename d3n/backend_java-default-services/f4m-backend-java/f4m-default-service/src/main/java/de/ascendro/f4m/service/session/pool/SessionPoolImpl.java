package de.ascendro.f4m.service.session.pool;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.server.F4MSessionStoreDestroyException;
import de.ascendro.f4m.service.exception.server.F4MSessionStoreInitException;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.session.SessionWrapper;

public class SessionPoolImpl extends ConcurrentHashMap<String, SessionStore> implements SessionPool {
	private static final long serialVersionUID = 6729100847775093488L;
	private static final Logger LOGGER = LoggerFactory.getLogger(SessionPoolImpl.class);

	private final SessionStoreCreator sessionStoreCreator;
	private final Config config;
	private final LoggingUtil loggingUtil;

	@Inject
	public SessionPoolImpl(SessionStoreCreator sessionStoreCreator, Config config, LoggingUtil loggingUtil) {
		this.sessionStoreCreator = sessionStoreCreator;
		this.config = config;
		this.loggingUtil = loggingUtil;
	}

	@Override
	public SessionStore getSession(String sessionId) {
		return get(sessionId);
	}
	
	@Override
	public Collection<SessionStore> allSessionStores() {
		return values();
	}

	@Override
	public SessionStore removeSession(String sessionId) throws F4MSessionStoreDestroyException {
		final SessionStore sessionStore = remove(sessionId);
		sessionStore.destroy();
		return sessionStore;
	}

	@Override
	public SessionStore createSession(String sessionId, SessionWrapper sessionWrapper) throws F4MSessionStoreInitException {
		final SessionStore sessionStore = sessionStoreCreator.createSessionStore(config, loggingUtil, sessionWrapper);
		sessionStore.init();
		put(sessionId, sessionStore);
		return sessionStore;
	}

	@Override
	public SessionWrapper getSessionByClientIdServiceName(final String clientId, final String serviceName) {
		SessionWrapper clientSessionWrapper = null;

		for (Entry<String, SessionStore> sessionStoreEntry : entrySet()) {
			final SessionStore sessionStore = sessionStoreEntry.getValue();
			String connectedClientServiceName = sessionStore.getSessionWrapper().getConnectedClientServiceName();
			if (serviceName.equals(connectedClientServiceName) && sessionStore.hasClient(clientId)) {
				clientSessionWrapper = sessionStore.getSessionWrapper();
				break;
			}
		}

		LOGGER.debug("In session pool for client [{}] found session wrappper [{}] ", clientId, clientSessionWrapper);
		return clientSessionWrapper;
	}

}
