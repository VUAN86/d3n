package de.ascendro.f4m.service.profile.model.delete;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonRequiredNullable;

import com.google.gson.JsonElement;

public class DeleteProfileResponse implements JsonMessageContent {
	@JsonRequiredNullable
	private JsonElement profile = null;

	public DeleteProfileResponse() {
	}

	public void setProfile(JsonElement profile) {
		this.profile = profile;
	}

	public JsonElement getProfile() {
		return profile;
	}

}
