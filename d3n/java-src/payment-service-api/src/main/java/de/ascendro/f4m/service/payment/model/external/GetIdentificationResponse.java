package de.ascendro.f4m.service.payment.model.external;

import java.time.ZonedDateTime;

import com.google.gson.annotations.JsonAdapter;

import de.ascendro.f4m.service.json.GsonSimpleDateAdapter;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.model.internal.IdentificationType;

public class GetIdentificationResponse implements JsonMessageContent {

	private String id;
	private String firstName;
	private String name;
	private String street;
	private String zip;
	private String city;
	private String country;
	@JsonAdapter(value=GsonSimpleDateAdapter.class)
	private ZonedDateTime dateOfBirth;
	private String placeOfBirth;
	private IdentificationType type;

    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public ZonedDateTime getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(ZonedDateTime dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public String getPlaceOfBirth() {
		return placeOfBirth;
	}

	public void setPlaceOfBirth(String placeOfBirth) {
		this.placeOfBirth = placeOfBirth;
	}

	public IdentificationType getType() {
		return type;
	}

	public void setType(IdentificationType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetIdentificationResponse [id=").append(id);
		builder.append(", firstName=").append(firstName);
		builder.append(", name=").append(name);
		builder.append(", street=").append(street);
		builder.append(", zip=").append(zip);
		builder.append(", city=").append(city);
		builder.append(", country=").append(country);
		builder.append(", dateOfBirth=").append(dateOfBirth);
		builder.append(", placeOfBirth=").append(placeOfBirth);
		builder.append(", type=").append(type);
		builder.append("]");
		return builder.toString();
	}

}
