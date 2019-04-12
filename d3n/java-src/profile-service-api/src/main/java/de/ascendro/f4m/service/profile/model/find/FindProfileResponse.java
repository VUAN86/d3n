package de.ascendro.f4m.service.profile.model.find;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class FindProfileResponse implements JsonMessageContent {
	private String userId;

	public FindProfileResponse() {
	}

	public FindProfileResponse(String userId) {
		this.userId = userId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

}
