package de.ascendro.f4m.service.payment.rest.model;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes object in Paydent/Expolio API for insert
 */
@XmlRootElement
public class ExchangeRateRestInsert {
	@JsonProperty("FromCurrencyId")
	protected String fromCurrencyId;
	
	@JsonProperty("ToCurrencyId")
	protected String toCurrencyId;
	
	@JsonProperty("Rate")
	protected BigDecimal rate;

	public String getFromCurrencyId() {
		return fromCurrencyId;
	}

	public void setFromCurrencyId(String fromCurrencyId) {
		this.fromCurrencyId = fromCurrencyId;
	}

	public String getToCurrencyId() {
		return toCurrencyId;
	}

	public void setToCurrencyId(String toCurrencyId) {
		this.toCurrencyId = toCurrencyId;
	}

	public BigDecimal getRate() {
		return rate;
	}

	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ExchangeRateRestInsert [fromCurrencyId=");
		builder.append(fromCurrencyId);
		builder.append(", toCurrencyId=");
		builder.append(toCurrencyId);
		builder.append(", rate=");
		builder.append(rate);
		builder.append("]");
		return builder.toString();
	}
}
