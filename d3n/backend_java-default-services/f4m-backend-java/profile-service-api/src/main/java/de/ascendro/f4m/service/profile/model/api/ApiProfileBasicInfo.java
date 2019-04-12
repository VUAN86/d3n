package de.ascendro.f4m.service.profile.model.api;

import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.profile.model.Sex;

public class ApiProfileBasicInfo {

	private String userId;
	private String firstName;
	private String lastName;
	private String nickname;
	private Sex sex;
	private String country;
	private String city;

	public ApiProfileBasicInfo() {
		// initialize empty object
	}
	
	public ApiProfileBasicInfo(String userId) {
		this.userId = userId;
	}

	public ApiProfileBasicInfo(Profile profile) {
		if (profile != null) {
			this.userId = profile.getUserId();

			ProfileUser person = profile.getPersonWrapper();
			if (person != null) {
				if (profile.isShowFullName()) {
					this.firstName = person.getFirstName();
					this.lastName = person.getLastName();
				}
				this.nickname = person.getNickname();
				this.sex = person.getSex();
			}

			ProfileAddress address = profile.getAddress();
			if (address != null) {
				this.country = address.getCountry();
				this.city = address.getCity();
			}
		}
	}
	
	public String getName() {
		String displayName = StringUtils.joinWith(StringUtils.SPACE, firstName, lastName).trim();
		if (StringUtils.isEmpty(displayName) && nickname != null) {
			displayName = nickname;
		}
		return displayName;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public Sex getSex() {
		return sex;
	}

	public void setSex(Sex sex) {
		this.sex = sex;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ApiProfileBasicInfo [userId=");
		builder.append(userId);
		builder.append(", firstName=");
		builder.append(firstName);
		builder.append(", lastName=");
		builder.append(lastName);
		builder.append(", nickname=");
		builder.append(nickname);
		builder.append(", sex=");
		builder.append(sex);
		builder.append(", country=");
		builder.append(country);
		builder.append(", city=");
		builder.append(city);
		builder.append("]");
		return builder.toString();
	}

}
