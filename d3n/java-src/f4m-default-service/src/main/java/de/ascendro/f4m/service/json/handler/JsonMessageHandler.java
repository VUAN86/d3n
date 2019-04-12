package de.ascendro.f4m.service.json.handler;

import java.time.format.DateTimeParseException;

import javax.websocket.EncodeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.validation.F4MValidationException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.handler.ServiceMessageHandler;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessageError;
import de.ascendro.f4m.service.session.SessionWrapper;

public abstract class JsonMessageHandler
		extends ServiceMessageHandler<RequestContext, String, JsonMessageContent> {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonMessageHandler.class);

	protected JsonMessageUtil jsonMessageUtil;

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
			RequestContext requestContext, Throwable e) {
		JsonMessage<? extends JsonMessageContent> originalMessage = requestContext.getMessage();
		if (originalMessage != null) {
			LOGGER.error("Failed to process message with seq[{}], message {}", originalMessage.getSeq(), originalMessage, e);
		} else {
			LOGGER.error("Failed to process message [{}]: ", originalMessageEncoded, e);
			originalMessage = tryParsingJsonMessageBaseAttributes(originalMessageEncoded);
		}
		sendErrorMessage(originalMessage, e);
	}
	
	private JsonMessage<? extends JsonMessageContent> tryParsingJsonMessageBaseAttributes(String originalMessageEncoded) {
		try {
			return jsonMessageUtil.parseBaseAttributesFromJson(originalMessageEncoded);
		} catch (Exception e) {
			LOGGER.warn("Failed attempt to parse only base attributes of incorrect message", e);
			return null;
		}
	}

	protected void sendErrorMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded, Throwable e) {
		getSessionWrapper().sendErrorMessage(originalMessageDecoded, e);
	}

	protected void sendErrorMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded, JsonMessageError error) {
		getSessionWrapper().sendErrorMessage(originalMessageDecoded, error);
	}
	
	@Override
	public void onEncodeFailure(RequestContext requestContext, EncodeException e) {
		onFailure(null, requestContext, e);
	}

	@Override
	public void onResponse(RequestContext requestContext,
			JsonMessageContent responseMessageContent) {
		sendResponse(requestContext.getMessage(), responseMessageContent, getSessionWrapper());
	}

    protected void sendResponse(JsonMessage<? extends JsonMessageContent> originalMessageDecoded,
			JsonMessageContent responseMessageContent, Object sessionWrapper) {
    	sendResponse(originalMessageDecoded, responseMessageContent, (SessionWrapper) sessionWrapper, null);
    }
    
    protected void sendResponse(JsonMessage<? extends JsonMessageContent> originalMessageDecoded,
			JsonMessageContent responseMessageContent, SessionWrapper sessionWrapper, JsonMessageError error) {
		final String messageResponseName = JsonMessage.getResponseMessageName(originalMessageDecoded.getName());
		JsonMessage<JsonMessageContent> responseMessage = jsonMessageUtil.createNewResponseMessage(messageResponseName,
				responseMessageContent, originalMessageDecoded);
		if (error != null) {
			responseMessage.setError(error);
		}
		sessionWrapper.sendAsynMessage(responseMessage);
	}

	public void sendAsyncMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		RequestContext sendingContext = new RequestContext(originalMessageDecoded);
		sendAsyncMessage(sendingContext);
	}

	public void setJsonMessageUtil(JsonMessageUtil jsonUtil) {
		this.jsonMessageUtil = jsonUtil;
	}

	public JsonMessageUtil getJsonMessageUtil() {
		return jsonMessageUtil;
	}

}
