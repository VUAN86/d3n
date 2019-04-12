package de.ascendro.f4m.service.game.engine.client.payment;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.request.RequestInfoImpl;

import java.util.List;

public class CancelGameTournamentRequestInfo extends RequestInfoImpl {
    private String gameInstanceId;
    private List<String> transactionIds;
    private JsonMessageContent responseToForward;

    public CancelGameTournamentRequestInfo(String gameInstanceId, List<String> transactionIds, RequestInfo originalRequestInfo) {
        super(originalRequestInfo.getSourceMessage(), originalRequestInfo.getSourceSession());
        this.gameInstanceId = gameInstanceId;
        this.transactionIds = transactionIds;
    }

    public JsonMessageContent getResponseToForward() {
        return responseToForward;
    }

    public void setResponseToForward(JsonMessageContent responseToForward) {
        this.responseToForward = responseToForward;


    }

    public String getGameInstanceId() {
        return gameInstanceId;
    }

    public void setGameInstanceId(String gameInstanceId) {
        this.gameInstanceId = gameInstanceId;
    }

    public List<String> getTransactionIds() {
        return transactionIds;
    }

    public void setTransactionIds(List<String> transactionIds) {
        this.transactionIds = transactionIds;
    }

}
