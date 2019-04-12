package de.ascendro.f4m.service.usermessage.dao.model;

import java.util.List;

import de.ascendro.f4m.service.usermessage.model.MessageInfo;

public class UserMessage extends MessageInfo {
	//a nasty side-effect - if user connects with a new device after the message has been sent, he won't receive message, since it is not in unread list
	private List<String> unreadDeviceUUID;

	public List<String> getUnreadDeviceUUID() {
		return unreadDeviceUUID;
	}

	public void setUnreadDeviceUUID(List<String> unreadDeviceUUID) {
		this.unreadDeviceUUID = unreadDeviceUUID;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserMessage [unreadDeviceUUID=");
		builder.append(unreadDeviceUUID);
		builder.append(", messageId=");
		builder.append(messageId);
		builder.append(", message=");
		builder.append(message);
		builder.append(", payload=");
		builder.append(payload);
		builder.append(", type=");
		builder.append(type);
		builder.append("]");
		return builder.toString();
	}
}
