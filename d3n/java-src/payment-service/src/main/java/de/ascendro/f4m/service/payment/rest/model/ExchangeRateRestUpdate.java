package de.ascendro.f4m.service.payment.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes object in Paydent/Expolio API for update
 */
@XmlRootElement
public class ExchangeRateRestUpdate extends ExchangeRateRestInsert {
	@JsonProperty("Disabled")
	protected boolean disabled;
	
	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ExchangeRateRestUpdate [disabled=");
		builder.append(disabled);
		builder.append(", fromCurrencyId=");
		builder.append(fromCurrencyId);
		builder.append(", toCurrencyId=");
		builder.append(toCurrencyId);
		builder.append(", rate=");
		builder.append(rate);
		builder.append("]");
		return builder.toString();
	}
}
