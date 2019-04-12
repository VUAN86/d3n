package de.ascendro.f4m.service.json.handler;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessageError;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.logging.LoggedMessageAction;
import de.ascendro.f4m.service.logging.LoggedMessageType;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypes;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.pool.SessionStore;

/**
 * In service business functionality must be implemented in of two methods:
 * <li>{@link #onUserMessage(RequestContext)} (*the preferred one)</li>
 * <li>{@link #onUserMessage(ClientInfo, JsonMessage)} (legacy)</li>
 */
public abstract class JsonAuthenticationMessageHandler extends JsonMessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonAuthenticationMessageHandler.class);

	@Override
	public JsonMessageContent onProcess(RequestContext requestContext) {
		long start = System.currentTimeMillis();
		JsonMessageContent response = null;
		
		onAuthentication(requestContext);		
		
		fillOriginalRequestInfo(requestContext);

		// Log response time
		boolean isHeartbeat = isHearbeat(requestContext.getMessage());
		String messageName = requestContext.getMessage().getName();
		String originalRequestMessageName = null;
		if (! isHeartbeat) {
			JsonMessage<? extends JsonMessageContent> sourceMessage = requestContext.getOriginalRequestInfo() == null ? null
					: requestContext.getOriginalRequestInfo().getSourceMessage();
			boolean isResponse = sourceMessage != null;
			if (isResponse) {
				originalRequestMessageName = sourceMessage.getName();
				if (sourceMessage.getTimestamp() != null) {
					long executionTime = System.currentTimeMillis() - sourceMessage.getTimestamp();
					loggingUtil.logExecutionStatistics(String.format("Received response %s of request message %s in %d ms", 
								messageName, originalRequestMessageName, executionTime), 
							messageName, originalRequestMessageName, executionTime);
				}
			}
		}
		
		try {
			// Process
			if (requestContext.getMessage().getError() == null) {
				try {
					response = onUserDefaultMessage(requestContext.getMessage());
				} catch (F4MValidationFailedException uex) {
					response = onUserMessage(requestContext);
				}
			} else {
				handleUserErrorMessage(requestContext);
			}
		} finally {
			// Log message processing
			if (! isHeartbeat) {
				long processingTime = System.currentTimeMillis() - start;
				loggingUtil.logExecutionStatistics(String.format("Processed %s message in %d ms", messageName, processingTime), 
						messageName, originalRequestMessageName, processingTime);
			}
		}
		return response;
	}

	@Override
	protected void logReceivedMessage(RequestContext context, String originalMessageEncoded) {
		JsonMessage<JsonMessageContent> originalMessageDecoded = context.getMessage();
		loggingUtil.logProtocolMessage(LoggedMessageType.DATA, LoggedMessageAction.RECEIVED, "Message received", originalMessageEncoded, 
				getSessionWrapper(), isHearbeat(originalMessageDecoded));
	}

	private boolean isHearbeat(JsonMessage<JsonMessageContent> originalMessageDecoded) {
		boolean isHeartbeat = false;
		if (originalMessageDecoded != null) {
			ServiceRegistryMessageTypes registryMessageType = originalMessageDecoded.getType(ServiceRegistryMessageTypes.class);
			isHeartbeat = registryMessageType == ServiceRegistryMessageTypes.HEARTBEAT 
					|| registryMessageType == ServiceRegistryMessageTypes.HEARTBEAT_RESPONSE
					|| registryMessageType == ServiceRegistryMessageTypes.PUSH_SERVICE_STATISTICS;
		}
		return isHeartbeat;
	}

	@Override
	public ClientInfo onAuthentication(RequestContext requestContext) {
		try {
            final SessionStore sessionStore = getSessionWrapper().getSessionStore();
			LOGGER.warn("onAuthentication sessionStore {}", sessionStore);
            String clientId = requestContext.getMessage().getClientId();
            if (sessionStore != null) {
				sessionStore.registerClient(clientId);
            } else {
            	LOGGER.warn("No session store for session {}, not registering clientId {}", getSessionWrapper().getSessionId(), clientId);
            }
			return requestContext.getMessage().getClientInfo();
		} catch (IllegalArgumentException ilEx) {
			throw new F4MValidationFailedException("Message must be provided", ilEx);
		}
	}

	public void onUserErrorMessage(RequestContext requestContext) {
		//this method is intended to be overridden in services, if specific error handling for received error messages on client interface is necessary  
	}
	
	protected void handleUserErrorMessage(RequestContext requestContext) {
		final JsonMessageError error;
		JsonMessage<? extends JsonMessageContent> originalMessageDecoded = requestContext.getMessage();
		if (originalMessageDecoded != null && originalMessageDecoded.getError() != null) {
			error = originalMessageDecoded.getError();
			LOGGER.error("Received error message[{}]: {} - \"{}\"", originalMessageDecoded.getName(), error.getType(),
					error.getCode());
		} else {
			error = null;
			LOGGER.error("Received error message[{}] with no error block",
					originalMessageDecoded != null ? originalMessageDecoded.getName() : null);
		}

		try {
			onUserErrorMessage(requestContext);
		} finally {
			forwardErrorMessageToOriginIfExists(null, requestContext, null, error);
		}
	}

	@Override
	public void onFailure(String originalMessageEncoded,
			RequestContext requestContext, Throwable e) {
		boolean forwarded = forwardErrorMessageToOriginIfExists(originalMessageEncoded, requestContext, e,
				null);
		if (!forwarded) {
			super.onFailure(originalMessageEncoded, requestContext, e);
		}
	}

	protected boolean forwardErrorMessageToOriginIfExists(String originalMessageEncoded,
			RequestContext requestContext, Throwable e, JsonMessageError error) {
		boolean success;
		final RequestInfo originalFromForward;
		if (!requestContext.isForwardErrorMessagesToOrigin()) {
			final Object original = originalMessageEncoded != null ? originalMessageEncoded : requestContext.getMessage();
			LOGGER.error("Failed to process and will not forward message [{}]: ", original, e);
			success = true;
		} else if ((originalFromForward = requestContext.getOriginalRequestInfo()) != null) {
			final Object original = originalMessageEncoded != null ? originalMessageEncoded : requestContext.getMessage();
			final JsonMessage<? extends JsonMessageContent> messageFromOrigin = originalFromForward.getSourceMessage();
			if (originalFromForward.getSourceSession() != null) {
				LOGGER.warn("Failed to process message [{}], found message of origin {}, error will be forwarded to {}",
						original, messageFromOrigin, originalFromForward.getSourceSession().getSessionId(), e);

				SessionWrapper originalSession = originalFromForward.getSourceSession();
				if (e != null) {
					originalSession.sendErrorMessage(messageFromOrigin, e);
				} else if (error != null) {
					originalSession.sendErrorMessage(messageFromOrigin, error);
				} else {
					LOGGER.error("Cannot forward error message, no error information {}", error, e);
					throw new F4MFatalErrorException("Cannot forward error message, no error information");
				}
			} else {
				LOGGER.error("Cannot forward error message to null session, message was {}, message of origin was {}",
						original, messageFromOrigin, e);
			}
			success = true;
		} else {
			success = false;
		}
		return success;
	}
	
	private void fillOriginalRequestInfo(RequestContext requestContext) {
		LOGGER.debug("fillOriginalRequestInfo before={}",requestContext);
		Optional<Long> ack = Optional.ofNullable(requestContext).map(RequestContext::getMessage)
				.map(JsonMessage::getFirstAck);
		if (ack.isPresent()) {
			SessionStore sessionStore = getSessionWrapper().getSessionStore();
			requestContext.setOriginalRequestInfo(sessionStore.popRequest(ack.get()));
		}
		LOGGER.debug("fillOriginalRequestInfo after={}",requestContext);
	}
	
	public JsonMessageContent onUserMessage(RequestContext ctx) {
		//method might be made abstract, if onUserMessage(userInfo, originalMessageDecoded) is removed
		return onUserMessage(ctx.getMessage());
	}
	
	public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		//empty implementation so that compiler does not complain, if only onUserMessage(RequestContext ctx) is implemented in service
		return null;
	}
	
	public abstract JsonMessageContent onUserDefaultMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded)
					throws F4MException, F4MValidationFailedException;
}
