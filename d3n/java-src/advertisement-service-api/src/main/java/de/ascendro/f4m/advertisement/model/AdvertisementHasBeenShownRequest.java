package de.ascendro.f4m.advertisement.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class AdvertisementHasBeenShownRequest implements JsonMessageContent {

    private String advertisementBlobKey;
    private String gameId;
    private String gameInstanceId;

    public String getAdvertisementBlobKey() {
        return advertisementBlobKey;
    }

    public void setAdvertisementBlobKey(String advertisementBlobKey) {
        this.advertisementBlobKey = advertisementBlobKey;
    }

    public String getGameInstanceId() { return gameInstanceId; }

    public void setGameInstanceId(String gameInstanceId) { this.gameInstanceId = gameInstanceId; }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }
}
