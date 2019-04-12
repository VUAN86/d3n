package de.ascendro.f4m.service.friend.client;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageRequest;
import de.ascendro.f4m.service.usermessage.notification.MobilePushJsonNotification;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

/**
 * Class for sending requests to user message service
 *
 */
public class UserMessageServiceCommunicator {
	private final ServiceRegistryClient serviceRegistryClient;
	private final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private final JsonMessageUtil jsonMessageUtil;

	@Inject
	public UserMessageServiceCommunicator(ServiceRegistryClient serviceRegistryClient,
			JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, JsonMessageUtil jsonMessageUtil) {
		this.serviceRegistryClient = serviceRegistryClient;
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.jsonMessageUtil = jsonMessageUtil;
	}

	public void pushMessageToUser(String userId, MobilePushJsonNotification payload, ClientInfo clientInfo, String messageText, String... params) {
		SendWebsocketMessageRequest requestContent = new SendWebsocketMessageRequest(true);
		requestContent.setUserId(userId);
		requestContent.setMessage(messageText);
		if (ArrayUtils.isNotEmpty(params)) {
			requestContent.setParameters(params);
		}
		requestContent.setPayload(jsonMessageUtil.toJsonElement(payload));
		requestContent.setType(payload.getType());
		JsonMessage<SendWebsocketMessageRequest> message = jsonMessageUtil
				.createNewMessage(UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE, requestContent);
		message.setClientInfo(clientInfo);
		try {
			ServiceConnectionInformation userMessageConnInfo = serviceRegistryClient.getServiceConnectionInformation(UserMessageMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessage(userMessageConnInfo, message);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send SendMobilePushRequest to User Message Service", e);
		}
	}

}
