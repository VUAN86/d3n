package de.ascendro.f4m.service.payment.rest.model;

import java.math.BigDecimal;

public class AccountRestBuilder {
	private String id;
	private String userId;
	private String currencyId;
	private BigDecimal balance;
	private AccountState state;
	private CurrencyRest currency;

	public AccountRestBuilder id(String id) {
		this.id = id;
		return this;
	}

	public AccountRestBuilder userId(String userId) {
		this.userId = userId;
		return this;
	}

	public AccountRestBuilder currencyId(String currencyId) {
		this.currencyId = currencyId;
		return this;
	}

	public AccountRestBuilder balance(BigDecimal balance) {
		this.balance = balance;
		return this;
	}

	public AccountRestBuilder state(AccountState state) {
		this.state = state;
		return this;
	}

	public AccountRestBuilder currency(CurrencyRest currency) {
		this.currency = currency;
		return this;
	}

	public AccountRest build() {
		AccountRest accountRest = new AccountRest();
		accountRest.id = id;
		accountRest.userId = userId;
		accountRest.currencyId = currencyId;
		accountRest.balance = balance;
		accountRest.state = state;
		accountRest.currency = currency;
		return accountRest;
	}
}
