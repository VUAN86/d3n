package de.ascendro.f4m.service.friend.model.api;

import java.math.BigDecimal;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.model.Currency;

public class AppRatedResponse implements JsonMessageContent {

	private BigDecimal amount;
	private Currency currency;

	public AppRatedResponse(BigDecimal amount, Currency currency) {
		this.amount = amount;
		this.currency = currency;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("AppRatedResponse{");
		sb.append("amount=").append(amount);
		sb.append(", currency=").append(currency);
		sb.append('}');
		return sb.toString();
	}
}
