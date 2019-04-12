package de.ascendro.f4m.service.event;

import de.ascendro.f4m.service.json.model.type.MessageType;

public enum EventMessageTypes implements MessageType {

	SUBSCRIBE, SUBSCRIBE_RESPONSE, UNSUBSCRIBE, PUBLISH, NOTIFY_SUBSCRIBER, RESUBSCRIBE, RESUBSCRIBE_RESPONSE, INFO, INFO_RESPONSE;

	public static final String SERVICE_NAME = "event";
	public static final String NAMESPACE = SERVICE_NAME;

	@Override
	public String getShortName() {
		return convertEnumNameToMessageShortName(name());
	}

	@Override
	public String getNamespace() {
		return NAMESPACE;
	}
}
