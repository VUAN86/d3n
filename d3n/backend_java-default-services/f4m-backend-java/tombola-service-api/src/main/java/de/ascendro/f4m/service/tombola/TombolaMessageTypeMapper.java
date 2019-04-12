package de.ascendro.f4m.service.tombola;


import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;
import de.ascendro.f4m.service.tombola.model.MoveTombolasRequest;
import de.ascendro.f4m.service.tombola.model.buy.TombolaBuyRequest;
import de.ascendro.f4m.service.tombola.model.drawing.TombolaDrawingGetRequest;
import de.ascendro.f4m.service.tombola.model.drawing.TombolaDrawingGetResponse;
import de.ascendro.f4m.service.tombola.model.drawing.TombolaDrawingListRequest;
import de.ascendro.f4m.service.tombola.model.drawing.TombolaDrawingListResponse;
import de.ascendro.f4m.service.tombola.model.events.TombolaEventRequest;
import de.ascendro.f4m.service.tombola.model.get.TombolaBuyerListRequest;
import de.ascendro.f4m.service.tombola.model.get.TombolaBuyerListResponse;
import de.ascendro.f4m.service.tombola.model.get.TombolaGetRequest;
import de.ascendro.f4m.service.tombola.model.get.TombolaGetResponse;
import de.ascendro.f4m.service.tombola.model.get.TombolaListRequest;
import de.ascendro.f4m.service.tombola.model.get.TombolaListResponse;
import de.ascendro.f4m.service.tombola.model.get.TombolaWinnerListRequest;
import de.ascendro.f4m.service.tombola.model.get.TombolaWinnerListResponse;
import de.ascendro.f4m.service.tombola.model.get.UserTombolaGetRequest;
import de.ascendro.f4m.service.tombola.model.get.UserTombolaGetResponse;
import de.ascendro.f4m.service.tombola.model.get.UserTombolaListRequest;
import de.ascendro.f4m.service.tombola.model.get.UserTombolaListResponse;

public class TombolaMessageTypeMapper extends JsonMessageTypeMapImpl {

	private static final long serialVersionUID = -2612882596176213982L;

	public TombolaMessageTypeMapper() {
        init();
    }

    protected void init() {

        this.register(TombolaMessageTypes.TOMBOLA_GET.getMessageName(), new TypeToken<TombolaGetRequest>() {}.getType());
        this.register(TombolaMessageTypes.TOMBOLA_GET_RESPONSE.getMessageName(), new TypeToken<TombolaGetResponse>() {}
                .getType());

        this.register(TombolaMessageTypes.TOMBOLA_LIST.getMessageName(), new TypeToken<TombolaListRequest>() {}.getType());
        this.register(TombolaMessageTypes.TOMBOLA_LIST_RESPONSE.getMessageName(), new TypeToken<TombolaListResponse>() {}.getType());

        this.register(TombolaMessageTypes.TOMBOLA_BUY.getMessageName(), new TypeToken<TombolaBuyRequest>() {}.getType());
        this.register(TombolaMessageTypes.TOMBOLA_BUY_RESPONSE.getMessageName(), new TypeToken<EmptyJsonMessageContent>() {}
                .getType());

        this.register(TombolaMessageTypes.TOMBOLA_DRAWING_LIST.getMessageName(), new TypeToken<TombolaDrawingListRequest>() {}.getType());
        this.register(TombolaMessageTypes.TOMBOLA_DRAWING_LIST_RESPONSE.getMessageName(), new TypeToken<TombolaDrawingListResponse>() {}
                .getType());

        this.register(TombolaMessageTypes.TOMBOLA_DRAWING_GET.getMessageName(), new TypeToken<TombolaDrawingGetRequest>() {}.getType());
        this.register(TombolaMessageTypes.TOMBOLA_DRAWING_GET_RESPONSE.getMessageName(), new TypeToken<TombolaDrawingGetResponse>() {}
                .getType());

        this.register(TombolaMessageTypes.USER_TOMBOLA_LIST.getMessageName(), new TypeToken<UserTombolaListRequest>() {}.getType());
        this.register(TombolaMessageTypes.USER_TOMBOLA_LIST_RESPONSE.getMessageName(), new TypeToken<UserTombolaListResponse>() {}
                .getType());

        this.register(TombolaMessageTypes.USER_TOMBOLA_GET.getMessageName(), new TypeToken<UserTombolaGetRequest>() {}.getType());
        this.register(TombolaMessageTypes.USER_TOMBOLA_GET_RESPONSE.getMessageName(), new TypeToken<UserTombolaGetResponse>() {}
                .getType());

        this.register(TombolaMessageTypes.MOVE_TOMBOLAS.getMessageName(), new TypeToken<MoveTombolasRequest>() {}.getType());
        this.register(TombolaMessageTypes.MOVE_TOMBOLAS_RESPONSE.getMessageName(), new TypeToken<EmptyJsonMessageContent>() {}
                .getType());

        this.register(TombolaMessageTypes.TOMBOLA_WINNER_LIST.getMessageName(), new TypeToken<TombolaWinnerListRequest>() {}.getType());
        this.register(TombolaMessageTypes.TOMBOLA_WINNER_LIST_RESPONSE.getMessageName(), new TypeToken<TombolaWinnerListResponse>() {}.getType());

        this.register(TombolaMessageTypes.TOMBOLA_BUYER_LIST.getMessageName(), new TypeToken<TombolaBuyerListRequest>() {}.getType());
        this.register(TombolaMessageTypes.TOMBOLA_BUYER_LIST_RESPONSE.getMessageName(), new TypeToken<TombolaBuyerListResponse>() {}.getType());

        this.register(TombolaMessageTypes.TOMBOLA_OPEN_CHECKOUT.getMessageName(), new TypeToken<TombolaEventRequest>() {}.getType());
        this.register(TombolaMessageTypes.TOMBOLA_OPEN_CHECKOUT_RESPONSE.getMessageName(), new TypeToken<EmptyJsonMessageContent>() {}.getType());

        this.register(TombolaMessageTypes.TOMBOLA_CLOSE_CHECKOUT.getMessageName(), new TypeToken<TombolaEventRequest>() {}.getType());
        this.register(TombolaMessageTypes.TOMBOLA_CLOSE_CHECKOUT_RESPONSE.getMessageName(), new TypeToken<EmptyJsonMessageContent>() {}.getType());

        this.register(TombolaMessageTypes.TOMBOLA_DRAW.getMessageName(), new TypeToken<TombolaEventRequest>() {}.getType());
        this.register(TombolaMessageTypes.TOMBOLA_DRAW_RESPONSE.getMessageName(), new TypeToken<EmptyJsonMessageContent>() {}.getType());
    }
}