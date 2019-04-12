package de.ascendro.f4m.service.friend.model.api.buddy;

import de.ascendro.f4m.service.friend.model.api.ApiBuddy;
import de.ascendro.f4m.service.friend.model.api.ApiProfile;

public class ApiBuddyListResult {

	private ApiBuddy buddy;
	private ApiProfile profile;

	public ApiBuddyListResult(ApiBuddy buddy) {
		this.buddy = buddy;
	}
	
	public ApiBuddy getBuddy() {
		return buddy;
	}

	public ApiProfile getProfile() {
		return profile;
	}
	
	public void setProfile(ApiProfile profile) {
		this.profile = profile;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("buddy=").append(buddy);
		builder.append(", profile=").append(profile);
		builder.append("]");
		return builder.toString();
	}

}
