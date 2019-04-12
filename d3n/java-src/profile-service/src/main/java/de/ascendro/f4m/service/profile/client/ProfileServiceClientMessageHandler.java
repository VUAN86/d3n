package de.ascendro.f4m.service.profile.client;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import de.ascendro.f4m.service.session.SessionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessageError;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;

public class ProfileServiceClientMessageHandler extends DefaultJsonMessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileServiceClientMessageHandler.class);

	@Override
	public JsonMessageContent onUserMessage(RequestContext context) {
		processMessages(context, "success response", this::onInsertPaymentUserSuccess);
		return null;
	}

	@Override
	public void onUserErrorMessage(RequestContext context) {
		processMessages(context, "error response", this::onInsertPaymentUserError);
	}

	private void processMessages(RequestContext context, String responseType,
			BiConsumer<RequestContext, PaymentUserRequestInfo> userInsertProcessor) {
		JsonMessage<? extends JsonMessageContent> message = context.getMessage();
		if (PaymentMessageTypes.INSERT_OR_UPDATE_USER_RESPONSE == message.getType(PaymentMessageTypes.class)) {
			PaymentUserRequestInfo originalRequestInfo = context.getOriginalRequestInfo();
			if (originalRequestInfo != null) {
				userInsertProcessor.accept(context, originalRequestInfo);
			} else {
				LOGGER.error("OriginalRequestInfo not found for {} {}", responseType, message.getContent());
			}
		} else {
			LOGGER.warn("Unrecognized original message type for {} {}", responseType, message);
		}
	}

	private void onInsertPaymentUserSuccess(RequestContext context, PaymentUserRequestInfo originalRequestInfo) {
		AtomicInteger numberOfExpectedResponsesRemaining = originalRequestInfo.getNumberOfExpectedResponses();
		JsonMessage<? extends JsonMessageContent> message = context.getMessage();
		if (numberOfExpectedResponsesRemaining != null) {
			int remaining = numberOfExpectedResponsesRemaining.decrementAndGet();
			if (remaining == 0) {
				LOGGER.debug("Forwarding {} to user, all expected payment responses received", originalRequestInfo.getResponseToForward());
				this.sendResponse(originalRequestInfo.getSourceMessage(), originalRequestInfo.getResponseToForward(),
						originalRequestInfo.getSourceSession());
			} else {
				LOGGER.debug("Not forwarding GetAppConfigurationResponse to user, waiting for {} more responses from payment", remaining);
			}
		} else {
			LOGGER.debug("Response to user was already sent for message {}", message);
		}
	}

	private void onInsertPaymentUserError(RequestContext context, PaymentUserRequestInfo originalRequestInfo) {
		AtomicInteger numberOfExpectedResponsesRemaining = originalRequestInfo.getNumberOfExpectedResponses();
		JsonMessage<? extends JsonMessageContent> message = context.getMessage();
		if (numberOfExpectedResponsesRemaining != null && numberOfExpectedResponsesRemaining.decrementAndGet() == 0) {
			JsonMessageError error = message.getError();
			SessionWrapper sessionWrapper = (SessionWrapper ) originalRequestInfo.getSourceSession();
			sessionWrapper.sendErrorMessage(originalRequestInfo.getSourceMessage(), error);
		} else {
			LOGGER.error("Received error from payment on user insert, waiting for other responses {}", message);
			//In case of multiple new tenant ids (unlikely situation) we will wait for other responses.
			//If those are successful, user will receive success even if this one failed!
		}
		//mark error handling finished, do not forward any additional error messages to original caller
		context.setForwardErrorMessagesToOrigin(false);
	}
}
