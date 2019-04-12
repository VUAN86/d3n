package de.ascendro.f4m.service.profile.model;

import java.time.ZonedDateTime;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.F4MEnumUtils;

/**
 * Wrapper for wrapper object in a Profile. 
 */
public class ProfileUser extends JsonObjectWrapper {
	public static final String PERSON_FIRST_NAME_PROPERTY = "firstName";
	public static final String PERSON_LAST_NAME_PROPERTY = "lastName";
	public static final String NICKNAME_PROPERTY = "nickname";
	public static final String BIRTH_DATE_PROPERTY = "birthDate";
	public static final String SEX_PROPERTY = "sex";
	
	public ProfileUser() {
		// Initialize empty object
	}
	
	public ProfileUser(JsonObject profileJsonObject) {
		this.jsonObject = profileJsonObject;
	}

	public String getFirstName() {
		return getPropertyAsString(PERSON_FIRST_NAME_PROPERTY);
	}
	
	public void setFirstName(String firstName) {
		setProperty(PERSON_FIRST_NAME_PROPERTY, firstName);
	}
	
	public String getLastName() {
		return getPropertyAsString(PERSON_LAST_NAME_PROPERTY);
	}
	
	public void setLastName(String lastName) {
		setProperty(PERSON_LAST_NAME_PROPERTY, lastName);
	}
	
	public String getNickname() {
		return getPropertyAsString(NICKNAME_PROPERTY);
	}

	public void setNickname(String nickname) {
		setProperty(NICKNAME_PROPERTY, nickname);
	}
	
	public ZonedDateTime getBirthDate() {
		return getPropertyAsZonedDateTime(BIRTH_DATE_PROPERTY);
	}
	
	public void setBirthDate(ZonedDateTime birthDate) {
		setProperty(BIRTH_DATE_PROPERTY, birthDate);
	}

	public String getBirthDateAsString() {
		return getPropertyAsString(BIRTH_DATE_PROPERTY);
	}
	
	public void setBirthDate(String birthDate) {
		setProperty(BIRTH_DATE_PROPERTY, DateTimeUtil.parseISODateTimeString(birthDate));
	}
	
	public Sex getSex() {
		return F4MEnumUtils.getEnum(Sex.class, getPropertyAsString(SEX_PROPERTY));
	}

	public void setSex(Sex sex) {
		setProperty(SEX_PROPERTY, sex == null ? null : sex.name());
	}
	
}
