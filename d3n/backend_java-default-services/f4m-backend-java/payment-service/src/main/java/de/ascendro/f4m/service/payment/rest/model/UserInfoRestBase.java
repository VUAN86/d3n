package de.ascendro.f4m.service.payment.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class UserInfoRestBase {
	@JsonProperty("FirstName")
	protected String firstName;
	@JsonProperty("Name")
	protected String name;
	@JsonProperty("Zip")
	protected String zip;
	@JsonProperty("Street")
	protected String street;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}
}
