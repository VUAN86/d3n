package de.ascendro.f4m.service.game.selection.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import de.ascendro.f4m.server.request.jackpot.PaymentRequestInformation;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import de.ascendro.f4m.server.analytics.model.InviteEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.request.jackpot.JackpotDataGetter;
import de.ascendro.f4m.server.request.jackpot.PaymentCreateJackpotRequestInfo;
import de.ascendro.f4m.server.request.jackpot.PaymentGetJackpotRequestInfo;
import de.ascendro.f4m.service.auth.AuthMessageTypes;
import de.ascendro.f4m.service.auth.model.register.InviteUserByEmailResponse;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.NotifySubscriberMessageContent;
import de.ascendro.f4m.service.event.model.subscribe.SubscribeResponse;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypes;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListAllIdsResponse;
import de.ascendro.f4m.service.friend.model.api.group.GroupListPlayersResponse;
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypes;
import de.ascendro.f4m.service.game.engine.model.register.RegisterResponse;
import de.ascendro.f4m.service.game.selection.GameSelectionMessageTypes;
import de.ascendro.f4m.service.game.selection.client.communicator.AuthServiceCommunicator;
import de.ascendro.f4m.service.game.selection.client.communicator.FriendManagerCommunicator;
import de.ascendro.f4m.service.game.selection.client.communicator.GameEngineCommunicator;
import de.ascendro.f4m.service.game.selection.client.communicator.ProfileCommunicator;
import de.ascendro.f4m.service.game.selection.model.dashboard.GetDashboardResponse;
import de.ascendro.f4m.service.game.selection.model.dashboard.PlayedGameInfo;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameFilter;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.GetGameListRequest;
import de.ascendro.f4m.service.game.selection.model.game.GetGameListResponse;
import de.ascendro.f4m.service.game.selection.model.game.GetGameResponse;
import de.ascendro.f4m.service.game.selection.model.game.Jackpot;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.CreatePublicGameResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InvitationListResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InviteExternalPersonToGameRequest;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InviteExternalPersonToGameResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InviteGroupToGameRequest;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InviteGroupToGameResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InviteUsersToGameRequest;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.InviteUsersToGameResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.JoinMultiplayerGameResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.PublicGameListResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.RespondToInvitationResponse;
import de.ascendro.f4m.service.game.selection.request.FriendsGameListRequestInfoImpl;
import de.ascendro.f4m.service.game.selection.request.InviteRequestInfoImpl;
import de.ascendro.f4m.service.game.selection.request.RequestInfoWithUserIdImpl;
import de.ascendro.f4m.service.game.selection.request.ResultEngineRequestInfo;
import de.ascendro.f4m.service.game.selection.request.UserMessageRequestInfo;
import de.ascendro.f4m.service.game.selection.server.DashboardManager;
import de.ascendro.f4m.service.game.selection.server.GameSelector;
import de.ascendro.f4m.service.game.selection.server.MultiplayerGameInstanceManager;
import de.ascendro.f4m.service.game.selection.subscription.StartLiveUserTournamentEvent;
import de.ascendro.f4m.service.game.selection.subscription.SubscriptionManager;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.internal.GetJackpotResponse;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.merge.ProfileMergeEvent;
import de.ascendro.f4m.service.profile.model.sub.get.GetProfileBlobResponse;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.result.engine.ResultEngineMessageTypes;
import de.ascendro.f4m.service.result.engine.model.get.GetMultiplayerResultsResponse;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendUserPushResponse;
import de.ascendro.f4m.service.util.EventServiceClient;

/**
 * Web Socket Client response message handler for Game Selection Service
 * 
 */
