package de.ascendro.f4m.service.payment.model.external;

import java.math.BigDecimal;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ExchangeRate implements JsonMessageContent {
	private String fromCurrency;
	private String toCurrency;
	private BigDecimal fromAmount;
	private BigDecimal toAmount;

	public ExchangeRate() {
		//
	}
			
	public ExchangeRate(String fromCurrency, String toCurrency, BigDecimal fromAmount, BigDecimal toAmount) {
		this.fromCurrency = fromCurrency;
		this.toCurrency = toCurrency;
		this.fromAmount = fromAmount;
		this.toAmount = toAmount;
	}

	public String getFromCurrency() {
		return fromCurrency;
	}

	public void setFromCurrency(String fromCurrency) {
		this.fromCurrency = fromCurrency;
	}

	public String getToCurrency() {
		return toCurrency;
	}

	public void setToCurrency(String toCurrency) {
		this.toCurrency = toCurrency;
	}

	public BigDecimal getFromAmount() {
		return fromAmount;
	}

	public void setFromAmount(BigDecimal fromAmount) {
		this.fromAmount = fromAmount;
	}

	public BigDecimal getToAmount() {
		return toAmount;
	}

	public void setToAmount(BigDecimal toAmount) {
		this.toAmount = toAmount;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{fromCurrency=").append(fromCurrency);
		builder.append(", toCurrency=").append(toCurrency);
		builder.append(", fromAmount=").append(fromAmount);
		builder.append(", toAmount=").append(toAmount);
		builder.append("}");
		return builder.toString();
	}
}