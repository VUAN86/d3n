package de.ascendro.f4m.service.tombola.client;

import java.math.BigDecimal;

import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.request.RequestInfoImpl;

public class TicketPurchaseRequestInfo extends RequestInfoImpl {

    private String tombolaId;
    private String transactionLogId;
    private int numberOfTicketsBought;
    private BigDecimal price;
    private Currency currency;
    private String bundleImageId;
    private String appId;
    private String tenantId;

    public TicketPurchaseRequestInfo(String tombolaId) {
        this.tombolaId = tombolaId;
    }

    public String getTombolaId() {
        return tombolaId;
    }

    public void setTombolaId(String tombolaId) {
        this.tombolaId = tombolaId;
    }

    public String getTransactionLogId() {
        return transactionLogId;
    }

    public void setTransactionLogId(String transactionLogId) {
        this.transactionLogId = transactionLogId;
    }

    public int getNumberOfTicketsBought() {
        return numberOfTicketsBought;
    }

    public void setNumberOfTicketsBought(int numberOfTicketsBought) {
        this.numberOfTicketsBought = numberOfTicketsBought;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getBundleImageId() {
        return bundleImageId;
    }

    public void setBundleImageId(String bundleImageId) {
        this.bundleImageId = bundleImageId;
    }
}
