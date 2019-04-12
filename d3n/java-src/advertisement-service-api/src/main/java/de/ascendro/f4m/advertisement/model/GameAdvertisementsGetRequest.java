package de.ascendro.f4m.advertisement.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GameAdvertisementsGetRequest implements JsonMessageContent {
    private long providerId;

    public long getProviderId() {
        return providerId;
    }

    public void setProviderId(long providerId) {
        this.providerId = providerId;
    }
}
