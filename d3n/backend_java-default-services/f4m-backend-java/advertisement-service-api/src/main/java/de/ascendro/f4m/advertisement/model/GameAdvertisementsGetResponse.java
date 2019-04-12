package de.ascendro.f4m.advertisement.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GameAdvertisementsGetResponse implements JsonMessageContent {

    public GameAdvertisementsGetResponse(String[] advertisementBlobKeys) {
        this.advertisementBlobKeys = advertisementBlobKeys;
    }

    private String[] advertisementBlobKeys;

    public String[] getAdvertisementBlobKeys() {
        return advertisementBlobKeys;
    }

    public void setAdvertisementBlobKeys(String[] advertisementBlobKeys) {
        this.advertisementBlobKeys = advertisementBlobKeys;
    }
}
