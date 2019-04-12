package de.ascendro.f4m.service.game.engine.client.payment;

import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.request.RequestInfoImpl;

public class WinSurchargeRequestInfo extends RequestInfoImpl {
    private Currency currency;
    private String transactionLogId;
    private final String gameInstanceId;

    WinSurchargeRequestInfo(String gameInstanceId, Currency currency) {
        this.gameInstanceId = gameInstanceId;
        this.currency = currency;
    }

    public String getTransactionLogId() {
        return transactionLogId;
    }

    void setTransactionLogId(String transactionLogId) {
        this.transactionLogId = transactionLogId;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public String getGameInstanceId() {
        return gameInstanceId;
    }
}
