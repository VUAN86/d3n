package de.ascendro.f4m.service.profile.model.delete;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.UserIdentifier;

public class DeleteProfileRequest implements JsonMessageContent, UserIdentifier {
	private String userId;

	@Override
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
}
