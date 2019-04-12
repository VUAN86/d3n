package de.ascendro.f4m.server.request.jackpot;

import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.singleplayer.SinglePlayerGameParameters;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.request.RequestInfoImpl;

import java.math.BigDecimal;

public class PaymentRequestInformation extends RequestInfoImpl {
    private final String gameId;
    private final String mgiId;
    private final GameType gameType;

    private final String transactionLogId;
    private final BigDecimal entryFeeAmount;
    private final Currency entryFeeCurrency;

    private SinglePlayerGameParameters singlePlayerGameConfig;

    public PaymentRequestInformation(String gameId, String mgiId, String transactionLogId, BigDecimal entryFeeAmount, Currency entryFeeCurrency, GameType gameType) {
        this.gameId = gameId;
        this.mgiId = mgiId;
        this.gameType = gameType;

        this.entryFeeAmount = entryFeeAmount;
        this.entryFeeCurrency = entryFeeCurrency;
        this.transactionLogId = transactionLogId;
    }

    public void setSinglePlayerGameConfig(SinglePlayerGameParameters singlePlayerGameConfig) {
        this.singlePlayerGameConfig = singlePlayerGameConfig;
    }

    public SinglePlayerGameParameters getSinglePlayerGameConfig() {
        return singlePlayerGameConfig;
    }

    public String getGameId() {
        return gameId;
    }

    public String getMgiId() {
        return mgiId;
    }

    public GameType getGameType() {
        return gameType;
    }

    public String getTransactionLogId() {
        return transactionLogId;
    }

    public BigDecimal getEntryFeeAmount() {
        return entryFeeAmount;
    }

    public Currency getEntryFeeCurrency() {
        return entryFeeCurrency;
    }



    public enum Type {
        ENTRY_FEE, JOKER_PURCHASE, LOAD_OR_WITHDRAW_WITHOUT_COVERAGE
    }

}
