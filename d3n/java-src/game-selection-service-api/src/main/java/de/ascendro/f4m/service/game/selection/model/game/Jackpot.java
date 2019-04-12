package de.ascendro.f4m.service.game.selection.model.game;

import java.math.BigDecimal;

import de.ascendro.f4m.service.payment.model.Currency;

public class Jackpot {

	private BigDecimal balance;
	private Currency currency;

	public Jackpot(BigDecimal balance, Currency currency) {
		this.balance = balance;
		this.currency = currency;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
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
		builder.append("Jackpot [balance=");
		builder.append(this.balance.toString());
		builder.append(", currency=").append(currency.toString());
		builder.append("]");
		return builder.toString();
	}
}
