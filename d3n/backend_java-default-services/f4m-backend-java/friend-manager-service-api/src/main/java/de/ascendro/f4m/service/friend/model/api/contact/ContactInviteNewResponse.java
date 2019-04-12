package de.ascendro.f4m.service.friend.model.api.contact;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ContactInviteNewResponse implements JsonMessageContent {

	private String userId;

	public ContactInviteNewResponse(String userId) {
		this.userId = userId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ContactInviteNewResponse [userId=");
		builder.append(userId);
		builder.append("]");
		return builder.toString();
	}

}
