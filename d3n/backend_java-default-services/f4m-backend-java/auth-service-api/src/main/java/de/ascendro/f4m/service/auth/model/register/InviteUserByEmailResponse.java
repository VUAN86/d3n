package de.ascendro.f4m.service.auth.model.register;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class InviteUserByEmailResponse implements JsonMessageContent {

	private String userId;
	private String token;
	
	public InviteUserByEmailResponse(String userId, String token) {
		this.userId = userId;
		this.token = token;
	}
	
	public String getUserId() {
		return userId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InviteUserByEmailResponse [userId=");
		builder.append(userId);
		builder.append(", token=");
		builder.append(token);
		builder.append("]");
		return builder.toString();
	}
}
