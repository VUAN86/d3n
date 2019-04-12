package de.ascendro.f4m.service.payment.model.config;

import com.google.gson.JsonElement;

public class InsertOrUpdateUserRequestBuilder {
	private String tenantId;
	private String profileId;
	private boolean insertNewUser;
	private JsonElement profile;

	public InsertOrUpdateUserRequestBuilder tenantId(String tenantId) {
		this.tenantId = tenantId;
		return this;
	}

	public InsertOrUpdateUserRequestBuilder profileId(String profileId) {
		this.profileId = profileId;
		return this;
	}

	public InsertOrUpdateUserRequestBuilder insertNewUser(boolean insertNewUser) {
		this.insertNewUser = insertNewUser;
		return this;
	}

	public InsertOrUpdateUserRequestBuilder profile(JsonElement profile) {
		this.profile = profile;
		return this;
	}

	public InsertOrUpdateUserRequest build() {
		InsertOrUpdateUserRequest insertOrUpdateUserRequest = new InsertOrUpdateUserRequest();
		insertOrUpdateUserRequest.tenantId = tenantId;
		insertOrUpdateUserRequest.profileId = profileId;
		insertOrUpdateUserRequest.insertNewUser = insertNewUser;
		insertOrUpdateUserRequest.profile = profile;
		return insertOrUpdateUserRequest;
	}
}
