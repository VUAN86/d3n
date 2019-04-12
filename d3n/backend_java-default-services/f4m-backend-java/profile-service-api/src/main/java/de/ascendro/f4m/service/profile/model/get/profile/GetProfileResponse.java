package de.ascendro.f4m.service.profile.model.get.profile;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import com.google.gson.JsonElement;

/**
 * Publicly available profile getter - response.
 */
public class GetProfileResponse implements JsonMessageContent {
	private JsonElement profile;

	public GetProfileResponse() {
	}

	public GetProfileResponse(JsonElement profile) {
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
		return "GetProfileResponse{" +
				"profile=" + profile +
				'}';
	}
}
