package de.ascendro.f4m.server.analytics.model;


import java.math.BigDecimal;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.model.base.GameBaseEvent;

public class PaymentEvent extends GameBaseEvent {

    public static final String CREDIT_PAID_PROPERTY = "creditPaid";
    public static final String MONEY_PAID_PROPERTY = "moneyPaid";
    public static final String BONUS_POINTS_PROPERTY = "bonusPointsPaid";
    public static final String CREDIT_PURCHASED_PROPERTY = "creditPurchased";
    public static final String MONEY_CHARGED_PROPERTY = "moneyCharge";
    public static final String CREDIT_GIVEN_PROPERTY = "creditGiven";
    public static final String MONEY_GIVEN_PROPERTY = "moneyGiven";
    public static final String BONUS_POINTS_GIVEN_PROPERTY = "bonusPointsGiven";
    public static final String PAYMENT_DETAIL_PROPERTY = "paymentDetail";
    //Received payment from tenant
    public static final String PAYMENT_RECEIVED = "paymentReceived";
    public static final String PAYMENT_AMOUNT = "paymentAmount";

    public PaymentEvent() {
        //default constructor
    }

    public PaymentEvent(JsonObject paymentJsonObject) {
        super(paymentJsonObject);
    }

    public void setCreditPaid(Long creditPaid) {
        setProperty(CREDIT_PAID_PROPERTY, creditPaid);
    }

    public Long getCreditPaid() {
        return getPropertyAsLong(CREDIT_PAID_PROPERTY);
    }

    public void setMoneyPaid(BigDecimal moneyPaid) {
        setProperty(MONEY_PAID_PROPERTY, moneyPaid);
    }

    public BigDecimal getMoneyPaid() {
        return getPropertyAsBigDecimal(MONEY_PAID_PROPERTY);
    }

    public void setBonusPointsPaid(Long bonusPointsPaid) {
        setProperty(BONUS_POINTS_PROPERTY, bonusPointsPaid);
    }

    public Long getBonusPointsPaid() {
        return getPropertyAsLong(BONUS_POINTS_PROPERTY);
    }

    public void setCreditPurchased(Long creditPurchased) {
        setProperty(CREDIT_PURCHASED_PROPERTY, creditPurchased);
    }

    public Long getCreditPurchased() {
        return getPropertyAsLong(CREDIT_PURCHASED_PROPERTY);
    }

    public void setMoneyCharged(BigDecimal moneyCharged) {
        setProperty(MONEY_CHARGED_PROPERTY, moneyCharged);
    }

    public BigDecimal getMoneyCharged() {
        return getPropertyAsBigDecimal(MONEY_CHARGED_PROPERTY);
    }

    public void setCreditGiven(Long creditGiven) {
        setProperty(CREDIT_GIVEN_PROPERTY, creditGiven);
    }

    public Long getCreditGiven() {
        return getPropertyAsLong(CREDIT_GIVEN_PROPERTY);
    }

    public void setMoneyGiven(BigDecimal moneyGiven) {
        setProperty(MONEY_GIVEN_PROPERTY, moneyGiven);
    }

    public BigDecimal getMoneyGiven() {
        return getPropertyAsBigDecimal(MONEY_GIVEN_PROPERTY);
    }

    public void setBonusPointsGiven(Long bonusPointsGiven) {
        setProperty(BONUS_POINTS_GIVEN_PROPERTY, bonusPointsGiven);
    }

    public Long getBonusPointsGiven() {
        return getPropertyAsLong(BONUS_POINTS_GIVEN_PROPERTY);
    }

    public void setPaymentDetailsJSON(String paymentDetailsJSON) {
        setProperty(PAYMENT_DETAIL_PROPERTY, paymentDetailsJSON);
    }

    public String getPaymentDetailsJSON() {
        return getPropertyAsString(PAYMENT_DETAIL_PROPERTY);
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        setProperty(PAYMENT_AMOUNT, paymentAmount);
    }

    public BigDecimal getPaymentAmount() {
        return getPropertyAsBigDecimal(PAYMENT_AMOUNT);
    }

    public void setPaymentReceived(Boolean paymentReceived) {
        setProperty(PAYMENT_RECEIVED, paymentReceived);
    }

    public Boolean isPaymentReceived() {
        return getPropertyAsBoolean(PAYMENT_RECEIVED);
    }

    @Override
	protected boolean isUserInfoRequired() {
		return false;
	}

}
