package de.ascendro.f4m.service.tombola;

import de.ascendro.f4m.service.json.model.type.MessageType;

public enum TombolaMessageTypes implements MessageType {


    TOMBOLA_GET, TOMBOLA_GET_RESPONSE,
    TOMBOLA_LIST, TOMBOLA_LIST_RESPONSE,
    TOMBOLA_BUY, TOMBOLA_BUY_RESPONSE,

    TOMBOLA_DRAWING_GET, TOMBOLA_DRAWING_GET_RESPONSE,
    TOMBOLA_DRAWING_LIST, TOMBOLA_DRAWING_LIST_RESPONSE,
    USER_TOMBOLA_LIST, USER_TOMBOLA_LIST_RESPONSE,
    USER_TOMBOLA_GET, USER_TOMBOLA_GET_RESPONSE,

    TOMBOLA_WINNER_LIST, TOMBOLA_WINNER_LIST_RESPONSE,
    
    MOVE_TOMBOLAS, MOVE_TOMBOLAS_RESPONSE,

    TOMBOLA_BUYER_LIST, TOMBOLA_BUYER_LIST_RESPONSE,

    TOMBOLA_OPEN_CHECKOUT, TOMBOLA_OPEN_CHECKOUT_RESPONSE,
    TOMBOLA_CLOSE_CHECKOUT, TOMBOLA_CLOSE_CHECKOUT_RESPONSE,
    TOMBOLA_DRAW, TOMBOLA_DRAW_RESPONSE;

    public static final String SERVICE_NAME = "tombola";

    @Override
    public String getShortName() {
        return convertEnumNameToMessageShortName(name());
    }

    @Override
    public String getNamespace() {
        return SERVICE_NAME;
    }
}
