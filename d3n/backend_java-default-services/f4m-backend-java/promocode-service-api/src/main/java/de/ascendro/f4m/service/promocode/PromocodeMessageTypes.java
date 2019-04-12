package de.ascendro.f4m.service.promocode;

import de.ascendro.f4m.service.json.model.type.MessageType;

/**
 * Promocode Service supported messages
 */
public enum PromocodeMessageTypes implements MessageType {


	PROMOCODE_USE, PROMOCODE_USE_RESPONSE;

	public static final String SERVICE_NAME = "promocode";

	@Override
	public String getShortName() {
        return convertEnumNameToMessageShortName(name());
	}

	@Override
	public String getNamespace() {
		return SERVICE_NAME;
	}
}