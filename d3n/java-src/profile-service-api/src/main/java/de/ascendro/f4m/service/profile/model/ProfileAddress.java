package de.ascendro.f4m.service.profile.model;

import com.google.gson.JsonObject;
import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

/**
 * Wrapper for wrapper object in a Profile. 
 */
public class ProfileAddress extends JsonObjectWrapper {
	public static final String ADDRESS_STREET_PROPERTY = "street";
	public static final String ADDRESS_STREET_NUMBER_PROPERTY = "streetNumber";
	public static final String ADDRESS_CITY_PROPERTY = "city";
	public static final String ADDRESS_POSTAL_CODE_PROPERTY = "postalCode";
	public static final String ADDRESS_COUNTRY_PROPERTY = "country";

	public ProfileAddress() {
		// Initialize empty object
	}
	
	public ProfileAddress(JsonObject profileJsonObject) {
		this.jsonObject = profileJsonObject;
	}

	public void setStreet(String street) {
		setProperty(ADDRESS_STREET_PROPERTY, street);
	}

	public String getStreet() {
		return getPropertyAsString(ADDRESS_STREET_PROPERTY);
	}

	public void setStreetNumber(String streetNumber) {
		setProperty(ADDRESS_STREET_NUMBER_PROPERTY, streetNumber);
	}

	public String getStreetNumber() {
		return getPropertyAsString(ADDRESS_STREET_NUMBER_PROPERTY);
	}

	public void setCity(String city) {
		setProperty(ADDRESS_CITY_PROPERTY, city);
	}

	public String getCity() {
		return getPropertyAsString(ADDRESS_CITY_PROPERTY);
	}

	public void setPostalCode(String postalCode) {
		setProperty(ADDRESS_POSTAL_CODE_PROPERTY, postalCode);
	}

	public String getPostalCode() {
		return getPropertyAsString(ADDRESS_POSTAL_CODE_PROPERTY);
	}

	public void setCountry(String country) {
		setProperty(ADDRESS_COUNTRY_PROPERTY, country);
	}

	public String getCountry() {
		return getPropertyAsString(ADDRESS_COUNTRY_PROPERTY);
	}

}
