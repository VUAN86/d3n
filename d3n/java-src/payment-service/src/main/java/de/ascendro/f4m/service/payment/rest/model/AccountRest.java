package de.ascendro.f4m.service.payment.rest.model;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes object in Paydent/Expolio API.
 */
@XmlRootElement
public class AccountRest {
	@JsonProperty("Id")
	protected String id;
	@JsonProperty("UserId")
	protected String userId;
	@JsonProperty("CurrencyId")
	protected String currencyId;
	@JsonProperty("Balance")
	protected BigDecimal balance;
	@JsonProperty("State")
	protected AccountState state;
	@JsonProperty("Currency")
	protected CurrencyRest currency;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getCurrencyId() {
		return currencyId;
	}

	public void setCurrencyId(String currencyId) {
		this.currencyId = currencyId;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public AccountState getState() {
		return state;
	}

	public void setState(AccountState state) {
		this.state = state;
	}

	public CurrencyRest getCurrency() {
		return currency;
	}

	public void setCurrency(CurrencyRest currency) {
		this.currency = currency;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AccountRest [id=");
		builder.append(id);
		builder.append(", userId=");
		builder.append(userId);
		builder.append(", currencyId=");
		builder.append(currencyId);
		builder.append(", balance=");
		builder.append(balance);
		builder.append(", state=");
		builder.append(state);
		builder.append(", currency=");
		builder.append(currency);
		builder.append("]");
		return builder.toString();
	}
}
