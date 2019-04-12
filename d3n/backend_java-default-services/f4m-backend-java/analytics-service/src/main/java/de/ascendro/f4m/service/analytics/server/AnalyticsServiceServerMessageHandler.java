package de.ascendro.f4m.service.analytics.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class AnalyticsServiceServerMessageHandler extends DefaultJsonMessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsServiceServerMessageHandler.class);

    @Override
    public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> message) {
        // Notification module for Analytics Service is not expecting any message as server
        LOGGER.debug("Analytics Service as server received content {} in message {}",
                message != null ? message.getContent() : null, message);
        throw new F4MValidationFailedException("Analytics Service is not expecting any message as server");
    }
}
