package de.ascendro.f4m.service.payment.rest.model;

import java.time.ZonedDateTime;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.ascendro.f4m.service.payment.json.JacksonDateDeserializer;
import de.ascendro.f4m.service.payment.json.JacksonDateSerializer;
import de.ascendro.f4m.service.payment.json.JsonSerializerParameter;
import de.ascendro.f4m.service.payment.model.internal.IdentificationType;
import de.ascendro.f4m.service.util.DateTimeUtil;

@XmlRootElement
public class IdentityRest extends UserInfoRestBase {
	@JsonProperty("Id")
	private String id;
	@JsonProperty("City")
	private String city;
	@JsonProperty("Country")
	private String country;
	@JsonProperty("DateOfBirth")
	@JsonDeserialize(using = JacksonDateDeserializer.class)
	@JsonSerialize(using = JacksonDateSerializer.class)
	@JsonSerializerParameter(format = DateTimeUtil.JSON_DATE_FORMAT)
	private ZonedDateTime dateOfBirth; //almost identical to UserRestInsert.dateOfBirth expect for format. Move to UserInfoRestBase, if the attribute same can be used.
	@JsonProperty("PlaceOfBirth")
	private String placeOfBirth;
	@JsonProperty("Type")
	private IdentificationType type;
	@JsonProperty("IBAN")
	private String iban;
	@JsonProperty("BIC")
	private String bic;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public String getIban() {
		return iban;
	}

	public void setIban(String iban) {
		this.iban = iban;
	}

	public String getBic() {
		return bic;
	}

	public void setBic(String bic) {
		this.bic = bic;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("IdentityRest [id=");
		builder.append(id);
		builder.append(", city=");
		builder.append(city);
		builder.append(", country=");
		builder.append(country);
		builder.append(", dateOfBirth=");
		builder.append(dateOfBirth);
		builder.append(", placeOfBirth=");
		builder.append(placeOfBirth);
		builder.append(", type=");
		builder.append(type);
		builder.append(", iban=");
		builder.append(iban);
		builder.append(", bic=");
		builder.append(bic);
		builder.append(", firstName=");
		builder.append(firstName);
		builder.append(", name=");
		builder.append(name);
		builder.append(", zip=");
		builder.append(zip);
		builder.append(", street=");
		builder.append(street);
		builder.append("]");
		return builder.toString();
	}
}
