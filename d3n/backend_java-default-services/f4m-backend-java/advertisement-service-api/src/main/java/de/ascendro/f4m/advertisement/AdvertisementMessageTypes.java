package de.ascendro.f4m.advertisement;

import de.ascendro.f4m.service.json.model.type.MessageType;

public enum AdvertisementMessageTypes implements MessageType {


    GAME_ADVERTISEMENTS_GET, GAME_ADVERTISEMENTS_GET_RESPONSE,
    ADVERTISEMENT_HAS_BEEN_SHOWN, ADVERTISEMENT_HAS_BEEN_SHOWN_RESPONSE;

    public static final String SERVICE_NAME = "advertisement";

    @Override
    public String getShortName() {
        return convertEnumNameToMessageShortName(name());
    }

    @Override
    public String getNamespace() {
        return SERVICE_NAME;
    }
}
