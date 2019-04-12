package de.ascendro.f4m.service.session;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;

import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.SendHandler;
import javax.websocket.Session;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.common.WebSocketSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

import de.ascendro.f4m.client.WebSocketClient;
import de.ascendro.f4m.client.WebSocketClientSessionPoolImpl;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessageError;
import de.ascendro.f4m.service.logging.LoggedMessageAction;
import de.ascendro.f4m.service.logging.LoggedMessageType;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypes;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.session.pool.SessionStore;

public abstract class WebSocketSessionWrapper implements SessionWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketSessionWrapper.class);
	
	private static final String MESSAGE_PREFIX_STRING = "\"message\":\"";
	private static final String MESSAGE_SUFFIX_STRING = "\"";
	private static final String HEARTBEAT_MESSAGE_STRING = MESSAGE_PREFIX_STRING
			+ ServiceRegistryMessageTypes.HEARTBEAT.getMessageName() + MESSAGE_SUFFIX_STRING;
	private static final String HEARTBEAT_RESPONSE_MESSAGE_STRING = MESSAGE_PREFIX_STRING
			+ ServiceRegistryMessageTypes.HEARTBEAT_RESPONSE.getMessageName() + MESSAGE_SUFFIX_STRING;
	private static final String PUSH_STATISTICS_MESSAGE_STRING = MESSAGE_PREFIX_STRING
			+ ServiceRegistryMessageTypes.PUSH_SERVICE_STATISTICS.getMessageName() + MESSAGE_SUFFIX_STRING;

	protected final Session session;
	protected final SessionPool sessionPool;
	protected final Config config;
	protected final JsonMessageUtil jsonUtil;
	protected final LoggingUtil loggingUtil;

	public WebSocketSessionWrapper(@Assisted Session session, SessionPool sessionPool, Config config,
			JsonMessageUtil jsonUtil, LoggingUtil loggingUtil) {
		this.session = session;
		this.sessionPool = sessionPool;
		this.config = config;
		this.jsonUtil = jsonUtil;
		this.loggingUtil = loggingUtil;
	}

	@Override
	public Session getSession() {
		return session;
	}

	@Override
	public boolean isOpen() {
		return session.isOpen();
	}

	@Override
	public Async getAsyncRemote() {
		return session.getAsyncRemote();
	}

	@Override
	public Basic getBasicRemote() {
		return session.getBasicRemote();
	}

	@Override
	public void sendAsynMessage(JsonMessage<? extends JsonMessageContent> message)
			throws F4MValidationFailedException {
		sendAsynText(jsonUtil.toJson(message), createMessageSendHandler(message));
	}

	@Override
	public void sendAsynMessage(JsonMessage<? extends JsonMessageContent> message, RequestInfo requestInfo, boolean forwardClientInfo) {
		final ForwardedMessageSendHandler forwardedMessageSendHandler = createForwardedMessageSendHandler(message, requestInfo);
		if (forwardClientInfo) {
			copyClientInfo(requestInfo.getSourceMessage(), message);
		}
		getSessionStore().registerRequest(message.getSeq(), requestInfo);
		sendAsynText(jsonUtil.toJson(message), forwardedMessageSendHandler);
	}
	
	protected void copyClientInfo(JsonMessage<?> sourceMessage, JsonMessage<?> targetMessage) {
		if (sourceMessage != null && targetMessage.getClientInfo() == null && sourceMessage.getClientInfo() != null) {
			targetMessage.setClientInfo(sourceMessage.getClientInfo());
		} else if (targetMessage.getClientInfo() != null) {
			LOGGER.warn("Target json message already has client info specified set: [{}]", targetMessage);
		} else if (sourceMessage != null && sourceMessage.getClientInfo() == null) {
			LOGGER.debug("Source json message is missing client info: [{}]", sourceMessage);
		}
	}

	@Override
	public void sendAsynText(String text) {
		getAsyncRemote().sendText(text);
	}

	private void sendAsynText(String text, SendHandler handler) {
		assert validateJson(text);
		int textLength = StringUtils.length(text);
		if (textLength >= session.getMaxTextMessageBufferSize()) {
			//adding more elaborate message for error being thrown by org.eclipse.jetty.websocket.api.WebSocketPolicy.assertValidTextMessageSize
			LOGGER.error("Cannot send message too large - expected max size {} but was {} {}",
					session.getMaxTextMessageBufferSize(), textLength, text);
		}
		getAsyncRemote().sendText(text, handler);

		boolean isHeartbeat = text != null && (
				text.contains(HEARTBEAT_MESSAGE_STRING) || text.contains(HEARTBEAT_RESPONSE_MESSAGE_STRING)
					|| text.contains(PUSH_STATISTICS_MESSAGE_STRING));
		loggingUtil.logProtocolMessage(LoggedMessageType.DATA, LoggedMessageAction.SENT, "Message sent",
				text, this, isHeartbeat);

		final String sentMessageLogFormat = "Sent asynchronous JSON message {} via {}";
		if (isHeartbeat) {
			LOGGER.trace(sentMessageLogFormat, text, getSessionId());
		} else {
			LOGGER.debug(sentMessageLogFormat, text, getSessionId());
		}
	}
	
	private boolean validateJson(String text) {
		jsonUtil.validate(text);
		return true;
	}

	protected MessageSendHandler createMessageSendHandler(JsonMessage<? extends JsonMessageContent> message) {
		return new MessageSendHandler(jsonUtil, this, message);
	}

	protected ForwardedMessageSendHandler createForwardedMessageSendHandler(
			JsonMessage<? extends JsonMessageContent> message, RequestInfo requestInfo) {
		return new ForwardedMessageSendHandler(jsonUtil, this, message, requestInfo);
	}

	@Override
	public void sendErrorMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded, Throwable e) {
		sendAsynMessage(jsonUtil.createResponseErrorMessage(
				(e instanceof F4MException) ? (F4MException) e : new F4MFatalErrorException("Failed to process message", e) 
				, originalMessageDecoded));
	}

	@Override
	public void sendErrorMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded,
			JsonMessageError error) {
		sendAsynMessage(jsonUtil.createResponseErrorMessage(error, originalMessageDecoded));
	}

	@Override
	public Map<String, Object> getUserProperties() {
		return session.getUserProperties();
	}

	@Override
	public URI getLocalClientSessionURI() {
		final ServiceConnectionInformation serviceConnectionInformation = getServiceConnectionInformation();
		return serviceConnectionInformation != null ? URI.create(serviceConnectionInformation.getUri()) : null;
	}
	
	@Override
	public ServiceConnectionInformation getServiceConnectionInformation(){
		return (ServiceConnectionInformation) getUserProperties().get(WebSocketClientSessionPoolImpl.SERVICE_CONNECTION_INFO);
	}

	@Override
	public String getSessionId() {
		return getSession().getId();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends SessionStore> S getSessionStore() {
		return (S) sessionPool.getSession(getSessionId());
	}

	@Override
	public Config getConfig() {
		return config;
	}
	
	private WebSocketSession getWebSocketSession() {
		if (getSession() instanceof WebSocketSession) {
			return (WebSocketSession)getSession();
		} else {
			LOGGER.error("Not a websocket session, was {}", getSession());
			throw new F4MFatalErrorException("Not a websocket session");
		}
	}
	
	@Override
	public String getSource() {
		return StringUtils.substringBefore(getSessionId(), "->");
	}

	@Override
	public String getTarget() {
		final String sessionId = getSessionId();
		return sessionId.substring(sessionId.contains("->") ? sessionId.indexOf("->") + 2 : 0);
	}
	
	@Override
	public boolean isClient() {
		return WebSocketBehavior.CLIENT == getWebSocketSession().getConnection().getPolicy().getBehavior();
	}
	
	@Override
	public String getConnectedClientServiceName() {
		return getWebSocketSession().getUpgradeRequest().getHeader(WebSocketClient.SERVICE_NAME_HEADER);
	}
	
	@Override
	public InetSocketAddress getRemoteAddress() {
		return getWebSocketSession().getRemoteAddress();
	}
}
