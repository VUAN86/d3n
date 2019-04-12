package de.ascendro.f4m.service.tombola.model;


import de.ascendro.f4m.service.payment.model.Currency;

public enum PrizeType {
    MONEY,
    CREDIT,
    BONUS,
    VOUCHER;

    public Currency toCurrency() {
        Currency result = null;
        if (this == MONEY) {
            result = Currency.MONEY;
        } else if (this == CREDIT) {
            result = Currency.CREDIT;
        } else if (this == BONUS) {
            result = Currency.BONUS;
        }
        return result;
    }

    public static PrizeType fromCurrency(Currency currency) {
        PrizeType result = null;
        if (currency == Currency.MONEY) {
            result = MONEY;
        } else if (currency == Currency.CREDIT) {
            result = CREDIT;
        } else if (currency == Currency.BONUS) {
            result = BONUS;
        }
        return result;
    }
}
