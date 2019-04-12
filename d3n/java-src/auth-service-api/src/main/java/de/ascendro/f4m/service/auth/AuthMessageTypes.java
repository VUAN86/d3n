package de.ascendro.f4m.service.auth;

import de.ascendro.f4m.service.json.model.type.MessageType;

public enum AuthMessageTypes implements MessageType {

	GET_PUBLIC_KEY, GET_PUBLIC_KEY_RESPONSE,
	INVITE_USER_BY_EMAIL, INVITE_USER_BY_EMAIL_RESPONSE,
	SET_USER_ROLE, SET_USER_ROLE_RESPONSE;
	
	public static final String SERVICE_NAME = "auth";

	@Override
	public String getShortName() {
		return convertEnumNameToMessageShortName(name());
	}

	@Override
	public String getNamespace() {
		return SERVICE_NAME;
	}
}
