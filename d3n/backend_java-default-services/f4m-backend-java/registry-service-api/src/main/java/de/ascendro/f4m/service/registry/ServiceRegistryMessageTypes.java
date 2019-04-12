package de.ascendro.f4m.service.registry;

import de.ascendro.f4m.service.json.model.type.MessageType;

public enum ServiceRegistryMessageTypes implements MessageType {
	REGISTER, REGISTER_RESPONSE, 
	UNREGISTER, UNREGISTER_RESPONSE,
	HEARTBEAT, HEARTBEAT_RESPONSE, 
	GET, GET_RESPONSE, 
	LIST, LIST_RESPONSE,
	PUSH_SERVICE_STATISTICS,
	GET_INFRASTRUCTURE_STATISTICS, GET_INFRASTRUCTURE_STATISTICS_RESPONSE;

	public static final String SERVICE_NAME = "serviceRegistry";

	@Override
	public String getShortName() {
		return convertEnumNameToMessageShortName(name());
	}

	@Override
	public String getNamespace() {
		return SERVICE_NAME;
	}
}
