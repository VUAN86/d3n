package de.ascendro.f4m.service.friend.model.api.contact;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ResyncRequest implements JsonMessageContent {

	private String userId;
	
	public ResyncRequest(String userId) {
		this.userId = userId;
	}
	
	public String getUserId() {
		return userId;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("userId=").append(userId);
		builder.append("]");
		return builder.toString();
	}

}
