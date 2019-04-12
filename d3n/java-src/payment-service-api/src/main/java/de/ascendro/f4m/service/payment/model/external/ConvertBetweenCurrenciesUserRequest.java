package de.ascendro.f4m.service.payment.model.external;

import java.math.BigDecimal;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.model.Currency;

public class ConvertBetweenCurrenciesUserRequest implements JsonMessageContent {
	private Currency fromCurrency;
	private Currency toCurrency;
	private BigDecimal amount;
	private String description;

	public Currency getFromCurrency() {
		return fromCurrency;
	}

	public void setFromCurrency(Currency fromCurrency) {
		this.fromCurrency = fromCurrency;
	}

	public Currency getToCurrency() {
		return toCurrency;
	}

	public void setToCurrency(Currency toCurrency) {
		this.toCurrency = toCurrency;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ConvertBetweenCurrenciesUserRequest [fromCurrency=");
		builder.append(fromCurrency);
		builder.append(", toCurrency=");
		builder.append(toCurrency);
		builder.append(", amount=");
		builder.append(amount);
		builder.append(", description=");
		builder.append(description);
		builder.append("]");
		return builder.toString();
	}
}
