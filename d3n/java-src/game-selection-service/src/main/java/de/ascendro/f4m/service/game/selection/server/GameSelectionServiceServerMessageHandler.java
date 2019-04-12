package de.ascendro.f4m.service.game.selection.server;

import de.ascendro.f4m.server.game.GameUtil;
import de.ascendro.f4m.server.request.jackpot.JackpotDataGetter;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.selection.GameSelectionMessageTypes;
import de.ascendro.f4m.service.game.selection.client.communicator.AuthServiceCommunicator;
import de.ascendro.f4m.service.game.selection.client.communicator.FriendManagerCommunicator;
import de.ascendro.f4m.service.game.selection.client.communicator.GameEngineCommunicator;
import de.ascendro.f4m.service.game.selection.model.dashboard.GetDashboardRequest;
import de.ascendro.f4m.service.game.selection.model.dashboard.GetDashboardResponse;
import de.ascendro.f4m.service.game.selection.model.dashboard.UpdatePlayedGameRequest;
import de.ascendro.f4m.service.game.selection.model.game.*;
import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.game.selection.model.multiplayer.InvitedUser;
import de.ascendro.f4m.service.game.selection.model.multiplayer.PublicGameFilter;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.*;
import de.ascendro.f4m.service.game.selection.model.subscription.ReceiveGameStartNotificationsRequest;
import de.ascendro.f4m.service.game.selection.subscription.SubscriptionManager;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.eclipse.jetty.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Jetty Web Socket message handler for Game Selection Service
 * 
 */
