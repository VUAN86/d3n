package de.ascendro.f4m.service.gateway;

import de.ascendro.f4m.service.json.model.type.MessageType;

public enum GatewayMessageTypes implements MessageType {

	CLIENT_DISCONNECT, INSTANCE_DISCONNECT,
	SEND_USER_MESSAGE, SEND_USER_MESSAGE_RESPONSE;

	public static final String SERVICE_NAME = "gateway";
	
	@Override
	public String getShortName() {
		return convertEnumNameToMessageShortName(name());
	}

	@Override
	public String getNamespace() {
		return SERVICE_NAME;
	}
}
