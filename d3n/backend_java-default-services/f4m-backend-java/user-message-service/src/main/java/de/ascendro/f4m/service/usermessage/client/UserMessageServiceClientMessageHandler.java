package de.ascendro.f4m.service.usermessage.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.usermessage.direct.DirectWebsocketMessageCoordinator;
import de.ascendro.f4m.service.usermessage.model.NewWebsocketMessageRequest;
import de.ascendro.f4m.service.usermessage.model.NewWebsocketMessageResponse;

/**
 * Handles all User Message Service calls, where User Message is client side.
 */
public class UserMessageServiceClientMessageHandler extends DefaultJsonMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserMessageServiceClientMessageHandler.class);
	private DirectWebsocketMessageCoordinator directWebsocketMessageCoordinator;

	public UserMessageServiceClientMessageHandler(DirectWebsocketMessageCoordinator directWebsocketMessageCoordinator) {
		this.directWebsocketMessageCoordinator = directWebsocketMessageCoordinator;
	}

	@Override
	public JsonMessageContent onUserMessage(RequestContext context) {
		JsonMessage<? extends JsonMessageContent> message = context.getMessage();
		RequestInfo originalRequestInfo = context.getOriginalRequestInfo();
		if (originalRequestInfo != null) {
			if (message.getContent() instanceof NewWebsocketMessageResponse) {
				processReceivedGatewaySendUserMessageResponse(message.getContent(), originalRequestInfo);
			} else {
				LOGGER.error("Unrecognized original message type for message {}", message);
			}
		} else {
			LOGGER.error("OriginalRequestInfo not found for {}", message.getContent());
		}
		return null;
	}

	private void processReceivedGatewaySendUserMessageResponse(JsonMessageContent receivedResponse,
			RequestInfo originalRequestInfo) {
		NewWebsocketMessageResponse gatewaySendUserMessageResponse = (NewWebsocketMessageResponse) receivedResponse;
		if (NewWebsocketMessageResponse.Status.SUCCESS == gatewaySendUserMessageResponse.getDeliveryStatus()) {
			@SuppressWarnings("unchecked")
			JsonMessage<NewWebsocketMessageRequest> websocketRequest = (JsonMessage<NewWebsocketMessageRequest>) originalRequestInfo
					.getSourceMessage();
			String deviceUUID = gatewaySendUserMessageResponse.getDeviceUUID();
			directWebsocketMessageCoordinator.markMessageRead(websocketRequest.getContent(), deviceUUID);
		}
	}
}
