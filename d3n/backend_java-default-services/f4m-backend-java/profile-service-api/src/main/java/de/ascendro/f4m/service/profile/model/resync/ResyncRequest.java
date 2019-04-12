package de.ascendro.f4m.service.profile.model.resync;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ResyncRequest implements JsonMessageContent {

	private String userId;
	
	public ResyncRequest() {
		// Initialize empty object
	}

	public ResyncRequest(String userId) {
		setUserId(userId);
	}
	
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("userId=").append(userId);
		builder.append("]");
		return builder.toString();
	}

}
