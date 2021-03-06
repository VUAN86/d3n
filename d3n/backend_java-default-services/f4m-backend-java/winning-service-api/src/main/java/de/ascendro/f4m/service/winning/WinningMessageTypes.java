package de.ascendro.f4m.service.winning;

import de.ascendro.f4m.service.json.model.type.MessageType;

/**
 * Winning Service supported messages
 */
public enum WinningMessageTypes implements MessageType {

	WINNING_COMPONENT_GET, WINNING_COMPONENT_GET_RESPONSE,
	WINNING_COMPONENT_LIST, WINNING_COMPONENT_LIST_RESPONSE,
	USER_WINNING_COMPONENT_ASSIGN, USER_WINNING_COMPONENT_ASSIGN_RESPONSE,
	USER_WINNING_COMPONENT_LIST, USER_WINNING_COMPONENT_LIST_RESPONSE,
	USER_WINNING_COMPONENT_GET, USER_WINNING_COMPONENT_GET_RESPONSE,
	USER_WINNING_COMPONENT_LOAD, USER_WINNING_COMPONENT_LOAD_RESPONSE,
	USER_WINNING_COMPONENT_START, USER_WINNING_COMPONENT_START_RESPONSE,
	USER_WINNING_COMPONENT_STOP, USER_WINNING_COMPONENT_STOP_RESPONSE,
	USER_WINNING_COMPONENT_MOVE, USER_WINNING_COMPONENT_MOVE_RESPONSE,
	USER_WINNING_LIST, USER_WINNING_LIST_RESPONSE,
	USER_WINNING_GET, USER_WINNING_GET_RESPONSE,
	USER_WINNING_MOVE, USER_WINNING_MOVE_RESPONSE;

	public static final String SERVICE_NAME = "winning";

	@Override
	public String getShortName() {
        return convertEnumNameToMessageShortName(name());
	}

	@Override
	public String getNamespace() {
		return SERVICE_NAME;
	}
}
