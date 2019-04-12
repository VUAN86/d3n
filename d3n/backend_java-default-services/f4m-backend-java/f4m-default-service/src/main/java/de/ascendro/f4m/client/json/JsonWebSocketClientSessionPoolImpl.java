package de.ascendro.f4m.client.json;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.client.WebSocketClientSessionPoolImpl;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.di.client.JsonServiceClientEndpointProvider;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.SessionWrapperFactory;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.session.pool.SessionStore;

public class JsonWebSocketClientSessionPoolImpl extends WebSocketClientSessionPoolImpl
		implements JsonWebSocketClientSessionPool {

	private static final Logger LOGGER = LoggerFactory.getLogger(JsonWebSocketClientSessionPoolImpl.class);

	private final SessionPool sessionStorePool;

	@Inject
	public JsonWebSocketClientSessionPoolImpl(JsonServiceClientEndpointProvider clientServiceEndpointProvider,
			SessionPool sessionPool, SessionWrapperFactory wrapperFactory, Config config) {
		super(clientServiceEndpointProvider, wrapperFactory, config);
		this.sessionStorePool = sessionPool;
	}

	@Override
	public void sendAsyncMessage(ServiceConnectionInformation serviceConnectionInformation, JsonMessage<? extends JsonMessageContent> message) {
		final SessionWrapper session = getSession(serviceConnectionInformation);
		session.sendAsynMessage(message);
	}

	@Override
	public void sendAsyncMessageWithClientInfo(ServiceConnectionInformation serviceConnectionInformation, JsonMessage<? extends JsonMessageContent> message, RequestInfo requestInfo) {
		sendAsyncMessage(serviceConnectionInformation, message, requestInfo, true);
	}

	@Override
	public void sendAsyncMessageNoClientInfo(ServiceConnectionInformation serviceConnectionInformation, JsonMessage<? extends JsonMessageContent> message, RequestInfo requestInfo) {
		sendAsyncMessage(serviceConnectionInformation, message, requestInfo, false);
	}
	
	private void sendAsyncMessage(ServiceConnectionInformation serviceConnectionInformation, JsonMessage<? extends JsonMessageContent> message, RequestInfo requestInfo, boolean forwardClientInfo) {
		final SessionWrapper session = getSession(serviceConnectionInformation);
		registerRequestInfo(session, requestInfo, message.getSeq());

		session.sendAsynMessage(message, requestInfo, forwardClientInfo);
	}
	
	protected void registerRequestInfo(SessionWrapper session, RequestInfo requestInfo, Long seq) {
		final SessionStore sessionStore = sessionStorePool.getSession(session.getSessionId());

		if (seq != null && requestInfo != null) {
			sessionStore.registerRequest(seq, requestInfo);
		} else {
			LOGGER.warn("Failed to register RequestInfo[{}] for seq[{}]", requestInfo, seq);
		}
	}

}
