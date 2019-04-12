package de.ascendro.f4m.service.profile.model.sub.update;

import com.google.gson.JsonElement;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.UserIdentifier;

public class UpdateProfileBlobRequest implements JsonMessageContent, UserIdentifier {

	private String userId;

	private String name;

	private JsonElement value;

	public UpdateProfileBlobRequest() {
	}

	public UpdateProfileBlobRequest(String name) {
		this.name = name;
	}

	public UpdateProfileBlobRequest(String name, JsonElement value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public JsonElement getValue() {
		return value;
	}

	public void setValue(JsonElement value) {
		this.value = value;
	}

}
