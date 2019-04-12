package de.ascendro.f4m.service.advertisement.client;

import java.math.BigDecimal;

import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.request.RequestInfoImpl;

public class RewardPayoutRequestInfo extends RequestInfoImpl {

    private String transactionLogId;
    private BigDecimal amount;
    private Currency currency;
    private String tenantId;
    private String userId;
    private String appId;
    private String imageId;
    private String fyberTransactionId;

    public RewardPayoutRequestInfo() {
        //default constructor
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getFyberTransactionId() {
        return fyberTransactionId;
    }

    public void setFyberTransactionId(String fyberTransactionId) {
        this.fyberTransactionId = fyberTransactionId;
    }

    public String getTransactionLogId() {
        return transactionLogId;
    }

    public void setTransactionLogId(String transactionLogId) {
        this.transactionLogId = transactionLogId;
    }

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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

}
