package de.ascendro.f4m.service.tombola.client;

import java.math.BigDecimal;

import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.tombola.model.Tombola;
import de.ascendro.f4m.service.tombola.model.TombolaWinner;

public class PrizePayoutRequestInfo extends RequestInfoImpl {

    private Tombola tombola;
    private String transactionLogId;
    private BigDecimal amount;
    private Currency currency;
    private String tenantId;
    private String userId;
    private String appId;
    private String voucherId;
    private TombolaWinner tombolaWinner;

    public PrizePayoutRequestInfo(Tombola tombola, TombolaWinner tombolaWinner) {
        this.tombola = tombola;
        this.tombolaWinner = tombolaWinner;
    }

    public Tombola getTombola() {
        return tombola;
    }

    public String getTombolaId() {
        return tombola == null ? null : tombola.getId();
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

    public String getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(String voucherId) {
        this.voucherId = voucherId;
    }
    
    public TombolaWinner getTombolaWinner() {
    	return tombolaWinner;
    }
    
}
