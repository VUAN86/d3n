package de.ascendro.f4m.server.request.jackpot;

import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.SessionWrapper;

import java.math.BigDecimal;

public class PaymentGetJackpotRequestInformation extends RequestInfoImpl
{
    private final String gameId;
    private final String mgiId;
    private final BigDecimal entryFeeAmount;
    private final Currency entryFeeCurrency;
    private JsonMessageContent responseToForward;

    public PaymentGetJackpotRequestInformation(String gameId,
                                               String mgiId,
                                               BigDecimal entryFeeAmount,
                                               Currency entryFeeCurrency,
                                               JsonMessage<?> sourceMessage,
                                               SessionWrapper sourceSession,
                                               JsonMessageContent responseToForward)
    {
        super(sourceMessage, sourceSession);
        this.gameId = gameId;
        this.mgiId = mgiId;
        this.entryFeeAmount = entryFeeAmount;
        this.entryFeeCurrency = entryFeeCurrency;
        this.responseToForward = responseToForward;
    }

    public String getGameId() {
        return gameId;
    }

    public String getMgiId() {
        return mgiId;
    }

    public BigDecimal getEntryFeeAmount() {
        return entryFeeAmount;
    }

    public Currency getEntryFeeCurrency() {
        return entryFeeCurrency;
    }

    public JsonMessageContent getResponseToForward() {
        return responseToForward;
    }

    public void setResponseToForward(JsonMessageContent responseToForward) {
        this.responseToForward = responseToForward;
    }
}


