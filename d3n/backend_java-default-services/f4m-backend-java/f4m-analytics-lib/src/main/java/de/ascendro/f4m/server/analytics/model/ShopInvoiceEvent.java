package de.ascendro.f4m.server.analytics.model;

import java.math.BigDecimal;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.model.base.BaseEvent;

public class ShopInvoiceEvent extends BaseEvent {
    public static final String ARTICLE_ID = "articleId";
    public static final String ARTICLE_NUMBER = "articleNumber";
    public static final String ARTICLE_TITLE = "articleTitle";
    public static final String ORIGNAL_PURCHASE_COST = "originalPurchaseCost";
    public static final String PAYMENT_MONEY_AMOUNT = "paymentMoneyAmount";
    public static final String PAYMENT_BONUS_AMOUNT = "paymentBonusAmount";
    public static final String PAYMENT_CREDIT_AMOUNT = "paymentCreditAmount";

    public ShopInvoiceEvent() {
        //default constructor
    }

    public ShopInvoiceEvent(JsonObject inviteJsonObject) {
        super(inviteJsonObject);
    }

    public void setArticleId(String articleId) {
        setProperty(ARTICLE_ID, articleId);
    }

    public String getArticleId() {
        return getPropertyAsString(ARTICLE_ID);
    }

    public void setArticleNumber(String articleNumber) {
        setProperty(ARTICLE_NUMBER, articleNumber);
    }

    public String getArticleNumber() {
        return getPropertyAsString(ARTICLE_NUMBER);
    }

    public void setArticleTitle(String articleTitle) {
        setProperty(ARTICLE_TITLE, articleTitle);
    }

    public String getArticleTitle() {
        return getPropertyAsString(ARTICLE_TITLE);
    }

    public void setOriginalPurchaseCost(BigDecimal originalPurchaseCost) {
        setProperty(ORIGNAL_PURCHASE_COST, originalPurchaseCost);
    }

    public BigDecimal getOriginalPurchaseCost() {
        return getPropertyAsBigDecimal(ORIGNAL_PURCHASE_COST);
    }

    public void setPaymentMoneyAmount(BigDecimal paymentMoneyAmount) {
        setProperty(PAYMENT_MONEY_AMOUNT, paymentMoneyAmount);
    }

    public BigDecimal getPaymentMoneyAmount() {
        return getPropertyAsBigDecimal(PAYMENT_MONEY_AMOUNT);
    }

    public void setPaymentBonusAmount(BigDecimal paymentBonusAmount) {
        setProperty(PAYMENT_BONUS_AMOUNT, paymentBonusAmount);
    }

    public BigDecimal getPaymentBonusAmount() {
        return getPropertyAsBigDecimal(PAYMENT_BONUS_AMOUNT);
    }

    public void setPaymentCreditAmount(BigDecimal paymentCreditAmount) {
        setProperty(PAYMENT_CREDIT_AMOUNT, paymentCreditAmount);
    }

    public BigDecimal getPaymentCreditAmount() {
        return getPropertyAsBigDecimal(PAYMENT_CREDIT_AMOUNT);
    }

}
