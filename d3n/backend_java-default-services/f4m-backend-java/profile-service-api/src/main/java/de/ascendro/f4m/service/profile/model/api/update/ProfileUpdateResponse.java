package de.ascendro.f4m.service.profile.model.api.update;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.api.ApiProfile;

public class ProfileUpdateResponse implements JsonMessageContent {
	private ApiProfile profile;

	public ProfileUpdateResponse(Profile profile) {
		this.profile = profile == null ? null : new ApiProfile(profile);
	}

	public ApiProfile getProfile() {
		return profile;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("ProfileUpdateResponse [");
		builder.append("profile=").append(profile);
		builder.append("]");
		return builder.toString();
	}

}
