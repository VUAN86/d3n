package de.ascendro.f4m.service.profile.model.find;

import com.google.gson.JsonObject;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

import java.util.List;

public class FindListByIdentifiersResponse implements JsonMessageContent {

	private List<JsonObject> profileList;

	public FindListByIdentifiersResponse() {
		// empty constructor
	}

	public FindListByIdentifiersResponse(List<JsonObject> profileList) {
		this.profileList = profileList;
	}

	public List<JsonObject> getProfileList() {
		return profileList;
	}

	public void setProfileList(List<JsonObject> profileList) {
		this.profileList = profileList;
	}

}
