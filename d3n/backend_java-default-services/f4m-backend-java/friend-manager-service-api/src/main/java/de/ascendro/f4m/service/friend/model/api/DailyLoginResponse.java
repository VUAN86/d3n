package de.ascendro.f4m.service.friend.model.api;

import java.math.BigDecimal;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.model.Currency;

public class DailyLoginResponse implements JsonMessageContent {

	private BigDecimal amount;
	private Currency currency;
	private boolean alreadyLoggedInToday;

	public DailyLoginResponse(BigDecimal amount, Currency currency, boolean alreadyLoggedInToday) {
		this.amount = amount;
		this.currency = currency;
		this.alreadyLoggedInToday = alreadyLoggedInToday;
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

	public boolean isAlreadyLoggedInToday() {
		return alreadyLoggedInToday;
	}

	public void setAlreadyLoggedInToday(boolean alreadyLoggedInToday) {
		this.alreadyLoggedInToday = alreadyLoggedInToday;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("DailyLoginResponse{");
		sb.append("amount=").append(amount);
		sb.append(", currency=").append(currency);
		sb.append(", alreadyLoggedInToday=").append(alreadyLoggedInToday);
		sb.append('}');
		return sb.toString();
	}
}
