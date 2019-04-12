package de.ascendro.f4m.service.friend.builder;

import java.util.Arrays;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileIdentifierType;
import de.ascendro.f4m.service.util.JsonLoader;

public class ProfileBuilder {

	private String userId;
	private String[] phones;
	private String[] emails;
	private String[] applications;
	private String[] tenants;

	private final JsonUtil jsonUtil = new JsonUtil();

	public ProfileBuilder(String userId) {
		this.userId = userId;
	}

	public static ProfileBuilder createProfile(String userId) {
		return new ProfileBuilder(userId);
	}
	
	public Profile buildProfile() throws Exception {
		String profileJson = JsonLoader.getTextFromResources("profile.json", this.getClass())
				.replace("\"<<userId>>\"", jsonUtil.toJson(userId));
		Profile profile = new Profile(jsonUtil.fromJson(profileJson, JsonObject.class));
		if (phones != null) {
			Arrays.stream(phones).forEach(p -> profile.setProfileIdentifier(ProfileIdentifierType.PHONE, p));
		}
		if (emails != null) {
			Arrays.stream(emails).forEach(e -> profile.setProfileIdentifier(ProfileIdentifierType.EMAIL, e));
		}
		if (applications != null) {
			Arrays.stream(applications).forEach(a -> profile.addApplication(a));
		}
		if (tenants != null) {
			Arrays.stream(tenants).forEach(t -> profile.addTenant(t));
		}
		return profile;
	}

	public ProfileBuilder withTenants(String... tenants) {
		this.tenants = tenants;
		return this;
	}
	
	public ProfileBuilder withUserId(String userId) {
		this.userId = userId;
		return this;
	}

	public ProfileBuilder withPhones(String... phones) {
		this.phones = phones;
		return this;
	}

	public ProfileBuilder withEmails(String... emails) {
		this.emails = emails;
		return this;
	}

	public ProfileBuilder withApplications(String... applications) {
		this.applications = applications;
		return this;
	}

}