public class GameSelectionServiceServerMessageHandler extends DefaultJsonMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GameSelectionServiceServerMessageHandler.class);

	private final GameSelector gameSelector;
	private final FriendManagerCommunicator friendManagerCommunicator;
	private final SubscriptionManager subscriptionManager;
	private final MultiplayerGameInstanceManager mgiManager;
	private final GameEngineCommunicator gameEngineCommunicator;
	private final AuthServiceCommunicator authServiceCommunicator;
	private final DashboardManager dashboardManager;
	private final JackpotDataGetter jackpotDataGetter;
	public GameSelectionServiceServerMessageHandler(GameSelector gameSelector,
			FriendManagerCommunicator friendManagerCommunicator, SubscriptionManager subscriptionManager,
			MultiplayerGameInstanceManager mgiManager, GameEngineCommunicator gameEngineCommunicator,
			AuthServiceCommunicator authServiceCommunicator, DashboardManager dashboardManager,
			JackpotDataGetter jackpotDataGetter) {
		
		this.gameSelector = gameSelector;
		this.friendManagerCommunicator = friendManagerCommunicator;
		this.subscriptionManager = subscriptionManager;
		this.mgiManager = mgiManager;
		this.gameEngineCommunicator = gameEngineCommunicator;
		this.authServiceCommunicator = authServiceCommunicator;
		this.dashboardManager = dashboardManager;
		this.jackpotDataGetter = jackpotDataGetter;

	}

	@Override
	public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> message) throws F4MException {
		final GameSelectionMessageTypes gameSelectionType = message.getType(GameSelectionMessageTypes.class);
		final JsonMessageContent response;
		if (gameSelectionType != null) {
			if (isClientInfoRequired(message.getClientInfo(), gameSelectionType)) {
				throw new F4MInsufficientRightsException("User not authenticated");
			} else {
				response = onGameSelectionMessage(message.getClientInfo(), message, gameSelectionType);
			}
		} else {
			throw new F4MValidationFailedException(String.format("Received invalid message [%s]", message.getName()));
		}

		return response;
	}
	
	private boolean isClientInfoRequired(ClientInfo clientInfo, GameSelectionMessageTypes gameSelectionMessageTypes) {
		return !gameSelectionMessageTypes.isInternalMessage()
				&& (clientInfo == null || StringUtil.isBlank(clientInfo.getUserId()));
	}

	@SuppressWarnings("unchecked")
	private JsonMessageContent onGameSelectionMessage(ClientInfo clientInfo, JsonMessage<? extends JsonMessageContent> message,
			GameSelectionMessageTypes type) {
		JsonMessageContent response = null;

		switch (type) {
		case GET_GAME_LIST:
			response = onGetGameList(clientInfo, (JsonMessage<GetGameListRequest>) message);
			break;
		case GET_GAME:
			response = onGetGame((JsonMessage<GetGameRequest>) message);
			break;
		case INVITE_USERS_TO_GAME:
			response = onInviteFriendsToGame(clientInfo, (JsonMessage<InviteUsersToGameRequest>) message);
			break;
		case INVITE_EXTERNAL_PERSON_TO_GAME:
			// not used by front end / marked as internal (#9849)
			onInviteExternalPersonToGame(clientInfo, (JsonMessage<InviteExternalPersonToGameRequest>) message);
			break;
		case INVITE_GROUP_TO_GAME:
			onInviteGroupToGame(clientInfo, (JsonMessage<InviteGroupToGameRequest>) message);
			break;
		case CREATE_PUBLIC_GAME:
			onCreatePublicGame(clientInfo, (JsonMessage<CreatePublicGameRequest>) message);
			break;
		case PUBLIC_GAME_LIST:
			response = onPublicGameList(clientInfo, (JsonMessage<PublicGameListRequest>) message);
			break;
		case INVITATION_LIST:
			response = onInvitationList(clientInfo, (JsonMessage<InvitationListRequest>) message);
			break;
		case INVITED_LIST:
			response = onInvitedList((JsonMessage<InvitedListRequest>) message);
			break;
		case RESPOND_TO_INVITATION:
			response = onRespondToInvitation(clientInfo, (JsonMessage<RespondToInvitationRequest>) message);
			break;
		case REJECT_INVITATION:
			response = onRejectInvitation(clientInfo, (JsonMessage<RejectInvitationRequest>) message);
			break;
		case JOIN_MULTIPLAYER_GAME:
			onJoinMultiplayerGame(clientInfo, (JsonMessage<JoinMultiplayerGameRequest>) message);
			break;
		case RECEIVE_GAME_START_NOTIFICATIONS:
			response = onReceiveGameStartNotifications(clientInfo.getUserId(),
					(JsonMessage<ReceiveGameStartNotificationsRequest>) message);
			break;
		case GET_DASHBOARD:
			response = onGetDashboard(clientInfo, (JsonMessage<GetDashboardRequest>) message);
			break;
		case UPDATE_PLAYED_GAME:
			onUpdatePlayedGame((JsonMessage<UpdatePlayedGameRequest>) message);
			break;
		case ACTIVATE_INVITATIONS:
			onActivateInvitations((JsonMessage<ActivateInvitationsRequest>) message);
			break;
		default:
			LOGGER.error("Game Selection Service received unrecognized {} message {}", type, message);
			throw new F4MValidationFailedException("Incorrect message type " + type);
		}

		return response;
	}

	private GetGameListResponse onGetGameList(ClientInfo clientInfo, JsonMessage<GetGameListRequest> message) {
		String userId = clientInfo.getUserId();
		GetGameListResponse response = null;
		Validate.notNull(message.getContent(), "Message content of getGameList is mandatory");
		Validate.isInstanceOf(GetGameListRequest.class, message.getContent());
		if (message.getContent().isByFriendCreated() || message.getContent().isByFriendPlayed()) {
			friendManagerCommunicator.sendGetFriendsRequest(message, userId, this.getSessionWrapper());
		} else {
			String countryCode = clientInfo.getCountryCode() != null ? clientInfo.getCountryCode().toString() : null;
			GameFilter gameFilter = new GameFilter(clientInfo.getTenantId(), clientInfo.getAppId(),
					countryCode, jsonMessageUtil.toJsonElement(message.getContent()));
			GetGameListResponse currentResponse = gameSelector.getGameList(userId, gameFilter);
			boolean sendResponseNow = jackpotDataGetter.synchronizeUpdatedData(currentResponse.getGames(),
					message, currentResponse);
			if (sendResponseNow){
				response = currentResponse;
			}
		}
		return response;
	}
	

	private GetGameResponse onGetGame(JsonMessage<GetGameRequest> message) {
		Validate.notNull(message.getContent(), "Message content of getGame is mandatory");
		Validate.isInstanceOf(GetGameRequest.class, message.getContent());

		return gameSelector.getGameWithJackpotInfo(message.getContent(), message,
				getSessionWrapper());
	}

	private InviteUsersToGameResponse onInviteFriendsToGame(ClientInfo clientInfo, JsonMessage<InviteUsersToGameRequest> message) {
		InviteUsersToGameRequest request = message.getContent();
		GameType gameType = mgiManager.getGameType(request.getMultiplayerGameParameters(), request.getMultiplayerGameInstanceId());
		GameUtil.validateUserRolesForGameType(gameType, clientInfo);
		mgiManager.validateInvitation(request.getMultiplayerGameParameters(), request.getMultiplayerGameInstanceId(), request.getUsersIds());
		mgiManager.ensureHasNoBlocking(clientInfo.getUserId(), request.getUsersIds(), true);
		InviteUsersToGameResponse response;
		if (request.getMultiplayerGameInstanceId() == null) {
			mgiManager.validateUserPermissionsForEntryFee(message.getName(), clientInfo, request.getMultiplayerGameParameters());
			mgiManager.createMultiplayerGameInstance(
					request.getMultiplayerGameParameters(),
					clientInfo,
					message,
					getSessionWrapper(),
					request.isRematch(), false);
			response = null;
		} else {
			mgiManager.validateUserPermissionsForEntryFee(message.getName(), clientInfo, request.getMultiplayerGameInstanceId());
			String userId = clientInfo.getUserId();
			List<String> invitedUsers = mgiManager.addUsers(request.getUsersIds(),
					request.getMultiplayerGameInstanceId(), userId);
			mgiManager.updateLastInvitedAndPausedDuelOpponents(request.getMultiplayerGameInstanceId(), userId,
					request.getUsersIds(), request.getPausedUsersIds());
			response = new InviteUsersToGameResponse();
			response.setMultiplayerGameInstanceId(request.getMultiplayerGameInstanceId());
			response.setUsersIds(invitedUsers);
			if (gameType.isDuel()) {
				response.setPausedUsersIds(request.getPausedUsersIds());
			}
		}
		return response;
	}

	private void onInviteExternalPersonToGame(ClientInfo clientInfo, JsonMessage<InviteExternalPersonToGameRequest> message) {
		final InviteExternalPersonToGameRequest request = message.getContent();
		GameType gameType = mgiManager.getGameType(request.getMultiplayerGameParameters(), request.getMultiplayerGameInstanceId());
		GameUtil.validateUserRolesForGameType(gameType, clientInfo);
		mgiManager.validateInvitation(request.getMultiplayerGameParameters(), request.getMultiplayerGameInstanceId(), Collections.singletonList(request.getEmail()));
		
		if (request.getMultiplayerGameInstanceId() == null) {
			mgiManager.validateUserPermissionsForEntryFee(message.getName(), clientInfo, request.getMultiplayerGameParameters());
			mgiManager.createMultiplayerGameInstance(
					request.getMultiplayerGameParameters(),
					clientInfo, message, getSessionWrapper(),
					false, false);
		} else {
			mgiManager.validateUserPermissionsForEntryFee(message.getName(), clientInfo, request.getMultiplayerGameInstanceId());
			authServiceCommunicator.requestInviteUserByEmail(message, this.getSessionWrapper(), request.getEmail(),
					request.getMultiplayerGameInstanceId(), null);
		}
	}
	
	private void onInviteGroupToGame(ClientInfo clientInfo, JsonMessage<InviteGroupToGameRequest> message) {
		InviteGroupToGameRequest request = message.getContent();
		GameType gameType = mgiManager.getGameType(request.getMultiplayerGameParameters(), request.getMultiplayerGameInstanceId());
		GameUtil.validateUserRolesForGameType(gameType, clientInfo);
		mgiManager.validateInvitation(request.getMultiplayerGameParameters(), request.getMultiplayerGameInstanceId(), null);
		
		if (request.getMultiplayerGameInstanceId() == null) {
			mgiManager.validateUserPermissionsForEntryFee(message.getName(), clientInfo, request.getMultiplayerGameParameters());
			mgiManager.createMultiplayerGameInstance(
					request.getMultiplayerGameParameters(),
					clientInfo, message, this.getSessionWrapper(),
					false, false);
		} else {
			mgiManager.validateUserPermissionsForEntryFee(message.getName(), clientInfo, request.getMultiplayerGameInstanceId());
			friendManagerCommunicator.requestGroupListPlayers(message, this.getSessionWrapper(), request.getMultiplayerGameInstanceId(), null);
		}
	}
	
	private void onCreatePublicGame(ClientInfo clientInfo, JsonMessage<CreatePublicGameRequest> message) {
		CreatePublicGameRequest request = message.getContent();
		GameType gameType = mgiManager.getGameType(request.getMultiplayerGameParameters(), null);
		GameUtil.validateUserRolesForGameType(gameType, clientInfo);
		mgiManager.validateUserPermissionsForEntryFee(message.getName(), clientInfo, request.getMultiplayerGameParameters());
		mgiManager.createPublicGame(request.getMultiplayerGameParameters(), clientInfo, message, getSessionWrapper());
	}
	
	private PublicGameListResponse onPublicGameList(ClientInfo clientInfo, JsonMessage<PublicGameListRequest> message) {
		PublicGameFilter filter = message.getContent();
		filter.setAppId(clientInfo.getAppId());
		filter.setDefaultGameTypeIfNotSet();
		filter.setNotByUserId(clientInfo.getUserId());
		int limit = message.getContent().getLimit();
		List<Invitation> publicGames = mgiManager.listPublicGames(filter, limit);
		addGameInfo(publicGames, clientInfo);
		PublicGameListResponse currentResponse= new  PublicGameListResponse(publicGames);
		PublicGameListResponse response = null;
		boolean syncResponse = jackpotDataGetter.synchronizeUpdatedDataInvitation(publicGames, message, currentResponse);
		if (syncResponse){
			response = currentResponse;
		}
		return response;
	}

	private void addGameInfo(List<Invitation> publicGames, ClientInfo clientInfo) {
		mgiManager.addGameInfo(publicGames);
		mgiManager.addGameInstanceInfo(publicGames, clientInfo.getUserId());
		mgiManager.addInvitationUserInfo(publicGames, i -> i.getInviter().getUserId(), Invitation::setInviter);
		mgiManager.addInvitationUserInfo(publicGames, i -> i.getCreator().getUserId(), Invitation::setCreator);
	}

	private InvitationListResponse onInvitationList(ClientInfo clientInfo, JsonMessage<InvitationListRequest> message) {
		InvitationListRequest content = message.getContent();
		content.validateFilterCriteria(InvitationListRequest.MAX_LIST_LIMIT);
		String tenantId = clientInfo.getTenantId();
		String appId = clientInfo.getAppId();
		List<Invitation> invitationList = mgiManager.getInvitationList(tenantId, appId, clientInfo.getUserId(), content.getStates(), content.getCreatedBy(), content);
		if (content.getIsPending() != null) {
			invitationList = mgiManager.filterInvitationsByResults(invitationList, content.getIsPending());
		}
		if (BooleanUtils.isTrue(content.getIncludeOpponents())) {
			mgiManager.addInvitationOpponents(invitationList, clientInfo.getUserId());
		}
		addGameInfo(invitationList, clientInfo);
		InvitationListResponse currentResponse = new InvitationListResponse(content.getLimit(), content.getOffset(), invitationList.size(), invitationList);
		InvitationListResponse response =null;
		boolean syncResponse = jackpotDataGetter.synchronizeUpdatedDataInvitation(invitationList, message, currentResponse);
		if (syncResponse){
			response = currentResponse;
		}
		
		return response;
	}

	private JsonMessageContent onInvitedList(JsonMessage<InvitedListRequest> message) {
		InvitedListRequest content = message.getContent();
		content.validateFilterCriteria(InvitedListRequest.MAX_LIST_LIMIT);

		List<InvitedUser> invitedList = mgiManager.getInvitedList(content.getMultiplayerGameInstanceId(),
				content.getLimit(), content.getOffset(), content.getOrderBy(), content.getSearchBy());
		return new InvitedListResponse(content.getLimit(), content.getOffset(), invitedList.size(), invitedList);
	}

	private RespondToInvitationResponse onRespondToInvitation(ClientInfo clientInfo, JsonMessage<RespondToInvitationRequest> message) {
		final String userId = clientInfo.getUserId();
		final RespondToInvitationRequest respondToInvitationRequest = message.getContent();
		RespondToInvitationResponse response = null;
		String mgiId = respondToInvitationRequest.getMultiplayerGameInstanceId();
		if (respondToInvitationRequest.isAccept()) {
			GameType gameType = mgiManager.getGameType(null, mgiId);
			GameUtil.validateUserRolesForGameType(gameType, clientInfo);
			mgiManager.validateUserPermissionsForEntryFee(message.getName(), clientInfo, mgiId);
			// TODO: lock MGI; if (availableEntryCount > processingCount) then register else "please wait" exception
			mgiManager.validateConfirmInvitation(mgiId, userId);
			gameEngineCommunicator.requestRegister(message, this.getSessionWrapper(), mgiId);
		//	mgiManager.deleteDuel(mgiId);
		} else {
			mgiManager.declineInvitation(userId, mgiId, clientInfo);
			response = new RespondToInvitationResponse();
		}
		return response;
	}
	
	private JsonMessageContent onRejectInvitation(ClientInfo clientInfo, JsonMessage<RejectInvitationRequest> message) {
		mgiManager.rejectInvitation(clientInfo.getUserId(), message.getContent().getMultiplayerGameInstanceId(), clientInfo);
		return new EmptyJsonMessageContent();
	}
	
	private void onJoinMultiplayerGame(ClientInfo clientInfo, JsonMessage<JoinMultiplayerGameRequest> message) {
		String mgiId = message.getContent().getMultiplayerGameInstanceId();
		GameType gameType = mgiManager.getGameType(null, mgiId);
		GameUtil.validateUserRolesForGameType(gameType, clientInfo);
		mgiManager.validateUserPermissionsForEntryFee(message.getName(), clientInfo, mgiId);
		mgiManager.validateParticipantCount(mgiId, clientInfo.getUserId());
		mgiManager.inviteUserIfNotExist(clientInfo.getUserId(), mgiId);
		gameEngineCommunicator.requestRegister(message, this.getSessionWrapper(), mgiId);
	}
	
	private EmptyJsonMessageContent onReceiveGameStartNotifications(String userId, JsonMessage<ReceiveGameStartNotificationsRequest> message) {
		ReceiveGameStartNotificationsRequest content = message.getContent();
		String mgiId = content.getMultiplayerGameInstanceId();
		mgiManager.validateUserState(userId, mgiId);
		if (content.isReceive()) {
			subscriptionManager.subscribeUserToGame(message.getClientId(), mgiId);
		} else {
			subscriptionManager.unsubscribeUserFromGame(message.getClientId(), mgiId);
		}
		return new EmptyJsonMessageContent();
	}
	
	private GetDashboardResponse onGetDashboard(ClientInfo clientInfo, JsonMessage<GetDashboardRequest> message) {
		GetDashboardResponse dashboardResponse = dashboardManager.getDashboard(clientInfo, message.getContent());
		return dashboardManager.addLastUserTournamentPlacement(dashboardResponse, message, this.getSessionWrapper());
	}

	private void onUpdatePlayedGame(JsonMessage<UpdatePlayedGameRequest> message) {
		UpdatePlayedGameRequest content = message.getContent();
		if (content.getMgiId() == null) {
			dashboardManager.updatePlayedGame(content.getTenantId(), content.getUserId(), content.getPlayedGameInfo());
		} else {
			dashboardManager.updateFinishedMultiplayerGame(content.getTenantId(), content.getMgiId());
		}
	}
	
	private void onActivateInvitations(JsonMessage<ActivateInvitationsRequest> message) {
		mgiManager.activateInvitationsAndSendInviteNotifications(message.getContent().getMgiId(), message.getClientInfo());
	}

}
