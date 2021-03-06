package de.ascendro.f4m.service.game.selection;

import java.util.Arrays;

import de.ascendro.f4m.service.json.model.type.MessageType;

public enum GameSelectionMessageTypes implements MessageType {
	GET_GAME_LIST, GET_GAME_LIST_RESPONSE,
	GET_GAME, GET_GAME_RESPONSE,
	
	INVITE_USERS_TO_GAME, INVITE_USERS_TO_GAME_RESPONSE,
	INVITE_EXTERNAL_PERSON_TO_GAME, INVITE_EXTERNAL_PERSON_TO_GAME_RESPONSE,
	INVITE_GROUP_TO_GAME, INVITE_GROUP_TO_GAME_RESPONSE,
	CREATE_PUBLIC_GAME, CREATE_PUBLIC_GAME_RESPONSE,
	PUBLIC_GAME_LIST, PUBLIC_GAME_LIST_RESPONSE,
	
	INVITATION_LIST, INVITATION_LIST_RESPONSE,
	INVITED_LIST, INVITED_LIST_RESPONSE,
	RESPOND_TO_INVITATION, RESPOND_TO_INVITATION_RESPONSE,
	REJECT_INVITATION, REJECT_INVITATION_RESPONSE,
	JOIN_MULTIPLAYER_GAME, JOIN_MULTIPLAYER_GAME_RESPONSE,
	
	GAME_START_NOTIFICATION, GAME_START_COUNT_DOWN_NOTIFICATION,
	RECEIVE_GAME_START_NOTIFICATIONS, RECEIVE_GAME_START_NOTIFICATIONS_RESPONSE,
	
	GET_DASHBOARD, GET_DASHBOARD_RESPONSE,
	UPDATE_PLAYED_GAME,
	ACTIVATE_INVITATIONS;

	public static final String SERVICE_NAME = "gameSelection";

	@Override
	public String getShortName() {
		return convertEnumNameToMessageShortName(name());
	}

	@Override
	public String getNamespace() {
		return SERVICE_NAME;
	}
	
	public boolean isInternalMessage() {
		return Arrays.asList(UPDATE_PLAYED_GAME, ACTIVATE_INVITATIONS).contains(this);
	}

}
