package de.ascendro.f4m.service.profile.model.list;

import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ProfileListByIdsRequest implements JsonMessageContent {

	private List<String> profilesIds;

	public ProfileListByIdsRequest(List<String> profilesIds) {
		this.profilesIds = profilesIds;
	}

	public List<String> getProfilesIds() {
		return profilesIds;
	}

	public void setProfilesIds(List<String> profilesIds) {
		this.profilesIds = profilesIds;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ProfileListByIdsRequest");
		builder.append(" [profilesIds=").append(profilesIds);
		builder.append("]");

		return builder.toString();
	}

}
