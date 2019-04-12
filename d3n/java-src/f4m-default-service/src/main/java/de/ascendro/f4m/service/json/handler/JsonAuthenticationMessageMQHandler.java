package de.ascendro.f4m.service.json.handler;

import de.ascendro.f4m.service.communicator.RabbitClientSender;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.gateway.GatewayMessageTypes;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.*;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.logging.LoggedMessageAction;
import de.ascendro.f4m.service.logging.LoggedMessageType;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypes;
import de.ascendro.f4m.service.request.RequestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * In service business functionality must be implemented in of two methods:
 * <li>{@link #onUserMessage(RequestContext)} (*the preferred one)</li>
 * <li>{@link #onUserMessage(ClientInfo, JsonMessage)} (legacy)</li>
 */
public abstract class JsonAuthenticationMessageMQHandler extends JsonMessageMQHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonAuthenticationMessageMQHandler.class);

	@Override
	public JsonMessageContent onProcess(RequestContext requestContext, MessageSource messageSource) {
		System.out.println("on process");
		long start = System.currentTimeMillis();
		JsonMessageContent response = null;

		messageSource.setSeq(requestContext.getMessage().getSeq());

//		onAuthentication(requestContext);
		fillOriginalRequestInfo(requestContext);
		requestContext.getMessage().setMessageSource(messageSource);
		// Log response time
		boolean isHeartbeat = isHearbeat(requestContext.getMessage());
		String messageName = requestContext.getMessage().getName();
		System.out.println("messageName = " + messageName);
		String originalRequestMessageName = null;
		if (! isHeartbeat) {
			JsonMessage<? extends JsonMessageContent> sourceMessage = requestContext.getOriginalRequestInfo() == null ? null
					: requestContext.getOriginalRequestInfo().getSourceMessage();
			boolean isResponse = sourceMessage != null;
			if (isResponse) {
				originalRequestMessageName = sourceMessage.getName();
				System.out.println("originalRequestMessageName = " + originalRequestMessageName);
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
					System.out.println("try onUserDefaultMessage requestContext.getMessage="+requestContext.getMessage());
					response = onUserDefaultMessage(requestContext.getMessage());
				} catch (F4MValidationFailedException uex) {
					System.out.println("catch onUserMessage");
					response = onUserMessage(requestContext);
				}
			} else {
				handleUserErrorMessage(requestContext);
			}
			System.out.println("response = " + response);

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
		loggingUtil.logProtocolMQMessage(LoggedMessageType.DATA, LoggedMessageAction.RECEIVED, "Message received", originalMessageEncoded,
				isHearbeat(originalMessageDecoded));
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

//	@Override
//	public ClientInfo onAuthentication(RequestContext requestContext) {
//		try {
//            final SessionStore sessionStore = getSessionWrapper().getSessionStore();
//            String clientId = requestContext.getMessage().getClientId();
//            if (sessionStore != null) {
//				sessionStore.registerClient(clientId);
//            } else {
//            	LOGGER.warn("No session store for session {}, not registering clientId {}", getSessionWrapper().getSessionId(), clientId);
//            }
//			return requestContext.getMessage().getClientInfo();
//		} catch (IllegalArgumentException ilEx) {
//			throw new F4MValidationFailedException("Message must be provided", ilEx);
//		}
//	}

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

//	@Override
//	public void onFailure(String originalMessageEncoded,
//			RequestContext requestContext, Throwable e) {
//		boolean forwarded = forwardErrorMessageToOriginIfExists(originalMessageEncoded, requestContext, e,
//				null);
//		if (!forwarded) {
//			super.onFailure(originalMessageEncoded, requestContext, e);
//		}
//	}


	
	private void fillOriginalRequestInfo(RequestContext requestContext) {
		Optional<Long> ack = Optional.ofNullable(requestContext)
				.map(RequestContext::getMessage)
				.map(JsonMessage::getFirstAck);
		if (ack.isPresent()) {
			MessageSource originalMessageSource = RabbitClientSender.getOriginalRequest(ack.get());
			requestContext.setOriginalRequestInfo(originalMessageSource.getOriginalRequestInfo());
			System.out.println("Fill fillOriginalRequestInfo: originalMessageSource="+originalMessageSource);
		}
	}
	
	public JsonMessageContent onUserMessage(RequestContext ctx) {
		//method might be made abstract, if onUserMessage(userInfo, originalMessageDecoded) is removed
		return onUserMessage(ctx.getMessage());
	}
	
	public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		//empty implementation so that compiler does not complain, if only onUserMessage(RequestContext ctx) is implemented in service
		return null;
	}

	public JsonMessageContent onUserDefaultMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) throws F4MException {
		final JsonMessageContent result;

		final ServiceRegistryMessageTypes serviceRegistryMessageType;
		final GatewayMessageTypes gatewayMessageTypes;

//		if ((gatewayMessageTypes = originalMessageDecoded.getType(GatewayMessageTypes.class)) != null) {
//			result = onGatewayServiceMessage(gatewayMessageTypes, originalMessageDecoded);
//		} else {
			throw new F4MValidationFailedException("Unrecognized Service Message Type["
					+ originalMessageDecoded.getName() + "] is not supported");
//		}    //FIXME

//		return result;
	}

	protected boolean isAdmin(ClientInfo clientInfo) {
		boolean isAdmin = false;
		if (clientInfo != null) {
			Set<String> roles = new HashSet<>(Arrays.asList(clientInfo.getRoles()));
			isAdmin = roles.contains(ClientInfo.TENANT_ROLE_PREFIX + clientInfo.getTenantId() + "_ADMIN");
		}
		return isAdmin;
	}
	protected String getUserId(ClientInfo clientInfo, UserIdentifier userIdentifier) {
		final String userId;
		if (clientInfo != null) {
			userId = clientInfo.getUserId();
		} else {
			userId = userIdentifier.getUserId();
		}
		return userId;
	}

}
