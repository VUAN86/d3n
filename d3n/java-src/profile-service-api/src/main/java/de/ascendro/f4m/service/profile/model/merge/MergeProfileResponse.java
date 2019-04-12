package de.ascendro.f4m.service.profile.model.merge;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import com.google.gson.JsonElement;

public class MergeProfileResponse implements JsonMessageContent {
	private JsonElement profile;

	public MergeProfileResponse() {
	}

	public MergeProfileResponse(JsonElement profile) {
		this.profile = profile;
	}

	public JsonElement getProfile() {
		return profile;
	}

	public void setProfile(JsonElement profile) {
		this.profile = profile;
	}

}
