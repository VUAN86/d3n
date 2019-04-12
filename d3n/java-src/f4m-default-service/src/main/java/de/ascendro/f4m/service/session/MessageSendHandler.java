package de.ascendro.f4m.service.session;

import javax.websocket.SendHandler;
import javax.websocket.SendResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.exception.server.F4MConnectionErrorException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class MessageSendHandler implements SendHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageSendHandler.class);

	protected final JsonMessageUtil jsonMessageUtil;

	private final SessionWrapper sessionWrapper;
	private final JsonMessage<? extends JsonMessageContent> message;

	public MessageSendHandler(JsonMessageUtil jsonMessageUtil, SessionWrapper sessionWrapper,
			JsonMessage<? extends JsonMessageContent> message) {
		this.jsonMessageUtil = jsonMessageUtil;

		this.sessionWrapper = sessionWrapper;
		this.message = message;
	}

	@Override
	public void onResult(SendResult result) {
		if (!result.isOK()) {
			processFailedMessage(result);
		} else {
			LOGGER.trace("Message [{}] successfully sent", message);
		}
	}

	private void processFailedMessage(SendResult result) {
		if (message.getError() == null) {
			LOGGER.error("Attempt to send message [{}] failed", message, result.getException());
			SessionWrapper originalSession = getOriginalSession();
			JsonMessage<? extends JsonMessageContent> originalMessage = getOriginalMessage();
			originalSession.sendErrorMessage(originalMessage,
					new F4MConnectionErrorException("Error while sending message"));
		} else {
			LOGGER.error("Attempt to send error message [{}] failed", message, result.getException());
		}
	}

	protected SessionWrapper getOriginalSession() {
		return this.sessionWrapper;
	}

	protected JsonMessage<? extends JsonMessageContent> getOriginalMessage() {
		return this.message;
	}

}
