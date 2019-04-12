package de.ascendro.f4m.server.request.jackpot;

import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.request.RequestInfoImpl;

public class PaymentService extends RequestInfoImpl
{

    private String transactionLogId;
    private Currency currency;

    public String getTransactionLogId() {
        return transactionLogId;
    }

    public void setTransactionLogId(String transactionLogId) {
        this.transactionLogId = transactionLogId;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    @Override
    public String toString()
    {
        return "PaymentService{" +
                "transactionLogId='" + transactionLogId + '\'' +
                ", currency=" + currency + '}';
    }
}
