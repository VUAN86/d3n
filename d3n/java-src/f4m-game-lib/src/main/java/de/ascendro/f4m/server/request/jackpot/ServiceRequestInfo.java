package de.ascendro.f4m.server.request.jackpot;

import de.ascendro.f4m.service.request.RequestInfoImpl;

import java.util.List;

public class ServiceRequestInfo extends RequestInfoImpl
{
    private final String gameInstanceId;
    private final List<String> transactionLogIds;

    public ServiceRequestInfo(String gameInstanceId) {
        this(gameInstanceId, null);
    }

    public ServiceRequestInfo(String gameInstanceId, List<String> transactionLogIds) {
        this.gameInstanceId = gameInstanceId;
        this.transactionLogIds = transactionLogIds;
    }

    public String getGameInstanceId() {
        return gameInstanceId;
    }

    public List<String> getTransactionLogIds() {
        return transactionLogIds;
    }
}