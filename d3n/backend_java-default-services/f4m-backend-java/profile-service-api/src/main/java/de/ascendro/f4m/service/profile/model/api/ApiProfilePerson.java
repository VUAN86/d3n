package de.ascendro.f4m.service.profile.model.api;

import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.profile.model.Sex;
import de.ascendro.f4m.service.util.F4MEnumUtils;

/**
 * Wrapper for wrapper object in a Profile.
 */
public class ApiProfilePerson {

	private String firstName;
	private String lastName;
	private String nickname;
	private String birthDate;
	private String sex;
	
	public ApiProfilePerson() {
		// Initialize empty object
	}
	
	public ApiProfilePerson(ProfileUser person) {
		if (person != null) {
			firstName = person.getFirstName();
			lastName = person.getLastName();
			nickname = person.getNickname();
			birthDate = person.getBirthDateAsString();
			Sex sexEnum = person.getSex();
			sex = sexEnum == null ? null : sexEnum.name();
		}
	}

	public void fillProfile(Profile profile) {
		ProfileUser person = profile.getPersonWrapper();
		if (person == null) {
			person = new ProfileUser();
			profile.setPersonWrapper(person);
		}
		if (firstName != null) {
			person.setFirstName(firstName);
		}
		if (lastName != null) {
			person.setLastName(lastName);
		}
		if (nickname != null) {
			person.setNickname(nickname);
		}
		if (birthDate != null) {
			person.setBirthDate(birthDate);
		}
		if (sex != null) {
			person.setSex(F4MEnumUtils.getEnum(Sex.class, sex));
		}
	}


	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getNickname() {
		return nickname;
	}

	public String getBirthDate() {
		return birthDate;
	}

	public String getSex() {
		return sex;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("ApiProfilePerson [");
		builder.append("firstName=").append(firstName);
		builder.append(", lastName=").append(lastName);
		builder.append(", nickname=").append(nickname);
		builder.append(", birthDate=").append(birthDate);
		builder.append(", sex=").append(sex);
		builder.append("]");
		return builder.toString();
	}

}
