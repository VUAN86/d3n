package de.ascendro.f4m.service.profile.model.api;

import de.ascendro.f4m.service.profile.model.Profile;

public class ApiUpdateableProfile {

	private ApiProfileSettings settings;
	private ApiProfilePerson person;
	private ApiProfileAddress address;
	
	public ApiUpdateableProfile() {
		// Initialize empty object
		settings = new ApiProfileSettings();
		person = new ApiProfilePerson();
		address = new ApiProfileAddress();
	}
	
	public ApiUpdateableProfile(Profile profile) {
		settings = new ApiProfileSettings(profile);
		person = new ApiProfilePerson(profile.getPersonWrapper());
		address = new ApiProfileAddress(profile);
	}
	
	public Profile toProfile() {
		Profile profile = new Profile();
		if (person != null) {
			person.fillProfile(profile);
		}
		if (address != null) {
			address.fillProfile(profile);
		}
		if (settings != null) {
			settings.fillProfile(profile);
		}
		return profile;
	}
	
	public ApiUpdateableProfile(ApiProfilePerson person, ApiProfileAddress address, ApiProfileSettings settings) {
		this.person = person;
		this.address = address;
		this.settings = settings;
	}

	public ApiProfileSettings getSettings() {
		return settings;
	}

	public ApiProfilePerson getPerson() {
		return person;
	}

	public ApiProfileAddress getAddress() {
		return address;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("ApiUpdateableProfile [");
		builder.append(", settings=").append(settings);
		builder.append(", person=").append(person);
		builder.append(", address=").append(address);
		builder.append("]");
		return builder.toString();
	}
	
}
