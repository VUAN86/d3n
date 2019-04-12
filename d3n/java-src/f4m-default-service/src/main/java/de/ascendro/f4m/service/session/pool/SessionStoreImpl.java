package de.ascendro.f4m.service.session.pool;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.cache.CacheManager;
import de.ascendro.f4m.service.cache.CachedObject;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.exception.server.F4MSessionStoreDestroyException;
import de.ascendro.f4m.service.exception.server.F4MSessionStoreInitException;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.session.SessionWrapper;

public class SessionStoreImpl implements SessionStore {

	private static final Logger LOGGER = LoggerFactory.getLogger(SessionStoreImpl.class);

    private final CacheManager<String, CachedObject> clients;
//	private final CacheManager<Long, RequestInfo> requests;

	private final Config config;
	private final SessionWrapper sessionWrapper;

	public SessionStoreImpl(Config config, LoggingUtil loggingUtil, SessionWrapper sessionWrapper) {
		this.config = config;
		this.sessionWrapper = sessionWrapper;
		final long cleanUpInterval = config.getPropertyAsLong(F4MConfigImpl.CACHE_MANAGER_CLEAN_UP_INTERVAL);

//		this.requests = new CacheManager<>(cleanUpInterval, loggingUtil);
		this.clients = new CacheManager<>(cleanUpInterval, loggingUtil);
	}

	@Override
	public void registerRequest(long sequence, RequestInfo requestInfo) {
		if (requestInfo != null) {
//			if (requestInfo.getTimeToLive() == null) {
//				requestInfo.setTimeToLive(config.getPropertyAsLong(F4MConfigImpl.REQUESTS_CACHE_TIME_TO_LIVE));
//			}
//			requests.put(sequence, requestInfo);
		}
	}
	
	@Override
    public void registerClient(String clientId) {
        if (StringUtils.isNotBlank(clientId)) {
        	final long clientsCacheTimeToLive = config.getPropertyAsLong(F4MConfigImpl.USERS_CACHE_TIME_TO_LIVE);
        	if(clientsCacheTimeToLive > 0){
	        	final CachedObject cachedClientObject = new CachedObject();
	        	cachedClientObject.setTimeToLive(clientsCacheTimeToLive);
	            clients.put(clientId, cachedClientObject);
        	}
        }
    }
	
	@Override
	public void removeClient(String clientId) {
		clients.remove(clientId);		
	}
	
	@Override
	public boolean hasClient(String clientId) {
		return clients.contains(clientId);
	}

	@Override
	public int getClientCount() {
		return clients.size();
	}

	@Override
	public RequestInfo popRequest(long sequence) {
		return null;//requests.remove(sequence);
	}
	
	@Override
	public RequestInfo peekRequest(long sequence) {
		return null;//requests.getAndRefresh(sequence);
	}
	
	@Override
	public SessionWrapper getSessionWrapper() {
		return sessionWrapper;
	}

	@Override
	public void init() throws F4MSessionStoreInitException {
		Exception cause = null;
        try {
            clients.schedule();
        } catch (Exception e) {
            LOGGER.error("Failed to destroy clients cache", e);
            cause = e;
        }
		try {
//			requests.schedule();
		} catch (Exception e) {
			LOGGER.error("Failed to destroy session requests cache", e);
			cause = e;
		}
		if (cause != null) {
			final F4MSessionStoreInitException sessionInitException = new F4MSessionStoreInitException(
					cause.getMessage());

			sessionInitException.initCause(cause);

			throw sessionInitException;
		}
	}

	@Override
	public void destroy() throws F4MSessionStoreDestroyException {
		Exception cause = null;
        try {
            clients.destroy();
        } catch (Exception e) {
            LOGGER.error("Failed to destroy clients User Info cache", e);
            cause = e;
        }
		try {
//			requests.destroy();
		} catch (Exception e) {
			LOGGER.error("Failed to destroy session requests cache", e);
			cause = e;
		}
		if (cause != null) {
			final F4MSessionStoreDestroyException sessionDestroyException = new F4MSessionStoreDestroyException(
					cause.getMessage());

			sessionDestroyException.initCause(cause);

			throw sessionDestroyException;
		}
	}

}
