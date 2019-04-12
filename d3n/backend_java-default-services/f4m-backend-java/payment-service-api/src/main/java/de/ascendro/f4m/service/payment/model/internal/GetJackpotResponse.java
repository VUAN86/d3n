package de.ascendro.f4m.service.payment.model.internal;

import java.math.BigDecimal;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.model.Currency;

public class GetJackpotResponse implements JsonMessageContent {

	private BigDecimal balance;
	private GameState state;
	private Currency currency;

	public GetJackpotResponse() {
	}

	public GetJackpotResponse balalance(BigDecimal balance) {
		this.balance = balance;
		return this;
	}

	public GetJackpotResponse currency(Currency currency) {
		this.currency = currency;
		return this;
	}
	public GetJackpotResponse state(GameState state) {
		this.state = state;
		return this;
	}


	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public GameState getState() {
		return state;
	}

	public void setState(GameState state) {
		this.state = state;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetJackpotResponse [balance=");
		builder.append(balance);
		builder.append(", state=");
		builder.append(state);
		builder.append(", currency=");
		builder.append(currency);
		builder.append("]");
		return builder.toString();
	}

}
