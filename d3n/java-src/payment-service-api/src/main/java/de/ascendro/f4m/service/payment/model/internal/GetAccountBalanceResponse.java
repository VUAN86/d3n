package de.ascendro.f4m.service.payment.model.internal;

import java.math.BigDecimal;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.model.Currency;

//consider, if structure should not be similar as in GetUserAccountBalanceResponse.AccountBalance.
public class GetAccountBalanceResponse implements JsonMessageContent {
	private BigDecimal amount;
	private Currency currency;
	private String subcurrency;

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

	public String getSubcurrency() {
		return subcurrency;
	}

	public void setSubcurrency(String subcurrency) {
		if (subcurrency==null){
			this.subcurrency = "";
		} else {
			this.subcurrency = subcurrency;
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetAccountBalanceResponse [amount=");
		builder.append(amount);
		builder.append(", currency=");
		builder.append(currency);
		builder.append(", subcurrency=");
		builder.append(subcurrency);
		builder.append("]");
		return builder.toString();
	}
}
