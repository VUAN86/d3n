package de.ascendro.f4m.service.profile.model.sub.update;

import com.google.gson.JsonElement;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UpdateProfileBlobResponse implements JsonMessageContent {

	private String name;

	private JsonElement value;

	public UpdateProfileBlobResponse() {
	}

	public UpdateProfileBlobResponse(String name) {
		this.name = name;
	}

	public UpdateProfileBlobResponse(String name, JsonElement value) {
		this.name = name;
		this.value = value;
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
