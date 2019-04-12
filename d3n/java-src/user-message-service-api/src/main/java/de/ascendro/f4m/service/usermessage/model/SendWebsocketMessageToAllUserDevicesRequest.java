package de.ascendro.f4m.service.usermessage.model;

/**
 * Request to send a direct message to all devices of a user via WebSocket.
 */
public class SendWebsocketMessageToAllUserDevicesRequest extends SendWebsocketMessageRequest {
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SendWebsocketMessageToAllUserDevicesRequest [");
		contentsToString(builder);
		builder.append("]");
		return builder.toString();
	}
}
