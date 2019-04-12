package de.ascendro.f4m.service.result.engine.model;

import de.ascendro.f4m.service.profile.model.ProfileAddress;

/**
 * Wrapper for wrapper object in a Profile. 
 */
public class ApiProfileAddress {

	private String country;
	private String city;
	
	public ApiProfileAddress() {
		// Initialize empty object
	}
	
	public ApiProfileAddress(ProfileAddress address) {
		if (address != null) {
			country = address.getCountry();
			city = address.getCity();
		}
	}

	public String getCountry() {
		return country;
	}

	public String getCity() {
		return city;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("country=").append(country);
		builder.append(", city=").append(city);
		builder.append("]");
		return builder.toString();
	}

}
