package de.ascendro.f4m.service.analytics.client;

import java.math.BigDecimal;

import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.request.RequestInfoImpl;

public abstract class PaymentTransferRequestInfo extends RequestInfoImpl {

    private String transactionLogId;
    private BigDecimal amount;
    private Currency currency;
    private String tenantId;
    private String userId;
    private String appId;

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
