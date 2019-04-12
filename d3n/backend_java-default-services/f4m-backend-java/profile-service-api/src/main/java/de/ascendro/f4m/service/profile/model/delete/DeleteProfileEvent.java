package de.ascendro.f4m.service.profile.model.delete;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class DeleteProfileEvent implements JsonMessageContent {
	private String userId;

	public DeleteProfileEvent(String userId) {
		this.userId = userId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

}
