package de.ascendro.f4m.service.profile.model.update;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import com.google.gson.JsonElement;

public class UpdateProfileRequest implements JsonMessageContent {
	/**
	 * From other services only.
	 */
	private String userId;
	private String service;
	private JsonElement profile;

	public UpdateProfileRequest() {
	}

	public UpdateProfileRequest(JsonElement profile) {
		this.profile = profile;
	}

	public UpdateProfileRequest(String userId) {
		this.userId = userId;
	}

	public JsonElement getProfile() {
		return profile;
	}

	public void setProfile(JsonElement profile) {
		this.profile = profile;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public boolean isServiceResultEngine() {
		return ("resultEngineService").equals(getService());
	}
}
