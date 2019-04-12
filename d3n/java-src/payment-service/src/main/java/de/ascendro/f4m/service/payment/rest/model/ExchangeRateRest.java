package de.ascendro.f4m.service.payment.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes object in Paydent/Expolio API.
 */
@XmlRootElement
public class ExchangeRateRest extends ExchangeRateRestUpdate {
	@JsonProperty("Id")
	private String id;

	@JsonProperty("FromCurrency")
	private CurrencyRest fromCurrency;

	@JsonProperty("ToCurrency")
	private CurrencyRest toCurrency;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public CurrencyRest getFromCurrency() {
		return fromCurrency;
	}

	public void setFromCurrency(CurrencyRest fromCurrency) {
		this.fromCurrency = fromCurrency;
	}

	public CurrencyRest getToCurrency() {
		return toCurrency;
	}

	public void setToCurrency(CurrencyRest toCurrency) {
		this.toCurrency = toCurrency;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ExchangeRateRest [id=");
		builder.append(id);
		builder.append(", fromCurrencyId=");
		builder.append(fromCurrencyId);
		builder.append(", fromCurrency=");
		builder.append(fromCurrency);
		builder.append(", toCurrencyId=");
		builder.append(toCurrencyId);
		builder.append(", toCurrency=");
		builder.append(toCurrency);
		builder.append(", rate=");
		builder.append(rate);
		builder.append(", disabled=");
		builder.append(disabled);
		builder.append("]");
		return builder.toString();
	}
}
