package de.ascendro.f4m.service.payment.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes object in Paydent/Expolio API.
 */
@XmlRootElement
public class CurrencyRest extends CurrencyRestInsert {
	@JsonProperty("Id")
	private String id;
	@JsonProperty("IsInternal")
	private Boolean isInternal;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Boolean getIsInternal() {
		return isInternal;
	}

	public void setIsInternal(Boolean isInternal) {
		this.isInternal = isInternal;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CurrencyRest [id=");
		builder.append(id);
		builder.append(", name=");
		builder.append(name);
		builder.append(", shortName=");
		builder.append(shortName);
		builder.append(", isInternal=");
		builder.append(isInternal);
		builder.append("]");
		return builder.toString();
	}
}
