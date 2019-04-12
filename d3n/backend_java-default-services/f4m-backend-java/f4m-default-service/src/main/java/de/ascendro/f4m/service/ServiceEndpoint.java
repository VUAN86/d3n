package de.ascendro.f4m.service;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.jsr356.JsrSession;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.handler.MessageHandlerProvider;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.exception.server.F4MSessionStoreDestroyException;
import de.ascendro.f4m.service.exception.server.F4MSessionStoreInitException;
import de.ascendro.f4m.service.handler.F4MMessageHandler;
import de.ascendro.f4m.service.logging.LoggedMessageAction;
import de.ascendro.f4m.service.logging.LoggedMessageType;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.SessionWrapperFactory;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class ServiceEndpoint<D, E, R> extends Endpoint {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceEndpoint.class);

	protected final MessageHandlerProvider<E> messageHandlerProvider;
	protected final SessionPool sessionPool;
	protected final LoggingUtil loggingUtil;
	protected final SessionWrapperFactory sessionWrapperFactory;
	protected final Config config;
	protected final ServiceRegistryClient serviceRegistryClient;
	protected final EventServiceClient eventServiceClient;

	public ServiceEndpoint(MessageHandlerProvider<E> messageHandlerProvider, SessionPool sessionPool,
			LoggingUtil loggingUtil, SessionWrapperFactory sessionWrapperFactory, Config config,
			ServiceRegistryClient serviceRegistryClient, EventServiceClient eventServiceClient) {
		this.messageHandlerProvider = messageHandlerProvider;
		this.sessionPool = sessionPool;
		this.loggingUtil = loggingUtil;
		this.sessionWrapperFactory = sessionWrapperFactory;
		this.config = config;
		this.serviceRegistryClient = serviceRegistryClient;
		this.eventServiceClient = eventServiceClient;
	}

	@Override
	public void onOpen(Session session, EndpointConfig config) {
		loggingUtil.saveBasicInformationInThreadContext();
		final F4MMessageHandler<E> serviceMessageHandler = messageHandlerProvider.get();
		serviceMessageHandler.setSession(session);
		
		final SessionWrapper sessionWrapper = serviceMessageHandler.getSessionWrapper();
		session.addMessageHandler(serviceMessageHandler);
		LOGGER.info("Opened connection {} with client {} service as {}", session.getId(),
				sessionWrapper.getConnectedClientServiceName(), sessionWrapper.isClient() ? "client" : "server");
		try {
			sessionPool.createSession(session.getId(), sessionWrapper);
			loggingUtil.logProtocolMessage(LoggedMessageType.CONNECTION, LoggedMessageAction.CREATED, "Connection created",
					null, sessionWrapper, false);
		} catch (F4MSessionStoreInitException sdEx) {
			LOGGER.error("Failed to initialize Session store", sdEx);
		}
		logSessionCertificateInfo(session, sessionWrapper);
		
		if(sessionWrapper.isClient()){
			clientEndpointPostOpenActions(sessionWrapper);
		}//No server Endpoint post open actions required at the moment
	}
	
	private void logSessionCertificateInfo(Session session, SessionWrapper sessionWrapper) {
		if (session instanceof JsrSession) {
			if (LOGGER.isDebugEnabled()) {
				logJsrSessionCertificateInfo((JsrSession)session, sessionWrapper);
			}
		} else {
			LOGGER.warn("CertificateInfo - {} was not a JserSession as expected", session);
		}
	}
	
	private void logJsrSessionCertificateInfo(JsrSession session, SessionWrapper sessionWrapper) {
		String serverOrClient = sessionWrapper.isClient() ? "CLIENT" : "SERVER";
		UpgradeRequest upgradeRequest = session.getUpgradeRequest();
		if (upgradeRequest instanceof ServletUpgradeRequest) {
			String otherEnd = sessionWrapper.getConnectedClientServiceName();
			ServletUpgradeRequest servletUpgradeRequest = (ServletUpgradeRequest) upgradeRequest;
			Object certificateAttribute = servletUpgradeRequest
					.getServletAttribute("javax.servlet.request.X509Certificate");
			String certificateInfo;
			if (certificateAttribute != null) {
				certificateInfo = getCertificateLogInfo(certificateAttribute);
			} else {
				certificateInfo = "no certificate";
			}
			LOGGER.debug(
					"CertificateInfo - {} as {} to {} for connection with isSecure {}, ssl_session_id {}, cipher_suite {}, key_size {}, X509Certificate {}",
					session.getId(), serverOrClient, otherEnd, servletUpgradeRequest.isSecure(),
					servletUpgradeRequest.getServletAttribute("javax.servlet.request.ssl_session_id"),
					servletUpgradeRequest.getServletAttribute("javax.servlet.request.cipher_suite"),
					servletUpgradeRequest.getServletAttribute("javax.servlet.request.key_size"), certificateInfo);
		} else if (upgradeRequest instanceof ClientUpgradeRequest) {
			String otherEnd = Optional.ofNullable(sessionWrapper.getServiceConnectionInformation())
					.map(ServiceConnectionInformation::getServiceName).orElse("target not set");
			LOGGER.debug("CertificateInfo - {} is {} to {}", session.getId(), serverOrClient,
					otherEnd);
		} else {
			LOGGER.debug("CertificateInfo - upgradeRequest was {}", upgradeRequest);
		}
	}

	private String getCertificateLogInfo(Object certificateAttribute) {
		String certificateInfo;
		if (certificateAttribute instanceof X509Certificate[]) {
			X509Certificate[] certificate = (X509Certificate[]) certificateAttribute;
			if (certificate.length == 1) {
				certificateInfo = certificate[0].getSubjectDN().getName();
			} else {
				certificateInfo = certificate.length + " certificates";
			}
		} else {
			certificateInfo = "unrecognized certificate class "
					+ certificateAttribute.getClass().getSimpleName();
		}
		return certificateInfo;
	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
		loggingUtil.saveBasicInformationInThreadContext();
		try {
			if (session != null) {
				final SessionWrapper sessionWrapper = sessionWrapperFactory.create(session);
				LOGGER.info("Closing session[{}] with {} connected from {} as {}", session.getId(), closeReason,
						sessionWrapper.getConnectedClientServiceName(),
						sessionWrapper.isClient() ? "client" : "server");
				if(!config.getPropertyAsBoolean(F4MConfigImpl.SERVICE_SHUTTING_DOWN) && sessionWrapper.isClient()){
					clientEndpointPostCloseActions(sessionWrapper);
				}//No server Endpoint post close actions required at the moment
				
				removeSessionWithHandlers(session);

				LOGGER.debug("Session[{}] successfully closed", session.getId());
			} else {
				LOGGER.warn("Closing null session with reason {}", closeReason);
			}
		} catch (F4MSessionStoreDestroyException sdEx) {
			LOGGER.error("Failed to destroy Session store", sdEx);
		} catch (Exception e) {
			LOGGER.error("Failed to close Session", e);
		}
		SessionWrapper sessionWrapper = sessionWrapperFactory.create(session);
		loggingUtil.logProtocolMessage(LoggedMessageType.CONNECTION, LoggedMessageAction.REMOVED, "Connection closed",
				null, sessionWrapper, false);
	}
	
	private void clientEndpointPostOpenActions(SessionWrapper sessionWrapper) {
		if (isEventServiceClientConnection(sessionWrapper)) {
			if (eventServiceClient != null) {
				eventServiceClient.signalThatEventServiceConnected();
			}else{
				LOGGER.error("Event Service util is not available for resubscribe action");
			}
		}
	}

	private void clientEndpointPostCloseActions(SessionWrapper sessionWrapper) {
		final boolean reconnectOnServiceRegistryClose = config
				.getPropertyAsBoolean(F4MConfigImpl.SERVICE_RECONNECT_ON_SERVICE_REGISTRY_CLOSE) == Boolean.TRUE;
		
		final boolean requestServiceConnectionInformationOnConnectionClose = config
				.getPropertyAsBoolean(F4MConfigImpl.REQUEST_SERVICE_CONNECTION_INFO_ON_CONNECTION_CLOSE) == Boolean.TRUE;
		
		if (reconnectOnServiceRegistryClose && isServiceRegistryClientConnection(sessionWrapper)) {
			LOGGER.info("Disconnected from Service Registry. Attempt to re-register");
			serviceRegistryClient.register();
		} else if (isEventServiceClientConnection(sessionWrapper)) {
			if (config.getPropertyAsBoolean(F4MConfigImpl.EVENT_SERVICE_CLIENT_AUTO_RESUBSCRIBE_ON_CLOSE) == Boolean.TRUE) {
				LOGGER.info("Disconnected from Event Service. Attempt to re-subscribe");
				eventServiceClient.resubscribe();
			}else{
				LOGGER.warn("Auto resubscribe for events is disabled, see {} property", F4MConfigImpl.EVENT_SERVICE_CLIENT_AUTO_RESUBSCRIBE_ON_CLOSE);
			}
		}else if(requestServiceConnectionInformationOnConnectionClose){
			LOGGER.info("Requesting service [{}] connection information on connection close", sessionWrapper.getServiceConnectionInformation().getServiceName());
			serviceRegistryClient.requestServiceConnectionInformation(sessionWrapper.getServiceConnectionInformation().getServiceName());
		}	
	}

	private boolean isServiceRegistryClientConnection(SessionWrapper sessionWrapper) {
		return serviceRegistryClient.containsServiceRegistryUri(sessionWrapper
					.getLocalClientSessionURI());
	}

	private boolean isEventServiceClientConnection(SessionWrapper sessionWrapper) {
		final URI sessionUri = sessionWrapper.getLocalClientSessionURI();
		final ServiceConnectionInformation eventServiceInfo = serviceRegistryClient
				.getServiceConnInfoFromStore(EventMessageTypes.SERVICE_NAME);

		return eventServiceInfo != null && sessionUri.equals(URI.create(eventServiceInfo.getUri()));
	}

	/**
	 * Remove session from the session pool. Clear message handler reference to session
	 */
	private void removeSessionWithHandlers(Session session) {
		Set<MessageHandler> messageHandlers = session.getMessageHandlers();
		if (messageHandlers.size() != 1) {
			LOGGER.warn("Closing session with {} registered serviceMessageHandlers", messageHandlers.size());
		}
		for (MessageHandler handler : session.getMessageHandlers()) {
			if (handler instanceof F4MMessageHandler) {
				((F4MMessageHandler<?>)handler).destroy();
			}
		}
		sessionPool.removeSession(session.getId());
	}

	@Override
	public void onError(Session session, Throwable thr) {
		loggingUtil.saveBasicInformationInThreadContext();
		LOGGER.error("Error in {}", this, thr);
	}
}
