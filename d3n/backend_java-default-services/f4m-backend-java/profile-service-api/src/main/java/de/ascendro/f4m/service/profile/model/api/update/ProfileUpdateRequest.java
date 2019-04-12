package de.ascendro.f4m.service.profile.model.api.update;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.api.ApiProfileSettings;
import de.ascendro.f4m.service.profile.model.api.ApiUpdateableProfile;

public class ProfileUpdateRequest implements JsonMessageContent {

	private ApiUpdateableProfile profile;

	public ProfileUpdateRequest(Profile profile) {
		this.profile = new ApiUpdateableProfile(profile);
	}
	
	public ApiUpdateableProfile getProfile() {
		return profile;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("ProfileUpdateRequest [");
		builder.append("profile=").append(profile);
		builder.append("]");
		return builder.toString();
	}

}
