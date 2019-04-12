package de.ascendro.f4m.service.profile.model.list;

import com.google.gson.JsonObject;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

import java.util.List;

public class ProfileListByIdsResponse implements JsonMessageContent {

	private List<JsonObject> profileList;

	public ProfileListByIdsResponse() {
		// empty constructor
	}

	public ProfileListByIdsResponse(List<JsonObject> profileList) {
		this.profileList = profileList;
	}

	public List<JsonObject> getProfileList() {
		return profileList;
	}

	public void setProfileList(List<JsonObject> profileList) {
		this.profileList = profileList;
	}

}
