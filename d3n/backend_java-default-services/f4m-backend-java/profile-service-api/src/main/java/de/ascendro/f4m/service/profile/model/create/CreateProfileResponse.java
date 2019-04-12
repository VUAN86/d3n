package de.ascendro.f4m.service.profile.model.create;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class CreateProfileResponse implements JsonMessageContent {
	private String userId;

	public CreateProfileResponse() {
	}

	public CreateProfileResponse(String userId) {
		this.userId = userId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

}
