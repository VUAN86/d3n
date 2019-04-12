package de.ascendro.f4m.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.inject.Provider;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.ServiceEndpoint;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.registry.exception.F4MServiceConnectionInformationNotFoundException;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.SessionWrapperFactory;

public class WebSocketClientSessionPoolImpl extends WebSocketClient implements WebSocketClientSessionPool {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketClientSessionPoolImpl.class);

	private final Map<String, SessionWrapperFuture> clientSessionCache = new ConcurrentHashMap<>();

	private SessionWrapperFactory wrapperFactory;

	public WebSocketClientSessionPoolImpl(Provider<? extends ServiceEndpoint<?, ?, ?>> endpointProvider,
			SessionWrapperFactory wrapperFactory, Config config) {
		super(endpointProvider, config);
		this.wrapperFactory = wrapperFactory;
	}

	@Override
	public SessionWrapper getSession(ServiceConnectionInformation serviceConnectionInformation) {		
		if (validateServiceConnectionInformation(serviceConnectionInformation)) {
			SessionWrapperFuture sessionFuture = clientSessionCache.get(serviceConnectionInformation.getUri());
			if (sessionFuture == null) {
				LOGGER.info("Opening new client session to URI[{}] service {} from {}", serviceConnectionInformation.getUri(),
						serviceConnectionInformation.getServiceName(), config.getProperty(F4MConfigImpl.SERVICE_NAME));
				
				sessionFuture = createSession(serviceConnectionInformation);
			} else if (!toSessionWrapper(sessionFuture).isOpen()) {// closed session
				synchronized (clientSessionCache) {
					sessionFuture = clientSessionCache.get(serviceConnectionInformation.getUri());
					if (sessionFuture != null && !toSessionWrapper(sessionFuture).isOpen()) {
						LOGGER.debug("Remove closed session[{}]", toSessionWrapper(sessionFuture).getSessionId());
						clientSessionCache.remove(serviceConnectionInformation.getUri());
					}
					
					LOGGER.info("Re-opening client session to URI[{}]", serviceConnectionInformation.getUri());
				}
				sessionFuture = createSession(serviceConnectionInformation);
			} else {
				LOGGER.trace("Found existing session[{}]", toSessionWrapper(sessionFuture).getSessionId());
			}
			
			return toSessionWrapper(sessionFuture);
		}else{
			throw new F4MServiceConnectionInformationNotFoundException(
					"Service connection information must be specified to connect: " + serviceConnectionInformation);
		}
	}

	private boolean validateServiceConnectionInformation(ServiceConnectionInformation serviceConnectionInformation) {
		return serviceConnectionInformation != null 
				&& serviceConnectionInformation.getUri() != null
				&& serviceConnectionInformation.getServiceName() != null;
	}

	private SessionWrapperFuture createSession(ServiceConnectionInformation serviceConnectionInformation) {
		SessionWrapperFuture sessionFuture;
		final Pair<SessionWrapperFuture, Boolean> result = createAndPutSessionFuture(serviceConnectionInformation);
		sessionFuture = result.getKey();

		if (result.getValue()) {
			((FutureTask<SessionWrapper>) sessionFuture).run();// Open the connection
		}
		return sessionFuture;
	}

	protected SessionWrapper toSessionWrapper(SessionWrapperFuture sessionFuture) {
		SessionWrapper sessionWrapper = null;
		try {
			sessionWrapper = sessionFuture.get();
			if (sessionWrapper == null) {
				clientSessionCache.remove(sessionFuture.getUri());
				throw new F4MIOException("Has no WebSocket connection for URI[" + sessionFuture.getUri() + "]");
			}
		} catch (InterruptedException | ExecutionException e) {
			clientSessionCache.remove(sessionFuture.getUri());
			throw new F4MIOException("Did not establish WebSocket connection to URI[" + sessionFuture.getUri() + "]",
					e);
		}
		return sessionWrapper;
	}

	protected synchronized Pair<SessionWrapperFuture, Boolean> createAndPutSessionFuture(final ServiceConnectionInformation serviceConnectionInformation) {
		SessionWrapperFuture sessionFuture = clientSessionCache.get(serviceConnectionInformation.getUri());

		final boolean needsExecution;
		if (sessionFuture == null) {
			needsExecution = true;

			sessionFuture = new SessionWrapperFuture(new Callable<SessionWrapper>() {
				@Override
				public SessionWrapper call() {
					SessionWrapper sessionWrapper;
					try {
						sessionWrapper = wrapperFactory.create(connect(serviceConnectionInformation));
					} catch (F4MIOException e) {
						sessionWrapper = null;
						LOGGER.error("Cannot establish WebSocket connection to URI[" + serviceConnectionInformation.getUri() + "]", e);
					}
					return sessionWrapper;
				}
			}, serviceConnectionInformation);
			clientSessionCache.put(serviceConnectionInformation.getUri(), sessionFuture);
		} else {
			needsExecution = false;
		}

		return new ImmutablePair<>(sessionFuture, needsExecution);
	}

	@Override
	public void sendAsyncText(ServiceConnectionInformation serviceConnectionInformation, String message) {
		final SessionWrapper session = getSession(serviceConnectionInformation);
		session.sendAsynText(message);
	}

	@Override
	public List<SessionWrapper> getOpenSessions() {
		final Collection<SessionWrapperFuture> futureSessions = clientSessionCache.values();

		final List<SessionWrapper> sessions = new ArrayList<>();
		for (SessionWrapperFuture futureSession : futureSessions) {
			sessions.add(toSessionWrapper(futureSession));
		}
		return sessions;
	}

	static class SessionWrapperFuture extends FutureTask<SessionWrapper> {
		private final ServiceConnectionInformation serviceConnectionInformation;

		public SessionWrapperFuture(Callable<SessionWrapper> callable, ServiceConnectionInformation serviceConnectionInformation) {
			super(callable);
			this.serviceConnectionInformation = serviceConnectionInformation;
		}

		public ServiceConnectionInformation getServiceConnectionInformation() {
			return serviceConnectionInformation;
		}
		
		public String getUri(){
			return serviceConnectionInformation.getUri();
		}
	}

}
