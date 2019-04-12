package de.ascendro.f4m.service.usermessage.model;

import java.util.Arrays;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Response in case of successful mobile push to a user's device(s).
 */
public class SendUserPushResponse implements JsonMessageContent {
	private String[] notificationIds;

	public String[] getNotificationIds() {
		return notificationIds;
	}

	public void setNotificationIds(String[] notificationIds) {
		this.notificationIds = notificationIds;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SendUserPushResponse [notificationIds=");
		builder.append(Arrays.toString(notificationIds));
		builder.append("]");
		return builder.toString();
	}
}
