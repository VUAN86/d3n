package de.ascendro.f4m.service.usermessage;

import de.ascendro.f4m.service.json.model.type.MessageType;

/**
 * Contains all implemented F4M message names in User Message Service.
 */
public enum UserMessageMessageTypes implements MessageType {
	SEND_SMS, SEND_SMS_RESPONSE, 
	SEND_EMAIL, SEND_EMAIL_RESPONSE,
	SEND_USER_PUSH, SEND_USER_PUSH_RESPONSE,
	SEND_TOPIC_PUSH, SEND_TOPIC_PUSH_RESPONSE,
	SEND_WEBSOCKET_MESSAGE, SEND_WEBSOCKET_MESSAGE_RESPONSE,
	SEND_WEBSOCKET_MESSAGE_TO_ALL_USER_DEVICES, SEND_WEBSOCKET_MESSAGE_TO_ALL_USER_DEVICES_RESPONSE,
	LIST_UNREAD_WEBSOCKET_MESSAGES, LIST_UNREAD_WEBSOCKET_MESSAGES_RESPONSE,
	NEW_WEBSOCKET_MESSAGE, NEW_WEBSOCKET_MESSAGE_RESPONSE,
	CANCEL_USER_PUSH, CANCEL_USER_PUSH_RESPONSE;
	
	public static final String SERVICE_NAME = "userMessage";

	@Override
	public String getShortName() {
		return convertEnumNameToMessageShortName(name());
	}

	@Override
	public String getNamespace() {
		return SERVICE_NAME;
	}
}