public class GameSelectionServiceClientMessageHandler extends DefaultJsonMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GameSelectionServiceClientMessageHandler.class);

	private final GameSelector gameSelector;
	private final SubscriptionManager subscriptionManager;
	private final MultiplayerGameInstanceManager mgiManager;
	private final AuthServiceCommunicator authServiceCommunicator;
	private final GameEngineCommunicator gameEngineCommunicator;
	private final ProfileCommunicator profileCommunicator;
	private final EventServiceClient eventServiceClient;
	private final CommonProfileAerospikeDao profileDao;
	private final DashboardManager dashboardManager;
	private final FriendManagerCommunicator friendManagerCommunicator;
	private final Tracker tracker;
	private final JackpotDataGetter jackpotDataGetter;
	private final JsonMessageUtil jsonUtil;

	public GameSelectionServiceClientMessageHandler(GameSelector gameSelector, SubscriptionManager subscriptionManager,
			MultiplayerGameInstanceManager mgiManager, AuthServiceCommunicator authServiceCommunicator,
			GameEngineCommunicator gameEngineCommunicator, ProfileCommunicator profileCommunicator,
			EventServiceClient eventServiceClient, CommonProfileAerospikeDao profileDao,
			DashboardManager dashboardManager, FriendManagerCommunicator friendManagerCommunicator, Tracker tracker,
			JackpotDataGetter jackpotDataGetter, JsonMessageUtil jsonUtil) {
		this.gameSelector = gameSelector;
		this.subscriptionManager = subscriptionManager;
		this.mgiManager = mgiManager;
		this.authServiceCommunicator = authServiceCommunicator;
		this.gameEngineCommunicator = gameEngineCommunicator;
		this.profileCommunicator = profileCommunicator;
		this.eventServiceClient = eventServiceClient;
		this.profileDao = profileDao;
		this.dashboardManager = dashboardManager;
		this.friendManagerCommunicator = friendManagerCommunicator;
		this.tracker = tracker;
		this.jackpotDataGetter = jackpotDataGetter;
		this.jsonUtil = jsonUtil;
	}

	@Override
	protected void onEventServiceRegistered() {
		eventServiceClient.subscribe(true, GameSelectionMessageTypes.SERVICE_NAME, Game.getOpenRegistrationTopic());
		eventServiceClient.subscribe(true, GameSelectionMessageTypes.SERVICE_NAME, Game.getStartGameTopic());
		eventServiceClient.subscribe(true, GameSelectionMessageTypes.SERVICE_NAME, Game.getStartMultiplayerGameTopicAny());
		eventServiceClient.subscribe(true, GameSelectionMessageTypes.SERVICE_NAME, ProfileMergeEvent.PROFILE_MERGE_EVENT_TOPIC);
	}

	@Override
	public JsonMessageContent onUserMessage(RequestContext context) {
		JsonMessage<?> message = context.getMessage();
		
		FriendManagerMessageTypes friendManagerMessageType;
		ProfileMessageTypes profileMessageType;
		AuthMessageTypes authMessageType;
		PaymentMessageTypes paymentMessageType;
		GameEngineMessageTypes gameEngineMessageType;
		EventMessageTypes eventMessageTypes;
		ResultEngineMessageTypes resultEngineMessageType;
		UserMessageMessageTypes userMessageMessageTypes;
		
		if ((friendManagerMessageType = message.getType(FriendManagerMessageTypes.class)) != null) {
			onFriendManagerMessage(friendManagerMessageType, context);
		} else if ((profileMessageType = message.getType(ProfileMessageTypes.class)) != null) {
			onProfileMessage(profileMessageType, context);
		} else if ((authMessageType = message.getType(AuthMessageTypes.class)) != null) {
			onAuthMessage(authMessageType, context);
		} else if ((paymentMessageType = message.getType(PaymentMessageTypes.class)) != null) {
			onPaymentMessage(paymentMessageType, context);
		} else if ((gameEngineMessageType = message.getType(GameEngineMessageTypes.class)) != null && context.getOriginalRequestInfo() != null) {
			onGameEngineMessage(gameEngineMessageType, context);
		} else if ((eventMessageTypes = message.getType(EventMessageTypes.class)) != null) {
			onEventMessage(eventMessageTypes, context);
		} else if ((userMessageMessageTypes = message.getType(UserMessageMessageTypes.class)) != null) {
			LOGGER.debug("Received UserMessageService message {}", message.getContent());
			onUserMessageMessage(userMessageMessageTypes, context);
		} else if ((resultEngineMessageType = message.getType(ResultEngineMessageTypes.class)) != null) {
			onResultEngineMessage(resultEngineMessageType, context);
		} else {
			throw new F4MValidationFailedException(String.format("Unrecognized message [%s]", message.getName()));
		}
		return null;
	}

	@Override
	public void onUserErrorMessage(RequestContext context) {
		JsonMessage<?> message = context.getMessage();

		ResultEngineMessageTypes resultEngineMessageType;
		GameEngineMessageTypes gameEngineMessageType;
		PaymentMessageTypes paymentMessageType;
		if ((resultEngineMessageType = message.getType(ResultEngineMessageTypes.class)) != null) {
			onResultEngineMessage(resultEngineMessageType, context);
		} else if ((gameEngineMessageType = message.getType(GameEngineMessageTypes.class)) != null) {
			onGameEngineMessage(gameEngineMessageType, context);
		} else if ((paymentMessageType = message.getType(PaymentMessageTypes.class)) != null) {
			onPaymentMessage(paymentMessageType, context);
		}
	}
	
	private void onUserMessageMessage(UserMessageMessageTypes messageType, RequestContext context) {
		RequestInfo originalRequestInfo = context.getOriginalRequestInfo();
		JsonMessageContent content = context.getMessage().getContent();
		switch (messageType) {
		case SEND_WEBSOCKET_MESSAGE_RESPONSE:
			break;
		case CANCEL_USER_PUSH_RESPONSE:
			onCancelledNotificationResponse((UserMessageRequestInfo) originalRequestInfo);
			break;
		case SEND_USER_PUSH_RESPONSE:
			onScheduledNotificationResponse((UserMessageRequestInfo) originalRequestInfo, (SendUserPushResponse) content);
			break;
		default:
			LOGGER.error("Unexpected message with type {}", messageType);
		}
	}

	private void onCancelledNotificationResponse(UserMessageRequestInfo originalRequestInfo) {
		String mgiId = originalRequestInfo.getMgiId();
		String userId = originalRequestInfo.getUserId();
		mgiManager.resetScheduledNotification(mgiId, userId);
	}

	private void onScheduledNotificationResponse(UserMessageRequestInfo originalRequestInfo, SendUserPushResponse content) {
		String mgiId = originalRequestInfo.getMgiId();
		String userId = originalRequestInfo.getUserId();
		String[] notificationIds = content.getNotificationIds();
		
		Stream.of(notificationIds)
				.filter(e -> e != null)
				.findFirst()
				.ifPresent(notificationId -> mgiManager.setScheduledNotification(mgiId, userId, notificationId));		
	}

	private void onFriendManagerMessage(FriendManagerMessageTypes messageType, RequestContext context) {
		RequestInfo originalRequestInfo = context.getOriginalRequestInfo();
		JsonMessageContent content = context.getMessage().getContent();

		switch (messageType) {
		case BUDDY_LIST_ALL_IDS_RESPONSE:
			onBuddyListAllIdsResponse((RequestInfoWithUserIdImpl) originalRequestInfo, (BuddyListAllIdsResponse) content);
			break;
		case GROUP_LIST_PLAYERS_RESPONSE:
			onGroupListPlayersResponse((InviteRequestInfoImpl) originalRequestInfo, (GroupListPlayersResponse) content, originalRequestInfo.getSourceMessage().getClientInfo());
			break;
		case BUDDY_ADD_FOR_USER_RESPONSE:
			onBuddyAddForUserResponse((InviteRequestInfoImpl) originalRequestInfo);
			break;
		default:
			throw new F4MValidationFailedException(String.format("Unsupported message type [%s]", messageType));
		}
	}

	@SuppressWarnings("unchecked")
	private void onBuddyListAllIdsResponse(RequestInfoWithUserIdImpl requestInfo, BuddyListAllIdsResponse content) {
		String[] userIds = content.getUserIds();
		JsonMessage<GetGameListRequest> originalGetGameListRequest = (JsonMessage<GetGameListRequest>) requestInfo.getSourceMessage();

		if (originalGetGameListRequest.getContent().isByFriendPlayed()) {
			requestBuddiesPlayedGames(requestInfo, userIds);
		} else {
			GetGameListResponse response = getGameList(originalGetGameListRequest, requestInfo.getUserId(), userIds, new String[0]);
			this.sendResponse(requestInfo.getSourceMessage(), response, requestInfo.getSourceSession());
		}
	}
	
	private void requestBuddiesPlayedGames(RequestInfoWithUserIdImpl originalRequestInfo, String[] userIds) {
		FriendsGameListRequestInfoImpl requestInfo = new FriendsGameListRequestInfoImpl(
				originalRequestInfo.getSourceMessage(), originalRequestInfo.getSourceSession(),
				originalRequestInfo.getUserId());
		requestInfo.setFriends(Arrays.asList(userIds));

		profileCommunicator.requestGetProfileBlob(requestInfo);
	}
	
	private void onGroupListPlayersResponse(InviteRequestInfoImpl requestInfo, GroupListPlayersResponse content, ClientInfo clientInfo) {
		InviteGroupToGameRequest sourceRequest = (InviteGroupToGameRequest) requestInfo.getSourceMessage().getContent();
		List<String> inviteeIds = content.getUserIds();
		mgiManager.validateInvitation(sourceRequest.getMultiplayerGameParameters(), sourceRequest.getMultiplayerGameInstanceId(), inviteeIds);
		inviteeIds = mgiManager.ensureHasNoBlocking(clientInfo.getUserId(), inviteeIds, false);

		List<String> invitedUsers = mgiManager.addUsers(inviteeIds, requestInfo.getMgiId(), requestInfo.getUserId());

		InviteGroupToGameResponse response = new InviteGroupToGameResponse(requestInfo.getGameInstanceId(),
				requestInfo.getMgiId(), invitedUsers);

		//Add invitee statistic event
		invitedUsers.stream().forEach(userId -> {
			ClientInfo inviteeClientInfo = new ClientInfo(clientInfo.getTenantId(), clientInfo.getAppId(), userId, clientInfo.getIp(), clientInfo.getHandicap());
			InviteEvent inviteEvent = new InviteEvent();
			inviteEvent.setInvitedFromFriends(true);
			tracker.addEvent(inviteeClientInfo, inviteEvent);
		});

		//Add inviter statistic event
		ClientInfo inviterClientInfo =  new ClientInfo(clientInfo.getTenantId(), clientInfo.getAppId(), requestInfo.getUserId(), clientInfo.getIp(), clientInfo.getHandicap());
		InviteEvent inviteEvent = new InviteEvent();
		inviteEvent.setFriendsInvitedToo(true);
		tracker.addEvent(inviterClientInfo, inviteEvent);

		this.sendResponse(requestInfo.getSourceMessage(), response, requestInfo.getSourceSession());
	}
	
	private void onBuddyAddForUserResponse(InviteRequestInfoImpl requestInfo) {
		RespondToInvitationResponse response = new RespondToInvitationResponse(requestInfo.getGameInstanceId());
		this.sendResponse(requestInfo.getSourceMessage(), response, requestInfo.getSourceSession());
	}
	
	private void onProfileMessage(ProfileMessageTypes messageType, RequestContext context) {
		RequestInfo originalRequestInfo = context.getOriginalRequestInfo();
		JsonMessageContent content = context.getMessage().getContent();
		
		switch (messageType) {
		case GET_PROFILE_BLOB_RESPONSE:
			onGetProfileBlobResponse((FriendsGameListRequestInfoImpl) originalRequestInfo, (GetProfileBlobResponse) content);
			break;
		default:
			throw new F4MValidationFailedException(String.format("Unsupported message type [%s]", messageType));
		}
	}

	@SuppressWarnings("unchecked")
	private void onGetProfileBlobResponse(FriendsGameListRequestInfoImpl requestInfo, GetProfileBlobResponse content) {
		JsonArray gamesJsonArray = content.getValue().getAsJsonArray();
		requestInfo.addGames(getGames(gamesJsonArray));

		if (requestInfo.hasNextFriend()) {
			profileCommunicator.requestGetProfileBlob(requestInfo);
		} else {
			JsonMessage<GetGameListRequest> originalGetGameListRequest = (JsonMessage<GetGameListRequest>) requestInfo.getSourceMessage();
			String userId = requestInfo.getUserId();
			String[] friends = requestInfo.getFriendsAsArray();
			String[] games = requestInfo.getGamesAsArray();

			GetGameListResponse response = getGameList(originalGetGameListRequest, userId, friends, games);
			this.sendResponse(requestInfo.getSourceMessage(), response, requestInfo.getSourceSession());
		}
	}

	private List<String> getGames(JsonArray jsonArray) {
		List<String> games = new ArrayList<>();
		for (JsonElement gameIdElement : jsonArray) {
			games.add(gameIdElement.getAsString());
		}
		return games;
	}
	
	private GetGameListResponse getGameList(JsonMessage<GetGameListRequest> originalGetGameListRequestMessage, String userId,
			String[] friends, String[] games) {
		JsonElement gameListRequestJson = jsonMessageUtil.toJsonElement(originalGetGameListRequestMessage.getContent());
		String appId = originalGetGameListRequestMessage.getClientInfo().getAppId();
		String tenantId = originalGetGameListRequestMessage.getClientInfo().getTenantId();
		GameFilter gameFilter = new GameFilter(tenantId, appId, originalGetGameListRequestMessage.getClientInfo().getCountryCodeAsString(), gameListRequestJson);
		gameFilter.setFriends(friends);
		gameFilter.setGames(games);

		return gameSelector.getGameList(userId, gameFilter);
	}

	private void onAuthMessage(AuthMessageTypes messageType, RequestContext context) {
		RequestInfo originalRequestInfo = context.getOriginalRequestInfo();
		JsonMessageContent content = context.getMessage().getContent();
		
		switch (messageType) {
		case INVITE_USER_BY_EMAIL_RESPONSE:
			onRegisterEmailResponse((InviteRequestInfoImpl) originalRequestInfo, (InviteUserByEmailResponse) content, originalRequestInfo.getSourceMessage().getClientInfo());
			break;
		default:
			throw new F4MValidationFailedException(String.format("Unsupported message type [%s]", messageType));
		}
	}

	private void onRegisterEmailResponse(InviteRequestInfoImpl requestInfo, InviteUserByEmailResponse inviteUserByEmailResponse, ClientInfo clientInfo) {
		InviteExternalPersonToGameResponse response = new InviteExternalPersonToGameResponse();
				
		String personId = inviteUserByEmailResponse.getUserId();
		if (mgiManager.addUser(personId, requestInfo.getMgiId(), requestInfo.getUserId())) {
			response.setPersonId(personId);
		}

		//Add invitee statistic event
		ClientInfo inviteeClientInfo =  new ClientInfo(clientInfo.getTenantId(), clientInfo.getAppId(), inviteUserByEmailResponse.getUserId(), clientInfo.getIp(), clientInfo.getHandicap());
		InviteEvent inviteEvent = new InviteEvent();
		inviteEvent.setInvitedFromFriends(true);
		tracker.addEvent(inviteeClientInfo, inviteEvent);

		//Add inviter statistic event
		ClientInfo inviterClientInfo =  new ClientInfo(clientInfo.getTenantId(), clientInfo.getAppId(), requestInfo.getUserId(), clientInfo.getIp(), clientInfo.getHandicap());
		inviteEvent = new InviteEvent();
		inviteEvent.setFriendsInvitedToo(true);
		tracker.addEvent(inviterClientInfo, inviteEvent);

		response.setGameInstanceId(requestInfo.getGameInstanceId());
		response.setMultiplayerGameInstanceId(requestInfo.getMgiId());
		this.sendResponse(requestInfo.getSourceMessage(), response, requestInfo.getSourceSession());
	}

	private void onPaymentMessage(PaymentMessageTypes messageType, RequestContext context) {
		
		switch (messageType) {
		case CREATE_JACKPOT_RESPONSE:
			PaymentCreateJackpotRequestInfo originalRequestInfo = context.getOriginalRequestInfo();
			if (context.getMessage().getError() == null) {
				onCreateJackpotResponse(originalRequestInfo);
			} else {
				onPublicGameCreationError(originalRequestInfo, originalRequestInfo.getMgiId());
			}
			break;
		case GET_JACKPOT_RESPONSE:
			PaymentGetJackpotRequestInfo originalRequestInfoGetRequest = context.getOriginalRequestInfo();
			if (originalRequestInfoGetRequest != null  && originalRequestInfoGetRequest.getNumberOfExpectedResponses() == null){
				processGetJackpot(context, this::onGetJackpotResponse);
			} else {
				processGetJackpot(context, this::onGetJackpotResponseList);
			}
			break;
		case TRANSFER_JACKPOT_RESPONSE:
			PaymentRequestInformation originalRequestInformation = context.getOriginalRequestInfo();
		default:
			throw new F4MValidationFailedException(String.format("Unsupported message type [%s]", messageType));
		}
	}

	private void onCreateJackpotResponse(PaymentCreateJackpotRequestInfo originalRequestInfo) {
		if (originalRequestInfo.getSourceMessage() != null) {
			//follow flow of MultiplayerGameInstanceManagerImpl.createMultiplayerGameInstance - after jackpot do register
			gameEngineCommunicator.requestRegister(originalRequestInfo.getSourceMessage(),
					originalRequestInfo.getSourceSession(), originalRequestInfo.getMgiId());
		} else {
			handlePublicGameCreationActions(originalRequestInfo.getMgiId());
		}
	}

	private void processGetJackpot(RequestContext context, Consumer<RequestContext> getJackpotProcessor) {
		
		JsonMessage<? extends JsonMessageContent> message = context.getMessage();
		PaymentGetJackpotRequestInfo originalRequestInfo = context.getOriginalRequestInfo();
		if (originalRequestInfo != null) {
			getJackpotProcessor.accept(context);
		} else {
			LOGGER.error("OriginalRequestInfo not found for {}", message.getContent());
		}
		
	}

	private void onGetJackpotResponseList(RequestContext context) {

		PaymentGetJackpotRequestInfo originalRequestInfo =context.getOriginalRequestInfo();
		JsonMessage<GetJackpotResponse> message = context.getMessage();
		AtomicInteger numberOfExpectedResponsesRemaining = originalRequestInfo.getNumberOfExpectedResponses();

		if (message.getError() == null) {
			fillEntryWithJackpot(originalRequestInfo, message);
		} else {
			LOGGER.error("Error getting jackot, code: {}, type : {}  ", message.getError().getCode(), message.getError().getType() );
		}

		if (numberOfExpectedResponsesRemaining != null) {
			int remaining = numberOfExpectedResponsesRemaining.decrementAndGet();
			if (remaining == 0) {
				LOGGER.debug("Forwarding {} to user, all expected payment responses received",
						originalRequestInfo.getResponseToForward());
				this.sendResponse(originalRequestInfo.getSourceMessage(), originalRequestInfo.getResponseToForward(),
						originalRequestInfo.getSourceSession());
			} else {
				LOGGER.debug(
						"Not forwarding GetJackpotResponseList to user, waiting for {} more responses from payment",
						remaining);
			}
		} else {
			LOGGER.error("Response to user was already sent for message {}", message);
		}
	}

	private void fillEntryWithJackpot(PaymentGetJackpotRequestInfo originalRequestInfo,
			JsonMessage<GetJackpotResponse> message) {
		if (originalRequestInfo.getResponseToForward() instanceof GetGameListResponse) {
			Game gameToChange = originalRequestInfo.getGame();
			gameToChange.setJackpot(new Jackpot(message.getContent().getBalance(), message.getContent().getCurrency()));
			GetGameListResponse resp = (GetGameListResponse) originalRequestInfo.getResponseToForward();
			JsonArray moddedGames = jackpotDataGetter.modifyGameListWithJackpot(resp.getGames(), gameToChange);
			resp.setGames(moddedGames);
			originalRequestInfo.setResponseToForward(resp);
		} else if (originalRequestInfo.getResponseToForward() instanceof PublicGameListResponse) {
			Invitation invitationToChange = originalRequestInfo.getInvitation();
			invitationToChange.getGame()
					.setJackpot(new Jackpot(message.getContent().getBalance(), message.getContent().getCurrency()));

			PublicGameListResponse resp = (PublicGameListResponse) originalRequestInfo.getResponseToForward();
			List<Invitation> items = jackpotDataGetter.modifyInvitationGameListWithJackpot(resp.getItems(),
					invitationToChange);
			resp.setItems(items);
			originalRequestInfo.setResponseToForward(resp);
		} else if (originalRequestInfo.getResponseToForward() instanceof InvitationListResponse) {
			Invitation invitationToChange = originalRequestInfo.getInvitation();
			invitationToChange.getGame()
					.setJackpot(new Jackpot(message.getContent().getBalance(), message.getContent().getCurrency()));
			InvitationListResponse resp = (InvitationListResponse) originalRequestInfo.getResponseToForward();
			List<Invitation> items = jackpotDataGetter.modifyInvitationGameListWithJackpot(resp.getItems(),
					invitationToChange);
			resp.setItems(items);
			originalRequestInfo.setResponseToForward(resp);
		}
	}
	
	private void onGetJackpotResponse(RequestContext context) {
		PaymentGetJackpotRequestInfo originalRequestInfo = context.getOriginalRequestInfo();
		JsonMessage<GetJackpotResponse> message = context.getMessage();
		GetGameResponse response = new GetGameResponse(originalRequestInfo.getGame());
		
		if (context.getMessage().getError() == null) {
			response.getGame()
					.setJackpot(new Jackpot(message.getContent().getBalance(), message.getContent().getCurrency()));
		} else {
			LOGGER.error("Error getting jackpot request code: {}, type: {}", context.getMessage().getError().getCode(),
					context.getMessage().getError().getType());
		}
			
		this.sendResponse(originalRequestInfo.getSourceMessage(), response, originalRequestInfo.getSourceSession());
	}
	
	
	private void onGameEngineMessage(GameEngineMessageTypes messageType, RequestContext context) {
		JsonMessageContent content = context.getMessage().getContent();

		switch (messageType) {
		case REGISTER_RESPONSE:
			InviteRequestInfoImpl originalRequestInfo = context.getOriginalRequestInfo();
			if (context.getMessage().getError() == null) {
				onRegisterResponse(originalRequestInfo, (RegisterResponse) content, originalRequestInfo.getSourceMessage().getClientInfo());
			} else {
				onPublicGameCreationError(originalRequestInfo, originalRequestInfo.getMgiId());
			}
			break;
		default:
			throw new F4MValidationFailedException(String.format("Unsupported message type [%s]", messageType));
		}
	}
	
	private void onRegisterResponse(InviteRequestInfoImpl requestInfo, RegisterResponse content, ClientInfo clientInfo) {
		GameSelectionMessageTypes sourceMessageType = requestInfo.getSourceMessage()
				.getType(GameSelectionMessageTypes.class);
		switch (sourceMessageType) {
		case INVITE_USERS_TO_GAME:
			handlePublicGameCreationActions(requestInfo.getMgiId());
			onInviteUsersToGame(requestInfo, content, clientInfo);
			break;
		case INVITE_EXTERNAL_PERSON_TO_GAME:
			handlePublicGameCreationActions(requestInfo.getMgiId());
			requestInfo.setGameInstanceId(content.getGameInstanceId());
			requestInviteUserByEmail(requestInfo);
			break;
		case INVITE_GROUP_TO_GAME:
			handlePublicGameCreationActions(requestInfo.getMgiId());
			onInviteGroupToGame(requestInfo, content);
			break;
		case CREATE_PUBLIC_GAME:
			handlePublicGameCreationActions(requestInfo.getMgiId());
			onCreatePublicGame(requestInfo, content);
			break;
		case RESPOND_TO_INVITATION:
			// handlePublicGameCreationActions not needed here, MGI was already created
			onRespondToInvitation(requestInfo, content);
			break;
		case JOIN_MULTIPLAYER_GAME:
			// handlePublicGameCreationActions not needed here, MGI was already created
			onJoinMultiplayerGame(requestInfo, content);
			break;
		default:
			throw new F4MValidationFailedException(String.format("Unsupported source message type [%s]", sourceMessageType));
		}
	}

	/**
	 * Follow flow of MultiplayerGameInstanceManagerImpl.createMultiplayerGameInstance:
	 * 1) jackpot (already done or not needed)
	 * 2) register (already done or not needed)
	 * 3) create public game (being done here)
	 * @param mgiId
	 */
	private void handlePublicGameCreationActions(String mgiId) {
		mgiManager.addPublicGameToElastic(mgiId);
	}

	private void onPublicGameCreationError(RequestInfo requestInfo, String mgiId) {
		GameSelectionMessageTypes sourceMessageType = requestInfo.getSourceMessage()
				.getType(GameSelectionMessageTypes.class);
		switch (sourceMessageType) {
		case INVITE_USERS_TO_GAME:
		case INVITE_EXTERNAL_PERSON_TO_GAME:
		case INVITE_GROUP_TO_GAME:
		case CREATE_PUBLIC_GAME:
			mgiManager.setMultiplayerGameInstanceAsExpired(mgiId);
			break;
		default:
			// Nothing to do.
			break;
		}
	}

	private void onInviteUsersToGame(InviteRequestInfoImpl requestInfo, RegisterResponse content, ClientInfo clientInfo) {
		InviteUsersToGameResponse response = new InviteUsersToGameResponse();

		InviteUsersToGameRequest sourceRequest = (InviteUsersToGameRequest) requestInfo.getSourceMessage().getContent();
		mgiManager.validateInvitation(sourceRequest.getMultiplayerGameParameters(),
				sourceRequest.getMultiplayerGameInstanceId(), sourceRequest.getUsersIds());
		List<String> invitedUsers = mgiManager.addUsers(sourceRequest.getUsersIds(), requestInfo.getMgiId(),
				requestInfo.getUserId());
		mgiManager.updateLastInvitedAndPausedDuelOpponents(requestInfo.getMgiId(), requestInfo.getUserId(),
				sourceRequest.getUsersIds(), sourceRequest.getPausedUsersIds());

		response.setGameInstanceId(content.getGameInstanceId());
		response.setMultiplayerGameInstanceId(requestInfo.getMgiId());
		response.setUsersIds(invitedUsers);
		GameType gameType = mgiManager.getGameType(sourceRequest.getMultiplayerGameParameters(),
				sourceRequest.getMultiplayerGameInstanceId());
		if (gameType.isDuel()) {
			response.setPausedUsersIds(sourceRequest.getPausedUsersIds());
		} else if (GameType.USER_LIVE_TOURNAMENT.equals(gameType)) {
			String topic = Game.getStartMultiplayerGameTopic(requestInfo.getMgiId());
			eventServiceClient.publish(topic, true, 
					jsonUtil.toJsonElement(new StartLiveUserTournamentEvent(requestInfo.getMgiId(),
							sourceRequest.getMultiplayerGameParameters().getPlayDateTime())),
					sourceRequest.getMultiplayerGameParameters().getPlayDateTime());
			// Activate invitations
			mgiManager.activateInvitationsAndSendInviteNotifications(requestInfo.getMgiId(), clientInfo);			
		}

		//Add invitee statistic event
		invitedUsers.forEach(userId -> {
			ClientInfo inviteeClientInfo = new ClientInfo(clientInfo.getTenantId(), clientInfo.getAppId(), userId, clientInfo.getIp(), clientInfo.getHandicap());
			InviteEvent inviteEvent = new InviteEvent();
			inviteEvent.setInvitedFromFriends(true);
			tracker.addEvent(inviteeClientInfo, inviteEvent);
		});

		//Add inviter statistic event
		ClientInfo inviterClientInfo = new ClientInfo(clientInfo.getTenantId(), clientInfo.getAppId(), requestInfo.getUserId(), clientInfo.getIp(), clientInfo.getHandicap());
		InviteEvent inviteEvent = new InviteEvent();
		inviteEvent.setFriendsInvitedToo(true);
		tracker.addEvent(inviterClientInfo, inviteEvent);

		this.sendResponse(requestInfo.getSourceMessage(), response, requestInfo.getSourceSession());
	}

	private void requestInviteUserByEmail(InviteRequestInfoImpl requestInfo) {
		InviteExternalPersonToGameRequest sourceRequest = (InviteExternalPersonToGameRequest) requestInfo
				.getSourceMessage().getContent();
		mgiManager.validateInvitation(sourceRequest.getMultiplayerGameParameters(),
				sourceRequest.getMultiplayerGameInstanceId(), Arrays.asList(sourceRequest.getEmail()));
		authServiceCommunicator.requestInviteUserByEmail(requestInfo.getSourceMessage(), requestInfo.getSourceSession(),
				sourceRequest.getEmail(), requestInfo.getMgiId(), requestInfo.getGameInstanceId());
	}
	
	@SuppressWarnings("unchecked")
	private void onInviteGroupToGame(InviteRequestInfoImpl requestInfo, RegisterResponse content) {
		JsonMessage<InviteGroupToGameRequest> sourceMessage = (JsonMessage<InviteGroupToGameRequest>) requestInfo.getSourceMessage();
		friendManagerCommunicator.requestGroupListPlayers(sourceMessage, requestInfo.getSourceSession(),
				requestInfo.getMgiId(), content.getGameInstanceId());
	}

	private void onCreatePublicGame(InviteRequestInfoImpl requestInfo, RegisterResponse content) {
		CreatePublicGameResponse response = new CreatePublicGameResponse();
		response.setGameInstanceId(content.getGameInstanceId());
		response.setMultiplayerGameInstanceId(requestInfo.getMgiId());
		this.sendResponse(requestInfo.getSourceMessage(), response, requestInfo.getSourceSession());
	}

	private void onRespondToInvitation(InviteRequestInfoImpl requestInfo, RegisterResponse content) {
		String userId = requestInfo.getUserId();
		String mgiId = requestInfo.getMgiId();
		String appId = requestInfo.getSourceMessage().getAppId();
		ClientInfo clientInfo = ClientInfo.cloneOf(requestInfo.getSourceMessage().getClientInfo());
		mgiManager.acceptedInvitation(userId, mgiId, clientInfo);
		if (mgiManager.getPublicGame(appId, mgiId) != null && isDuel(mgiId)) {
			mgiManager.deletePublicGameSilently(mgiId);
		}
		
		requestInfo.setGameInstanceId(content.getGameInstanceId());
		boolean waitForResponse = addInviterAsBuddy(requestInfo, userId, mgiId);
		
		if (!waitForResponse) {
			RespondToInvitationResponse response = new RespondToInvitationResponse(content.getGameInstanceId());
			this.sendResponse(requestInfo.getSourceMessage(), response, requestInfo.getSourceSession());
		}
	}

	private void onJoinMultiplayerGame(InviteRequestInfoImpl requestInfo, RegisterResponse content) {
		JoinMultiplayerGameResponse response = new JoinMultiplayerGameResponse(content.getGameInstanceId());
		String appId = requestInfo.getSourceMessage().getAppId();
		String mgiId = requestInfo.getMgiId();
		if (mgiManager.getPublicGame(appId, mgiId) != null && isDuel(mgiId)) {
			mgiManager.deletePublicGameSilently(mgiId);
		}
		
		this.sendResponse(requestInfo.getSourceMessage(), response, requestInfo.getSourceSession());
	}
	
	private boolean isDuel(String mgiId) {
		CustomGameConfig multiplayerGameConfig = mgiManager.getMultiplayerGameConfig(mgiId);
		return multiplayerGameConfig.getGameType().isDuel();
	}
	
	private boolean addInviterAsBuddy(InviteRequestInfoImpl requestInfo, String userId, String mgiId) {
		boolean waitForResponse = false;
		String inviterId = mgiManager.getInviterId(userId, mgiId);
		if (!StringUtils.equals(inviterId, userId)) {
			friendManagerCommunicator.requestAddBuddyForUser(requestInfo, inviterId, userId);
			waitForResponse = true;
		}
		return waitForResponse;
	}
	
	private void onEventMessage(EventMessageTypes messageType, RequestContext context) {
		JsonMessageContent content = context.getMessage().getContent();
		
		switch (messageType) {
		case SUBSCRIBE_RESPONSE:
			onSubscribeResponse((SubscribeResponse) content);
			break;
		case NOTIFY_SUBSCRIBER:
			onNotifySubscriber((NotifySubscriberMessageContent) content);
			break;
		default:
			LOGGER.error("Unexpected message with type {}", messageType);
		}
	}
	
	private void onSubscribeResponse(SubscribeResponse content) {
		if (!StringUtils.isBlank(content.getTopic())) {
			subscriptionManager.updateSubscriptionId(content.isVirtual(), content.getConsumerName(), content.getTopic(), content.getSubscription());
		}
	}
	
	private void onNotifySubscriber(NotifySubscriberMessageContent notificationMessage) {
		String topic = notificationMessage.getTopic();
		if (Game.isTournamentTopic(topic)) {
			processTournamentTopic(notificationMessage);
		} else if (ProfileMergeEvent.PROFILE_MERGE_EVENT_TOPIC.equals(topic)) {
			ProfileMergeEvent event = new ProfileMergeEvent(notificationMessage.getNotificationContent().getAsJsonObject());
			mergeProfiles(event.getSourceUserId(), event.getTargetUserId());
		} else {
			LOGGER.error("Failed to process unrecognized topic [{}]", topic);
		}
	}

	private void mergeProfiles(String sourceUserId, String targetUserId) {
		Profile profile = Optional.ofNullable(profileDao.getProfile(sourceUserId))
				.orElse(profileDao.getProfile(targetUserId));
		if (CollectionUtils.isNotEmpty(profile.getTenants())) {
			profile.getTenants().forEach(id -> {
				mgiManager.moveMultiplayerGameInstanceData(id, sourceUserId, targetUserId);
				dashboardManager.moveDashboardData(id, sourceUserId, targetUserId);
			});
			mgiManager.movePublicGameData(sourceUserId, targetUserId);
		} else {
			LOGGER.warn("Tenants not found; sourceUserId [{}]; targetUserId [{}]", sourceUserId, targetUserId);
		}
	}

	private void processTournamentTopic(NotifySubscriberMessageContent notificationContent) {
		try {
			subscriptionManager.processEventServiceNotifyMessage(notificationContent.getTopic(), 
					notificationContent.getNotificationContent());
		} catch (Exception e) {
			LOGGER.error("Failed to process Notify Message [{}]", notificationContent, e);
		}
	}

	private void onResultEngineMessage(ResultEngineMessageTypes messageType, RequestContext context) {
		ResultEngineRequestInfo requestInfo = context.getOriginalRequestInfo();
		JsonMessageContent content = context.getMessage().getContent();

		switch (messageType) {
		case GET_MULTIPLAYER_RESULTS_RESPONSE:
			if (context.getMessage().getError() == null) {
				onGetMultiplayerResultsResponse((GetMultiplayerResultsResponse) content, requestInfo);
			} else {
				onGetMultiplayerResultsResponseWithError(requestInfo);
			}
			break;
		default:
			throw new F4MValidationFailedException(String.format("Unsupported message type [%s]", messageType));
		}
	}

	private void onGetMultiplayerResultsResponse(GetMultiplayerResultsResponse content,
			ResultEngineRequestInfo requestInfo) {
		GetDashboardResponse dashboard = requestInfo.getDashboard();
		PlayedGameInfo lastTournament = dashboard.getUserTournament().getLastTournament();
		lastTournament.getTournamentInfo().setPlacement(content.getPlace());

		JsonMessage<? extends JsonMessageContent> sourceMessage = requestInfo.getSourceMessage();
		dashboardManager.updatePlayedGame(sourceMessage.getTenantId(), sourceMessage.getUserId(), lastTournament);

		this.sendResponse(sourceMessage, dashboard, requestInfo.getSourceSession());
	}

	private void onGetMultiplayerResultsResponseWithError(ResultEngineRequestInfo requestInfo) {
		GetDashboardResponse response = requestInfo.getDashboard();
		this.sendResponse(requestInfo.getSourceMessage(), response, requestInfo.getSourceSession());
	}

}
