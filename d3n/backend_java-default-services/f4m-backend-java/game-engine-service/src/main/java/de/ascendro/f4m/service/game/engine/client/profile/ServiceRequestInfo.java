package de.ascendro.f4m.service.game.engine.client.profile;

import de.ascendro.f4m.service.request.RequestInfoImpl;

public class ServiceRequestInfo extends RequestInfoImpl {
    private final String gameInstanceId;

    public ServiceRequestInfo(String gameInstanceId) {
        this.gameInstanceId = gameInstanceId;
    }

    public String getGameInstanceId() {
        return gameInstanceId;
    }

}