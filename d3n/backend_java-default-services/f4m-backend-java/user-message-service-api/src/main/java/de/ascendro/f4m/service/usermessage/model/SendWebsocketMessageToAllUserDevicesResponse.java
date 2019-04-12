package de.ascendro.f4m.service.usermessage.model;

/**
 * Response in case of successfully sent direct message to all devices of a user
 * via WebSocket.
 */
public class SendWebsocketMessageToAllUserDevicesResponse extends SendWebsocketMessageResponse {

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SendWebsocketMessageToAllUserDevicesResponse [messageId=");
		builder.append(getMessageId());
		builder.append("]");
		return builder.toString();
	}
}
