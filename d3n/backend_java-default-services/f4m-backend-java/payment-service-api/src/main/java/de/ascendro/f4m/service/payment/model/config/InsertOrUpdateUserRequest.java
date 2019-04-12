package de.ascendro.f4m.service.payment.model.config;

import com.google.gson.JsonElement;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class InsertOrUpdateUserRequest implements JsonMessageContent {
	protected String tenantId;
	protected String profileId;
	protected boolean insertNewUser;
	protected JsonElement profile;

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	public boolean isInsertNewUser() {
		return insertNewUser;
	}

	public void setInsertNewUser(boolean insertNewUser) {
		this.insertNewUser = insertNewUser;
	}

	public JsonElement getProfile() {
		return profile;
	}

	public void setProfile(JsonElement profile) {
		this.profile = profile;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InsertOrUpdateUserRequest [tenantId=");
		builder.append(tenantId);
		builder.append(", profileId=");
		builder.append(profileId);
		builder.append(", profile=");
		builder.append(profile);
		builder.append("]");
		return builder.toString();
	}
}
