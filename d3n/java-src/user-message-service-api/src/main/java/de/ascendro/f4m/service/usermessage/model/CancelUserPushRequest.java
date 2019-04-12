package de.ascendro.f4m.service.usermessage.model;

import java.util.Arrays;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Request to cancel scheduled mobile push notifications to user/s device(s).
 */
public class CancelUserPushRequest implements JsonMessageContent {
	private String[] notificationIds;

	public String[] getMessageIds() {
		return notificationIds;
	}

	public void setMessageIds(String[] messageIds) {
		this.notificationIds = messageIds;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CancelMobilePushRequest [notificationIds=");
		builder.append(Arrays.toString(notificationIds));
		builder.append("]");
		return builder.toString();
	}
}
