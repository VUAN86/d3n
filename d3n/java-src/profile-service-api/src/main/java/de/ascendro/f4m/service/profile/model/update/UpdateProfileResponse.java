package de.ascendro.f4m.service.profile.model.update;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import com.google.gson.JsonElement;

public class UpdateProfileResponse implements JsonMessageContent {
	private JsonElement profile;

	public UpdateProfileResponse() {
	}

	public UpdateProfileResponse(JsonElement profile) {
		this.profile = profile;
	}

	public JsonElement getProfile() {
		return profile;
	}

	public void setProfile(JsonElement profile) {
		this.profile = profile;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UpdateProfileResponse [profile=");
		builder.append(profile);
		builder.append("]");
		return builder.toString();
	}

}
