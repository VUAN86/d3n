package de.ascendro.f4m.service.profile.model.api;

import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;

/**
 * Wrapper for wrapper object in a Profile. 
 */
public class ApiProfileAddress {

	private String country;
	private String city;
	private String postalCode;
	private String street;
	
	public ApiProfileAddress() {
		// Initialize empty object
	}
	
	public ApiProfileAddress(Profile profile) {
		ProfileAddress address = profile.getAddress();
		if (address != null) {
			country = address.getCountry();
			city = address.getCity();
			postalCode = address.getPostalCode();
			street = address.getStreet();
		}
	}

	public void fillProfile(Profile profile) {
		ProfileAddress address = profile.getAddress();
		if (address == null) {
			address = new ProfileAddress();
			profile.setAddress(address);
		}
		if (country != null) {
			address.setCountry(country);
		}
		if (city != null) {
			address.setCity(city);
		}
		if (postalCode != null) {
			address.setPostalCode(postalCode);
		}
		if (street != null) {
			address.setStreet(street);
		}
	}

	public String getCountry() {
		return country;
	}

	public String getCity() {
		return city;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public String getStreet() {
		return street;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("ApiProfileAddress [");
		builder.append("country=").append(country);
		builder.append(", city=").append(city);
		builder.append(", postalCode=").append(postalCode);
		builder.append(", street=").append(street);
		builder.append("]");
		return builder.toString();
	}

}
