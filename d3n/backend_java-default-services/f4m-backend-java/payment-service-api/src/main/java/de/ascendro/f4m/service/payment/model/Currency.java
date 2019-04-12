package de.ascendro.f4m.service.payment.model;

import java.util.Objects;

/**
 * Enumeration representing currencies defined in F4M.
 */
public enum Currency {
	MONEY("Money"),
	BONUS("Bonus points"),
	CREDIT("Credits");
	
	private final String fullName;
		
	private Currency(String fullName) {
		this.fullName = fullName;
	}

	public String getFullName() {
		return fullName;
	}

	public static boolean equals(Currency currency, String currencyString) {
		try {
			if (Objects.nonNull(currency) && Objects.nonNull(currencyString)) {
				return Currency.valueOf(currencyString) == currency;
			}
			return false;
		} catch (IllegalArgumentException e) {
			// if EUR, USD etc.(MONEY)
			if (currency == Currency.MONEY) {
				return true;
			}
			return false;
		}
	}

	public static boolean equalsWithoutRealMoney(Currency currency, String currencyString) {
		try {
			if (Objects.nonNull(currency) && Objects.nonNull(currencyString)) {
				return Currency.valueOf(currencyString) == currency;
			}
			return false;
		} catch (IllegalArgumentException e) {
			// if EUR, USD etc.(MONEY)
			return false;
		}
	}


}
