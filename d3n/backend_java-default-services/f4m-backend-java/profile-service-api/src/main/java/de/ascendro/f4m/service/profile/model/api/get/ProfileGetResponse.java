package de.ascendro.f4m.service.profile.model.api.get;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileStats;
import de.ascendro.f4m.service.profile.model.api.ApiProfile;

/**
 * Publicly available profile getter - response.
 */
public class ProfileGetResponse implements JsonMessageContent {
	
	private ApiProfile profile;

	public ProfileGetResponse(Profile profile, ProfileStats profileStats) {
		this.profile = profile == null ? null : new ApiProfile(profile);
		if (this.profile!=null) {
			this.profile.setStats(profileStats);
		}
	}

	public ApiProfile getProfile() {
		return profile;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("ProfileGetResponse [");
		builder.append("profile=").append(profile);
		builder.append("]");
		return builder.toString();
	}

}
