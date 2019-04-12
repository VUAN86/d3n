package de.ascendro.f4m.service.promocode;

import com.google.common.reflect.TypeToken;

import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;
import de.ascendro.f4m.service.promocode.model.promocode.PromocodeUseRequest;
import de.ascendro.f4m.service.promocode.model.promocode.PromocodeUseResponse;

@SuppressWarnings("serial")
public class PromocodeMessageTypeMapper extends JsonMessageTypeMapImpl {

	public PromocodeMessageTypeMapper() {
		init();
	}

	protected void init() {
		this.register(PromocodeMessageTypes.PROMOCODE_USE.getMessageName(),
                new TypeToken<PromocodeUseRequest>() {}.getType());
		this.register(PromocodeMessageTypes.PROMOCODE_USE_RESPONSE.getMessageName(),
                new TypeToken<PromocodeUseResponse>() {}.getType());


	}
}
