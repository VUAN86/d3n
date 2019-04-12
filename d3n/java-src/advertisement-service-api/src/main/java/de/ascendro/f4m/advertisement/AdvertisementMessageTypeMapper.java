package de.ascendro.f4m.advertisement;


import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.advertisement.model.AdvertisementHasBeenShownRequest;
import de.ascendro.f4m.advertisement.model.GameAdvertisementsGetRequest;
import de.ascendro.f4m.advertisement.model.GameAdvertisementsGetResponse;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;

public class AdvertisementMessageTypeMapper extends JsonMessageTypeMapImpl {

	private static final long serialVersionUID = 3493094214333740940L;

	public AdvertisementMessageTypeMapper() {
        init();
    }

    protected void init() {
        register(AdvertisementMessageTypes.GAME_ADVERTISEMENTS_GET.getMessageName(),
                new TypeToken<GameAdvertisementsGetRequest>() {}.getType());
        register(AdvertisementMessageTypes.GAME_ADVERTISEMENTS_GET_RESPONSE.getMessageName(),
                new TypeToken<GameAdvertisementsGetResponse>() {}.getType());

        register(AdvertisementMessageTypes.ADVERTISEMENT_HAS_BEEN_SHOWN.getMessageName(),
                new TypeToken<AdvertisementHasBeenShownRequest>() {}.getType());
        register(AdvertisementMessageTypes.ADVERTISEMENT_HAS_BEEN_SHOWN_RESPONSE.getMessageName(),
                new TypeToken<EmptyJsonMessageContent>() {}.getType());
    }

}
