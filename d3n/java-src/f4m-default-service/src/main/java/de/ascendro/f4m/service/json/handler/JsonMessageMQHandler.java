package de.ascendro.f4m.service.json.handler;

import com.google.gson.JsonSyntaxException;
import de.ascendro.f4m.service.communicator.RabbitClientSender;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.validation.F4MValidationException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.handler.ServiceMessageMQHandler;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessageError;
import de.ascendro.f4m.service.json.model.MessageSource;
import de.ascendro.f4m.service.request.RequestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeParseException;
import java.util.Objects;

public abstract class JsonMessageMQHandler
		extends ServiceMessageMQHandler<RequestContext, String, JsonMessageContent, MessageSource> {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonMessageMQHandler.class);

	protected JsonMessageUtil jsonMessageUtil;
	protected JsonMessageUtil jsonUtil;

	@Override
	public String encode(RequestContext request) {
		return jsonMessageUtil.toJson(request.getMessage());
	}
	
	@Override
	public RequestContext prepare() {
		return new RequestContext();
	}

	@Override
	public RequestContext decode(String message, RequestContext context) {
		try {
			context.setMessage(jsonMessageUtil.fromJson(message));
			return context;
		} catch (JsonSyntaxException | DateTimeParseException e) {
			LOGGER.error("Error decoding JSON content", e);
			throw new F4MValidationFailedException("Json message is not valid", e);
		}
	}

	@Override
	public void validate(String message) throws F4MValidationException {
		jsonMessageUtil.validate(message);
	}
	
	@Override
	public void validatePermissions(RequestContext context)
			throws F4MInsufficientRightsException, F4MValidationFailedException {
		if(config.getPropertyAsBoolean(F4MConfigImpl.SERVICE_MESSAGE_VALIDATION_CHECKS_PERMISSIONS) == Boolean.TRUE){
			jsonMessageUtil.validatePermissions(context.getMessage());
		}
	}

	@Override
	public void onFailure(String originalMessageEncoded,
			RequestContext requestContext, MessageSource messageSource, Throwable e) {
		boolean forwarded = forwardErrorMessageToOriginIfExists(originalMessageEncoded, requestContext, e,	null);
		if (!forwarded) {
			System.out.println("onFailure e=" + e.getMessage());
			JsonMessage<? extends JsonMessageContent> originalMessage = requestContext.getMessage();
			if (originalMessage != null) {
				LOGGER.error("Failed to process message with seq[{}], message {}", originalMessage.getSeq(), originalMessage, e);
			} else {
				LOGGER.error("Failed to process message [{}]: ", originalMessageEncoded, e);
				originalMessage = tryParsingJsonMessageBaseAttributes(originalMessageEncoded);
			}
			sendErrorMessage(originalMessage, messageSource, e);
		}
	}

	private JsonMessage<? extends JsonMessageContent> tryParsingJsonMessageBaseAttributes(String originalMessageEncoded) {
		try {
			return jsonMessageUtil.parseBaseAttributesFromJson(originalMessageEncoded);
		} catch (Exception e) {
			LOGGER.warn("Failed attempt to parse only base attributes of incorrect message", e);
			return null;
		}
	}

	protected void sendErrorMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded, MessageSource messageSource, Throwable e) {
		RabbitClientSender.sendErrorMessage(messageSource.getOriginalQueue(), originalMessageDecoded, e);
	}

	//not used
	protected void sendErrorMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded, MessageSource messageSource, JsonMessageError error) {
		RabbitClientSender.sendErrorMessage(messageSource.getOriginalQueue(),originalMessageDecoded, error);
	}
//
//	@Override
//	public void onEncodeFailure(RequestContext requestContext, EncodeException e) {
//		onFailure(null, requestContext, e);
//	}
//
	@Override
	public void onResponse(RequestContext requestContext,
			JsonMessageContent responseMessageContent, MessageSource messageSource) {
		sendResponse(requestContext.getMessage(), responseMessageContent, messageSource);
	}

    protected void sendResponse(JsonMessage<? extends JsonMessageContent> originalMessageDecoded,
			JsonMessageContent responseMessageContent, MessageSource messageSource) {
    	sendResponse(originalMessageDecoded, responseMessageContent, messageSource, null);
    }

    protected void sendResponse(JsonMessage<? extends JsonMessageContent> originalMessageDecoded,
			JsonMessageContent responseMessageContent, MessageSource messageSource, JsonMessageError error) {
		final String messageResponseName = JsonMessage.getResponseMessageName(originalMessageDecoded.getName());
		JsonMessage<JsonMessageContent> responseMessage = jsonMessageUtil.createNewResponseMessage(messageResponseName,
				responseMessageContent, originalMessageDecoded);
		if (error != null) {
			responseMessage.setError(error);
		}
		RabbitClientSender.sendAsyncMessage(messageSource.getOriginalQueue(),responseMessage);
	}

//	public void sendAsyncMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
//		RequestContext sendingContext = new RequestContext(originalMessageDecoded);
//		sendAsyncMessage(sendingContext);
//	}


	protected boolean forwardErrorMessageToOriginIfExists(String originalMessageEncoded, RequestContext requestContext,
														  Throwable e, JsonMessageError error) {
		boolean success;
		final RequestInfo originalFromForward;
		if (!requestContext.isForwardErrorMessagesToOrigin()) {
			final Object original = originalMessageEncoded != null ? originalMessageEncoded : requestContext.getMessage();
			LOGGER.error("Failed to process and will not forward message [{}]: ", original, e);
			success = true;
		} else if ((originalFromForward = requestContext.getOriginalRequestInfo()) != null) {
			final Object original = originalMessageEncoded != null ? originalMessageEncoded : requestContext.getMessage();
			final JsonMessage<? extends JsonMessageContent> messageFromOrigin = originalFromForward.getSourceMessage();
			if (Objects.nonNull(originalFromForward.getSourceMessageSource())) {
				LOGGER.warn("Failed to process message [{}], found message of origin {}, error will be forwarded to {}",
						original, messageFromOrigin, originalFromForward.getSourceMessageSource().getOriginalQueue(), e);

				String originalQueue = originalFromForward.getSourceMessageSource().getOriginalQueue();
				if (e != null) {
					RabbitClientSender.sendErrorMessage(originalQueue,messageFromOrigin, e);
				} else if (error != null) {
					RabbitClientSender.sendErrorMessage(originalQueue,messageFromOrigin, error);
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

	public void setJsonMessageUtil(JsonMessageUtil jsonUtil) {
		this.jsonMessageUtil = jsonUtil;
	}

	public JsonMessageUtil getJsonMessageUtil() {
		return jsonMessageUtil;
	}

}
