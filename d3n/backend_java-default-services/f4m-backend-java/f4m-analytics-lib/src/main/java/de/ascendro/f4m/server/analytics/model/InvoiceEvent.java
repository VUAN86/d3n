package de.ascendro.f4m.server.analytics.model;


import java.math.BigDecimal;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.model.base.BaseEvent;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.payment.model.Currency;

public class InvoiceEvent extends BaseEvent {
    public static final String PAYMENT_TYPE = "paymentType";
    public static final String GAME_TYPE = "gameType";
    public static final String PAYMENT_AMOUNT = "paymentAmount";
    public static final String CURRENCY = "currency";
    public static final String CURRENCY_TO = "currencyTo";
    public static final String EXCHANGE_RATE = "exchangeRate";
    public static final String PAYMENT_AMOUNT_TO = "paymentAmountTo";

    public InvoiceEvent() {
        //default constructor
    }

    public InvoiceEvent(JsonObject inviteJsonObject) {
        super(inviteJsonObject);
    }

    public void setPaymentType(PaymentType paymentType) {
        setPaymentType(paymentType.name());
    }

    public void setPaymentType(String paymentType) {
        setProperty(PAYMENT_TYPE, paymentType);
    }

    public String getPaymentType() {
        return getPropertyAsString(PAYMENT_TYPE);
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        setProperty(PAYMENT_AMOUNT, paymentAmount);
    }

    public BigDecimal getPaymentAmount() {
        return getPropertyAsBigDecimal(PAYMENT_AMOUNT);
    }

    public void setPaymentAmountTo(BigDecimal paymentAmountTo) {
        setProperty(PAYMENT_AMOUNT_TO, paymentAmountTo);
    }

    public BigDecimal getPaymentAmountTo() {
        return getPropertyAsBigDecimal(PAYMENT_AMOUNT_TO);
    }

    public void setGameType(GameType gameType){
        if (gameType!=null) {
            setGameType(gameType.name());
        }
    }

    public void setGameType(String gameType) {
        setProperty(GAME_TYPE, gameType);
    }

    public String getGameType() {
        return getPropertyAsString(GAME_TYPE);
    }

    public String getCurrency() {
        return getPropertyAsString(CURRENCY);
    }

    public void setCurrency(String currency) {
        setProperty(CURRENCY, currency);
    }

    public void setCurrency(Currency currency) {
        if (currency!=null) {
            setCurrency(currency.name());
        }
    }

    public String getCurrencyTo() {
        return getPropertyAsString(CURRENCY_TO);
    }

    public void setCurrencyTo(String currencyTo) {
        setProperty(CURRENCY_TO, currencyTo);
    }

    public void setCurrencyTo(Currency currencyTo) {
        if (currencyTo!=null) {
            setCurrencyTo(currencyTo.name());
        }
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        setProperty(EXCHANGE_RATE, exchangeRate);
    }

    public BigDecimal getExchangeRate() {
        return getPropertyAsBigDecimal(EXCHANGE_RATE);
    }

    @Override
    public boolean isUserIdRequired() {
        return true;
    }
    @Override
    public boolean isTenantIdRequired() {
        return true;
    }

    @Override
    public boolean isAppIdRequired() {
        return false;
    }

    @Override
    public boolean isSessionIpRequired() {
        return false;
    }

    public enum PaymentType {
        ENTRY_FEE("Entry Fee"),
        VOUCHER("Voucher"),
        JOKER("Joker"),
        WINNING("Winning Component"),
        CREDIT_SALES("Credit Sales"),
        TOMBOLA("Tombola");

        private final String fullName;

        PaymentType(String fullName) {
            this.fullName = fullName;
        }

        public String getFullName() {
            return fullName;
        }
    }
}
