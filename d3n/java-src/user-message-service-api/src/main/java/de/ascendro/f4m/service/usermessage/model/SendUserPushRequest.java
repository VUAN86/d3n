package de.ascendro.f4m.service.usermessage.model;

import java.time.ZonedDateTime;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Request to send a scheduled push notification to user (devices).
 */
public class SendUserPushRequest extends SendPushRequest implements JsonMessageContent, MessageToUserIdRequest {
	private String userId;
	private ZonedDateTime scheduledDateTime;

	@Override
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public ZonedDateTime getScheduledDateTime() {
		return scheduledDateTime;
	}

	public void setScheduledDateTime(ZonedDateTime scheduledDateTime) {
		this.scheduledDateTime = scheduledDateTime;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SendUserPushRequest [userId=");
		builder.append(userId);
		contentsToString(builder);
		builder.append(", scheduledDateTime=");
		builder.append(scheduledDateTime);
		builder.append("]");
		return builder.toString();
	}
}
