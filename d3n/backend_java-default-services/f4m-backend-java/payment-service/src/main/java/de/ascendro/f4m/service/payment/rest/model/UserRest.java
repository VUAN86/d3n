package de.ascendro.f4m.service.payment.rest.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes object in Paydent/Expolio API.
 */
@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_NULL) //annotation necessary to serialise only changes - that is, only non-null values
public class UserRest extends UserRestInsert {
	@JsonProperty("Disabled")
	private Boolean disabled;
	@JsonProperty("EMail")
	private String email;
	@JsonProperty("City")
	private String city;
	@JsonProperty("Country")
	private String country;
	@JsonProperty("PlaceOfBirth")
	private String placeOfBirth;
	@JsonProperty("Iban")
	private String iban;
	@JsonProperty("Bic")
	private String bic;
	@JsonProperty("IdentificationId")
	private String identificationId;
	@JsonProperty("IdentityId")
	private String identityId;
	@JsonProperty("Identification")
	private IdentificationRest identification;
	@JsonProperty("Identity")
	private IdentityRest identity; //content type not clear, null in all test data, same as IdentificationRest.Identity
	@JsonProperty("Accounts")
	private List<AccountRest> accounts;
	@JsonProperty("Flags")
	private Integer flags;

	public Boolean getDisabled() {
		return disabled;
	}

	public void setDisabled(Boolean disabled) {
		this.disabled = disabled;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
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

	public String getPlaceOfBirth() {
		return placeOfBirth;
	}

	public void setPlaceOfBirth(String placeOfBirth) {
		this.placeOfBirth = placeOfBirth;
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

	public String getIdentificationId() {
		return identificationId;
	}

	public void setIdentificationId(String identificationId) {
		this.identificationId = identificationId;
	}

	public String getIdentityId() {
		return identityId;
	}

	public void setIdentityId(String identityId) {
		this.identityId = identityId;
	}

	public IdentificationRest getIdentification() {
		return identification;
	}

	public void setIdentification(IdentificationRest identification) {
		this.identification = identification;
	}

	public IdentityRest getIdentity() {
		return identity;
	}

	public void setIdentity(IdentityRest identity) {
		this.identity = identity;
	}

	public List<AccountRest> getAccounts() {
		return accounts;
	}

	public void setAccounts(List<AccountRest> accounts) {
		this.accounts = accounts;
	}

	public Integer getFlags() {
		return flags;
	}

	public void setFlags(Integer flags) {
		this.flags = flags;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserRest [disabled=");
		builder.append(disabled);
		builder.append(", email=");
		builder.append(email);
		builder.append(", name=");
		builder.append(name);
		builder.append(", firstName=");
		builder.append(firstName);
		builder.append(", city=");
		builder.append(city);
		builder.append(", zip=");
		builder.append(zip);
		builder.append(", street=");
		builder.append(street);
		builder.append(", country=");
		builder.append(country);
		builder.append(", placeOfBirth=");
		builder.append(placeOfBirth);
		builder.append(", dateOfBirth=");
		builder.append(dateOfBirth);
		builder.append(", iban=");
		builder.append(iban);
		builder.append(", bic=");
		builder.append(bic);
		builder.append(", identificationId=");
		builder.append(identificationId);
		builder.append(", identityId=");
		builder.append(identityId);
		builder.append(", identification=");
		builder.append(identification);
		builder.append(", identity=");
		builder.append(identity);
		builder.append(", accounts=");
		builder.append(accounts);
		builder.append(", userId=");
		builder.append(userId);
		builder.append(", flags=");
		builder.append(flags);
		builder.append("]");
		return builder.toString();
	}
}
