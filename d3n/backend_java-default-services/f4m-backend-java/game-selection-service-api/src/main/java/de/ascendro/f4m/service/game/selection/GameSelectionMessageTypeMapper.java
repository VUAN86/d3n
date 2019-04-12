package de.ascendro.f4m.service.game.selection;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.game.selection.model.dashboard.GetDashboardRequest;
import de.ascendro.f4m.service.game.selection.model.dashboard.GetDashboardResponse;
import de.ascendro.f4m.service.game.selection.model.dashboard.UpdatePlayedGameRequest;
import de.ascendro.f4m.service.game.selection.model.game.GetGameListRequest;
import de.ascendro.f4m.service.game.selection.model.game.GetGameListResponse;
import de.ascendro.f4m.service.game.selection.model.game.GetGameRequest;
import de.ascendro.f4m.service.game.selection.model.game.GetGameResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.ActivateInvitationsRequest;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.CreatePublicGameRequest;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.CreatePublicGameResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InvitationListRequest;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InvitationListResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InviteExternalPersonToGameRequest;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InviteExternalPersonToGameResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InviteGroupToGameRequest;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InviteGroupToGameResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InviteUsersToGameRequest;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InviteUsersToGameResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InvitedListRequest;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InvitedListResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.JoinMultiplayerGameRequest;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.JoinMultiplayerGameResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.PublicGameListRequest;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.PublicGameListResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.RejectInvitationRequest;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.RespondToInvitationRequest;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.RespondToInvitationResponse;
import de.ascendro.f4m.service.game.selection.model.subscription.GameStartCountDownNotification;
import de.ascendro.f4m.service.game.selection.model.subscription.GameStartNotification;
import de.ascendro.f4m.service.game.selection.model.subscription.ReceiveGameStartNotificationsRequest;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;

public class GameSelectionMessageTypeMapper extends JsonMessageTypeMapImpl {
	private static final long serialVersionUID = 7451131403092737963L;

	public GameSelectionMessageTypeMapper() {
		init();
	}

	protected void init() {
		this.register(GameSelectionMessageTypes.GET_GAME_LIST, new TypeToken<GetGameListRequest>() {});
		this.register(GameSelectionMessageTypes.GET_GAME_LIST_RESPONSE, new TypeToken<GetGameListResponse>() {});
		
		this.register(GameSelectionMessageTypes.GET_GAME, new TypeToken<GetGameRequest>() {});
		this.register(GameSelectionMessageTypes.GET_GAME_RESPONSE, new TypeToken<GetGameResponse>() {});
		
		this.register(GameSelectionMessageTypes.INVITE_USERS_TO_GAME, new TypeToken<InviteUsersToGameRequest>() {});
		this.register(GameSelectionMessageTypes.INVITE_USERS_TO_GAME_RESPONSE, new TypeToken<InviteUsersToGameResponse>() {});
		
		this.register(GameSelectionMessageTypes.INVITE_EXTERNAL_PERSON_TO_GAME,	new TypeToken<InviteExternalPersonToGameRequest>() {});
		this.register(GameSelectionMessageTypes.INVITE_EXTERNAL_PERSON_TO_GAME_RESPONSE, new TypeToken<InviteExternalPersonToGameResponse>() {});
		
		this.register(GameSelectionMessageTypes.INVITE_GROUP_TO_GAME, new TypeToken<InviteGroupToGameRequest>() {});
		this.register(GameSelectionMessageTypes.INVITE_GROUP_TO_GAME_RESPONSE, new TypeToken<InviteGroupToGameResponse>() {});
		
		this.register(GameSelectionMessageTypes.CREATE_PUBLIC_GAME, new TypeToken<CreatePublicGameRequest>() {});
		this.register(GameSelectionMessageTypes.CREATE_PUBLIC_GAME_RESPONSE, new TypeToken<CreatePublicGameResponse>() {});
		
		this.register(GameSelectionMessageTypes.PUBLIC_GAME_LIST, new TypeToken<PublicGameListRequest>() {});
		this.register(GameSelectionMessageTypes.PUBLIC_GAME_LIST_RESPONSE, new TypeToken<PublicGameListResponse>() {});
		
		this.register(GameSelectionMessageTypes.INVITATION_LIST, new TypeToken<InvitationListRequest>() {});
		this.register(GameSelectionMessageTypes.INVITATION_LIST_RESPONSE, new TypeToken<InvitationListResponse>() {});
		
		this.register(GameSelectionMessageTypes.INVITED_LIST, new TypeToken<InvitedListRequest>() {});
		this.register(GameSelectionMessageTypes.INVITED_LIST_RESPONSE, new TypeToken<InvitedListResponse>() {});
		
		this.register(GameSelectionMessageTypes.RESPOND_TO_INVITATION, new TypeToken<RespondToInvitationRequest>() {});
		this.register(GameSelectionMessageTypes.RESPOND_TO_INVITATION_RESPONSE, new TypeToken<RespondToInvitationResponse>() {});
		
		this.register(GameSelectionMessageTypes.REJECT_INVITATION, new TypeToken<RejectInvitationRequest>() {});
		this.register(GameSelectionMessageTypes.REJECT_INVITATION_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});
		
		this.register(GameSelectionMessageTypes.JOIN_MULTIPLAYER_GAME, new TypeToken<JoinMultiplayerGameRequest>() {});
		this.register(GameSelectionMessageTypes.JOIN_MULTIPLAYER_GAME_RESPONSE, new TypeToken<JoinMultiplayerGameResponse>() {});
		
		this.register(GameSelectionMessageTypes.GAME_START_NOTIFICATION, new TypeToken<GameStartNotification>() {});
		
		this.register(GameSelectionMessageTypes.GAME_START_COUNT_DOWN_NOTIFICATION, new TypeToken<GameStartCountDownNotification>() {});
		
		this.register(GameSelectionMessageTypes.RECEIVE_GAME_START_NOTIFICATIONS, new TypeToken<ReceiveGameStartNotificationsRequest>() {});
		this.register(GameSelectionMessageTypes.RECEIVE_GAME_START_NOTIFICATIONS_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});
		
		this.register(GameSelectionMessageTypes.GET_DASHBOARD, new TypeToken<GetDashboardRequest>() {});
		this.register(GameSelectionMessageTypes.GET_DASHBOARD_RESPONSE, new TypeToken<GetDashboardResponse>() {});
		
		this.register(GameSelectionMessageTypes.UPDATE_PLAYED_GAME, new TypeToken<UpdatePlayedGameRequest>() {});
		
		this.register(GameSelectionMessageTypes.ACTIVATE_INVITATIONS, new TypeToken<ActivateInvitationsRequest>() {});
	}

}
