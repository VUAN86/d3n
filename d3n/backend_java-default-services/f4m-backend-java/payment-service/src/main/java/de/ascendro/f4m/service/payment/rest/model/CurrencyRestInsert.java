package de.ascendro.f4m.service.payment.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes object in Paydent/Expolio API.
 */
@XmlRootElement
public class CurrencyRestInsert {
	@JsonProperty("Name")
	protected String name;
	@JsonProperty("ShortName")
	protected String shortName;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RestCurrencyInsert [name=");
		builder.append(name);
		builder.append(", shortName=");
		builder.append(shortName);
		builder.append("]");
		return builder.toString();
	}
}
