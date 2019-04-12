package de.ascendro.f4m.service.friend.server;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import de.ascendro.f4m.server.analytics.model.InviteEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.PlayerListOrderType;
import de.ascendro.f4m.server.profile.model.AppConfig;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.friend.BuddyManager;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypes;
import de.ascendro.f4m.service.friend.client.PaymentServiceCommunicator;
import de.ascendro.f4m.service.friend.client.UserActionPayoutRequestInfo;
import de.ascendro.f4m.service.friend.model.Buddy;
import de.ascendro.f4m.service.friend.model.Contact;
import de.ascendro.f4m.service.friend.model.Group;
import de.ascendro.f4m.service.friend.model.api.ApiBuddy;
import de.ascendro.f4m.service.friend.model.api.ApiContact;
import de.ascendro.f4m.service.friend.model.api.ApiGroup;
import de.ascendro.f4m.service.friend.model.api.ApiProfile;
import de.ascendro.f4m.service.friend.model.api.AppRatedResponse;
import de.ascendro.f4m.service.friend.model.api.AppSharedResponse;
import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.friend.model.api.DailyLoginResponse;
import de.ascendro.f4m.service.friend.model.api.ElasticResyncResponse;
import de.ascendro.f4m.service.friend.model.api.LastDuelInvitationListResponse;
import de.ascendro.f4m.service.friend.model.api.LeaderboardPositionRequest;
import de.ascendro.f4m.service.friend.model.api.LeaderboardPositionResponse;
import de.ascendro.f4m.service.friend.model.api.MoveBuddiesRequest;
import de.ascendro.f4m.service.friend.model.api.MoveContactsRequest;
import de.ascendro.f4m.service.friend.model.api.MoveGroupsRequest;
import de.ascendro.f4m.service.friend.model.api.buddy.ApiBuddyListResult;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyAddForUserRequest;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyAddRequest;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyLeaderboardListRequest;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyLeaderboardListResponse;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListAllIdsRequest;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListAllIdsResponse;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListOrderType;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListRequest;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListResponse;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyRemoveRequest;
import de.ascendro.f4m.service.friend.model.api.contact.ApiContactListResult;
import de.ascendro.f4m.service.friend.model.api.contact.ApiPhonebookEntry;
import de.ascendro.f4m.service.friend.model.api.contact.ContactImportFacebookRequest;
import de.ascendro.f4m.service.friend.model.api.contact.ContactImportFacebookResponse;
import de.ascendro.f4m.service.friend.model.api.contact.ContactImportPhonebookRequest;
import de.ascendro.f4m.service.friend.model.api.contact.ContactImportPhonebookResponse;
import de.ascendro.f4m.service.friend.model.api.contact.ContactInviteNewRequest;
import de.ascendro.f4m.service.friend.model.api.contact.ContactInviteRequest;
import de.ascendro.f4m.service.friend.model.api.contact.ContactInviteResponse;
import de.ascendro.f4m.service.friend.model.api.contact.ContactListRequest;
import de.ascendro.f4m.service.friend.model.api.contact.ContactListResponse;
import de.ascendro.f4m.service.friend.model.api.contact.ContactRemoveInvitationRequest;
import de.ascendro.f4m.service.friend.model.api.contact.ContactResendInvitationRequest;
import de.ascendro.f4m.service.friend.model.api.contact.ContactResendInvitationResponse;
import de.ascendro.f4m.service.friend.model.api.contact.ResyncRequest;
import de.ascendro.f4m.service.friend.model.api.group.GroupAddPlayersRequest;
import de.ascendro.f4m.service.friend.model.api.group.GroupAddPlayersResponse;
import de.ascendro.f4m.service.friend.model.api.group.GroupBlockRequest;
import de.ascendro.f4m.service.friend.model.api.group.GroupCreateRequest;
import de.ascendro.f4m.service.friend.model.api.group.GroupCreateResponse;
import de.ascendro.f4m.service.friend.model.api.group.GroupDeleteRequest;
import de.ascendro.f4m.service.friend.model.api.group.GroupGetRequest;
import de.ascendro.f4m.service.friend.model.api.group.GroupGetResponse;
import de.ascendro.f4m.service.friend.model.api.group.GroupLeaveRequest;
import de.ascendro.f4m.service.friend.model.api.group.GroupListMemberOfRequest;
import de.ascendro.f4m.service.friend.model.api.group.GroupListMemberOfResponse;
import de.ascendro.f4m.service.friend.model.api.group.GroupListPlayersRequest;
import de.ascendro.f4m.service.friend.model.api.group.GroupListPlayersResponse;
import de.ascendro.f4m.service.friend.model.api.group.GroupListRequest;
import de.ascendro.f4m.service.friend.model.api.group.GroupListResponse;
import de.ascendro.f4m.service.friend.model.api.group.GroupRemovePlayersRequest;
import de.ascendro.f4m.service.friend.model.api.group.GroupRemovePlayersResponse;
import de.ascendro.f4m.service.friend.model.api.group.GroupUnblockRequest;
import de.ascendro.f4m.service.friend.model.api.group.GroupUpdateRequest;
import de.ascendro.f4m.service.friend.model.api.group.GroupUpdateResponse;
import de.ascendro.f4m.service.friend.model.api.group.PlayerIsGroupMemberRequest;
import de.ascendro.f4m.service.friend.model.api.group.PlayerIsGroupMemberResponse;
import de.ascendro.f4m.service.friend.model.api.player.ApiPlayerListResult;
import de.ascendro.f4m.service.friend.model.api.player.PlayerBlockRequest;
import de.ascendro.f4m.service.friend.model.api.player.PlayerLeaderboardListRequest;
import de.ascendro.f4m.service.friend.model.api.player.PlayerLeaderboardListResponse;
import de.ascendro.f4m.service.friend.model.api.player.PlayerListRequest;
import de.ascendro.f4m.service.friend.model.api.player.PlayerListResponse;
import de.ascendro.f4m.service.friend.model.api.player.PlayerUnblockRequest;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class FriendManagerServiceServerMessageHandler extends DefaultJsonMessageHandler {

	private static final String DAILY_LOGIN_PAYOUT_REASON = "Payout bonus on daily login";
	private static final String APP_RATED_PAYOUT_REASON = "Payout bonus on app rating";
	private static final String APP_SHARED_PAYOUT_REASON = "Payout bonus on app sharing";

	private final BuddyManager buddyManager;
	private final CommonProfileAerospikeDao profileDao;
	private final Tracker tracker;
	private final PaymentServiceCommunicator paymentServiceCommunicator;
	private final CommonProfileAerospikeDao commonProfileAerospikeDao;
	private ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao;

	public FriendManagerServiceServerMessageHandler(BuddyManager buddyManager, CommonProfileAerospikeDao profileDao,
			Tracker tracker, PaymentServiceCommunicator paymentServiceCommunicator,
			CommonProfileAerospikeDao commonProfileAerospikeDao,
			ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao) {
		this.buddyManager = buddyManager;
		this.profileDao = profileDao;
		this.tracker = tracker;
		this.paymentServiceCommunicator = paymentServiceCommunicator;
		this.commonProfileAerospikeDao = commonProfileAerospikeDao;
		this.applicationConfigurationAerospikeDao = applicationConfigurationAerospikeDao;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> message) {
		Validate.notNull(message, "Message is mandatory");
		FriendManagerMessageTypes type = message.getType(FriendManagerMessageTypes.class);
		ClientInfo clientInfo = message.getClientInfo();
		if (type != null) {
			switch (type) {
			case CONTACT_IMPORT_FACEBOOK:
				return onContactImportFacebook((JsonMessage<ContactImportFacebookRequest>) message);
			case CONTACT_IMPORT_PHONEBOOK:
				return onContactImportPhonebook((JsonMessage<ContactImportPhonebookRequest>) message);
			case CONTACT_LIST:
				return onContactList((JsonMessage<ContactListRequest>) message);
			case CONTACT_INVITE:
				return onContactInvite((JsonMessage<ContactInviteRequest>) message);
			case CONTACT_INVITE_NEW:
				return onContactInviteNew((JsonMessage<ContactInviteNewRequest>) message);
			case CONTACT_REMOVE_INVITATION:
				return onContactRemoveInvitation((JsonMessage<ContactRemoveInvitationRequest>) message);
			case CONTACT_RESEND_INVITATION:
				return onContactResendInvitation((JsonMessage<ContactResendInvitationRequest>) message);
			case RESYNC:
				return onResync((JsonMessage<ResyncRequest>) message);
			case PLAYER_BLOCK:
				return onPlayerBlock((JsonMessage<PlayerBlockRequest>) message);
			case PLAYER_UNBLOCK:
				return onPlayerUnblock((JsonMessage<PlayerUnblockRequest>) message);
			case PLAYER_LIST:
				return onPlayerList((JsonMessage<PlayerListRequest>) message);
			case PLAYER_LEADERBOARD_LIST:
				return onPlayerLeaderboardList((JsonMessage<PlayerLeaderboardListRequest>) message);
			case BUDDY_ADD:
				return onBuddyAdd((JsonMessage<BuddyAddRequest>) message);
			case BUDDY_ADD_FOR_USER:
				return onBuddyAddForUser((JsonMessage<BuddyAddForUserRequest>) message);
			case BUDDY_REMOVE:
				return onBuddyRemove((JsonMessage<BuddyRemoveRequest>) message);
			case BUDDY_LIST:
				return onBuddyList((JsonMessage<BuddyListRequest>) message);
			case BUDDY_LEADERBOARD_LIST:
				return onBuddyLeaderboardList((JsonMessage<BuddyLeaderboardListRequest>) message);
			case BUDDY_LIST_ALL_IDS:
				return onBuddyListAllIds((JsonMessage<BuddyListAllIdsRequest>) message);
			case GROUP_CREATE:
				return onGroupCreate((JsonMessage<GroupCreateRequest>) message, clientInfo);
			case GROUP_GET:
				return onGroupGet((JsonMessage<GroupGetRequest>) message);
			case GROUP_UPDATE:
				return onGroupUpdate((JsonMessage<GroupUpdateRequest>) message);
			case GROUP_DELETE:
				return onGroupDelete((JsonMessage<GroupDeleteRequest>) message);
			case GROUP_LEAVE:
				return onGroupLeave((JsonMessage<GroupLeaveRequest>) message);
			case GROUP_LIST:
				return onGroupList((JsonMessage<GroupListRequest>) message);
			case GROUP_LIST_MEMBER_OF:
				return onGroupListMemberOf((JsonMessage<GroupListMemberOfRequest>) message);
			case GROUP_ADD_PLAYERS:
				return onGroupAddPlayers((JsonMessage<GroupAddPlayersRequest>) message, clientInfo);
			case GROUP_REMOVE_PLAYERS:
				return onGroupRemovePlayers((JsonMessage<GroupRemovePlayersRequest>) message);
			case GROUP_LIST_PLAYERS:
				return onGroupListPlayers((JsonMessage<GroupListPlayersRequest>) message);
			case GROUP_BLOCK:
				return onGroupBlock((JsonMessage<GroupBlockRequest>) message);
			case GROUP_UNBLOCK:
				return onGroupUnblock((JsonMessage<GroupUnblockRequest>) message);
			case PLAYER_IS_GROUP_MEMBER:
				return onPlayerIsGroupMember((JsonMessage<PlayerIsGroupMemberRequest>) message);
			case LEADERBOARD_POSITION:
				return onLeaderboardPosition((JsonMessage<LeaderboardPositionRequest>) message);
			case DAILY_LOGIN:
				return onDailyLogin(message);
			case APP_RATED:
				return onAppRated(message);
			case APP_SHARED:
				return onAppShared(message);
			case MOVE_BUDDIES:
				return onMoveBuddies((MoveBuddiesRequest) message.getContent());
			case MOVE_CONTACTS:
				return onMoveContacts((MoveContactsRequest) message.getContent());
			case MOVE_GROUPS:
				return onMoveGroups((MoveGroupsRequest) message.getContent());
			case ELASTIC_RESYNC:
				return onElasticResync();
			case LAST_DUEL_INVITATION_LIST:
				return onLastDuelInvitationList((JsonMessage<EmptyJsonMessageContent>) message);
			default:
				throw new F4MValidationFailedException("Incorrect message type " + type);
			}
		} else {
			throw new F4MValidationFailedException("Incorrect message type " + message.getName());
		}
	}

	private DailyLoginResponse onDailyLogin(JsonMessage<? extends JsonMessageContent> message) {
		DailyLoginResponse result =  new DailyLoginResponse(BigDecimal.ZERO, Currency.BONUS, true);
		if (! message.isAuthenticated()) {
			throw new F4MInsufficientRightsException("User has to be authenticated to get daily login bonus");
		}
		ClientInfo clientInfo = message.getClientInfo();
		if (isFirstLoginForToday(clientInfo.getUserId(), clientInfo.getAppId())) {
			AppConfig appConfig = applicationConfigurationAerospikeDao.getAppConfiguration(clientInfo.getTenantId(),
					clientInfo.getAppId());
			BigDecimal bonuspointsForDailyLogin = null;
			if (isValidAppConfigurationAvailable(appConfig)) {
				bonuspointsForDailyLogin = appConfig.getApplication().getConfiguration().getBonuspointsForDailyLogin();
			}
			if (bonuspointsForDailyLogin != null && bonuspointsForDailyLogin.compareTo(BigDecimal.ZERO) != 0) {
				executeUserActionPayout(message, bonuspointsForDailyLogin, DAILY_LOGIN_PAYOUT_REASON);
				result = null;
			}
		}
		return result;
	}

	private boolean isValidAppConfigurationAvailable(AppConfig appConfig) {
		boolean result = false;
		if (appConfig != null && appConfig.getApplication() != null
				&& appConfig.getApplication().getConfiguration() != null ) {
			result = true;
		}
		return result;
	}

	private boolean isFirstLoginForToday(String userId, String appId) {
		boolean result = false;
		ZonedDateTime startOfToday = DateTimeUtil.getCurrentDateStart();
		String lastLoginTimestamp = commonProfileAerospikeDao.getLastLoginTimestampFromBlob(userId, appId);
		ZonedDateTime lastLoginDateTime = Optional.ofNullable(DateTimeUtil.parseISODateTimeString(lastLoginTimestamp))
				.orElse(startOfToday.minusDays(1));
		if (lastLoginDateTime.isBefore(startOfToday)) {
			result = true;
		}
		return result;
	}

	private AppRatedResponse onAppRated(JsonMessage<? extends JsonMessageContent> message) {
		AppRatedResponse result = new AppRatedResponse(BigDecimal.ZERO, Currency.BONUS);
		if (! message.isAuthenticated()) {
			throw new F4MInsufficientRightsException("User has to be authenticated to get app rated bonus");
		}
		ClientInfo clientInfo = message.getClientInfo();
		AppConfig appConfig = applicationConfigurationAerospikeDao.getAppConfiguration(clientInfo.getTenantId(),
				clientInfo.getAppId());
		BigDecimal bonuspointsForAppRated = null;
		if (isValidAppConfigurationAvailable(appConfig)) {
			bonuspointsForAppRated = appConfig.getApplication().getConfiguration().getBonuspointsForRating();
		}
		if (bonuspointsForAppRated != null && bonuspointsForAppRated.compareTo(BigDecimal.ZERO) != 0) {
			executeUserActionPayout(message, bonuspointsForAppRated, APP_RATED_PAYOUT_REASON);
			result = null;
		}
		return result;
	}

	private void executeUserActionPayout(JsonMessage<? extends JsonMessageContent> message, BigDecimal amount,
			String reason) {
		String userId = getUserId(message);
		String tenantId = message.getClientInfo().getTenantId();
		String appId = message.getClientInfo().getAppId();
		UserActionPayoutRequestInfo userActionPayoutRequestInfo = new UserActionPayoutRequestInfo(tenantId, userId);
		userActionPayoutRequestInfo.setSourceMessage(message);
		userActionPayoutRequestInfo.setSourceSession(getSessionWrapper());
		userActionPayoutRequestInfo.setAppId(appId);
		userActionPayoutRequestInfo.setCurrency(Currency.BONUS);
		userActionPayoutRequestInfo.setAmount(amount);
		userActionPayoutRequestInfo.setReason(reason);
		paymentServiceCommunicator.initiatePayment(userActionPayoutRequestInfo);
	}

	private AppSharedResponse onAppShared(JsonMessage<? extends JsonMessageContent> message) {
		AppSharedResponse result = new AppSharedResponse(BigDecimal.ZERO, Currency.BONUS);
		if (! message.isAuthenticated()) {
			throw new F4MInsufficientRightsException("User has to be authenticated to get app rated bonus");
		}
		ClientInfo clientInfo = message.getClientInfo();
		AppConfig appConfig = applicationConfigurationAerospikeDao.getAppConfiguration(clientInfo.getTenantId(),
				clientInfo.getAppId());
		BigDecimal bonuspointsForAppShared = null;
		if (isValidAppConfigurationAvailable(appConfig)) {
			bonuspointsForAppShared = appConfig.getApplication().getConfiguration().getBonuspointsForSharing();
		}
		if (bonuspointsForAppShared != null && bonuspointsForAppShared.compareTo(BigDecimal.ZERO) != 0) {
			executeUserActionPayout(message, bonuspointsForAppShared, APP_SHARED_PAYOUT_REASON);
			result = null;
		}
		return result;
	}

	private JsonMessageContent onContactImportFacebook(JsonMessage<ContactImportFacebookRequest> message) {
		String userId = getUserId(message);
		String token = message.getContent().getToken();
		String tenantId = message.getClientInfo().getTenantId();
		int importedContactCount = buddyManager.importFacebookContacts(userId, tenantId, token);
		return new ContactImportFacebookResponse(importedContactCount);
	}

	private JsonMessageContent onContactImportPhonebook(JsonMessage<ContactImportPhonebookRequest> message) {
		String userId = getUserId(message);
		ApiPhonebookEntry[] contacts = message.getContent().getContacts();
		String tenantId = message.getClientInfo().getTenantId();
		int importedContactCount = buddyManager.importPhonebookContacts(userId, tenantId, contacts);
		return new ContactImportPhonebookResponse(importedContactCount);
	}

	private JsonMessageContent onContactList(JsonMessage<ContactListRequest> message) {
		final ContactListRequest contactListRequest = message.getContent();
		contactListRequest.validateFilterCriteria(ContactListRequest.MAX_LIST_LIMIT);
		if (contactListRequest.getLimit() == 0) {
			return new ContactListResponse(contactListRequest.getLimit(), contactListRequest.getOffset()); // empty response
		}
		
		String userId = getUserId(message);
		String appId = message.getClientInfo().getAppId();
		String tenantId = message.getClientInfo().getTenantId();

		ListResult<Contact> contacts = buddyManager.listContacts(userId, appId, contactListRequest.isHasApp(),
				tenantId, contactListRequest.getSearchTerm(), contactListRequest.isHasInvitation(),
				contactListRequest.isHasProfile(), contactListRequest.getLimit(), contactListRequest.getOffset());
		List<ApiContactListResult> results = contacts.getItems().stream()
				.map(contact -> new ApiContactListResult(new ApiContact(appId, contact)))
				.collect(Collectors.toList());
		
		if (contactListRequest.isIncludeBuddyInfo() || contactListRequest.isIncludeProfileInfo()) {
			List<String> usersIds = contacts.getItems().stream()
					.map(c -> c.getUserId())
					.collect(Collectors.toList());
			
			if (contactListRequest.isIncludeBuddyInfo()) {
				buddyManager.joinBuddies(results, userId, usersIds, ApiBuddy::new, (r, b) -> r.setBuddy(b));
			}
			if (contactListRequest.isIncludeProfileInfo()) {
				buddyManager.joinProfiles(results, usersIds, ApiProfile::new, (r, p) -> r.setProfile(p));
			}
		}

		return new ContactListResponse(contacts.getLimit(), contacts.getOffset(), contacts.getTotal(), results);
	}
	
	private ContactInviteResponse onContactInvite(JsonMessage<ContactInviteRequest> message) {
		final ContactInviteRequest contactInviteRequest = message.getContent();
		final String[] notInvited = buddyManager.inviteContacts(message, getSessionWrapper(), 
				contactInviteRequest.getContactIds(), contactInviteRequest.getInvitationText(),
				contactInviteRequest.getGroupId());

		InviteEvent inviteEvent = new InviteEvent();
		inviteEvent.setFriendsInvited(1);
		inviteEvent.setBonusInvite(true);
		tracker.addEvent(message.getClientInfo(), inviteEvent);

		return new ContactInviteResponse(notInvited);
	}

	private JsonMessageContent onContactInviteNew(JsonMessage<ContactInviteNewRequest> message) {
		final ContactInviteNewRequest contactInviteNewRequest = message.getContent();
		ClientInfo clientInfo = message.getClientInfo();
		AppConfig appConfig = applicationConfigurationAerospikeDao.getAppConfiguration(clientInfo.getTenantId(),
				clientInfo.getAppId());
		
		//FIXME: need to put it somewhere else?
		final String downloadUrl = appConfig.getApplication().getConfiguration().getDownloadUrl();
		String newInvitationText = contactInviteNewRequest.getInvitationText() + " " + downloadUrl;
		
		buddyManager.inviteNewContact(message, getSessionWrapper(), 
				contactInviteNewRequest.getInvitees(), newInvitationText,
				contactInviteNewRequest.getGroupId());
		return null;
	}

	private JsonMessageContent onContactRemoveInvitation(JsonMessage<ContactRemoveInvitationRequest> message) {
		String userId = getUserId(message);
		String[] userIds = message.getContent().getContactIds();
		String appId = message.getClientInfo().getAppId();
		buddyManager.removeInvitations(userId, appId, userIds);
		return new EmptyJsonMessageContent();
	}

	private JsonMessageContent onContactResendInvitation(JsonMessage<ContactResendInvitationRequest> message) {
		final ContactResendInvitationRequest contactResendInvitationRequest = message.getContent();
		final String[] notInvited = buddyManager.resendInvitations(message, getSessionWrapper(), 
				contactResendInvitationRequest.getContactIds(), contactResendInvitationRequest.getInvitationText(),
				contactResendInvitationRequest.getGroupId());
        return new ContactResendInvitationResponse(notInvited);

	}

	private JsonMessageContent onResync(JsonMessage<ResyncRequest> message) {
		String userId = message.getContent().getUserId();
		ClientInfo clientInfo = message.getClientInfo();
		
		if (clientInfo != null && clientInfo.getUserId() != null) {
			userId = clientInfo.getUserId();
		}
		
		if (userId == null) {
			throw new F4MValidationFailedException("Have to either send userId via clientInfo, or explicitly specify in message");
		}
		return buddyManager.resync(userId);
	}

	private JsonMessageContent onPlayerBlock(JsonMessage<PlayerBlockRequest> message) {
		String userId = getUserId(message);
		String tenantId = message.getClientInfo().getTenantId();
		String[] userIds = message.getContent().getUserIds();
		long blockedPlayers = buddyManager.blockPlayers(userId, tenantId, userIds);

		InviteEvent inviteEvent = new InviteEvent();
		inviteEvent.setFriendsBlocked(blockedPlayers);
		tracker.addEvent(message.getClientInfo(), inviteEvent);

		return new EmptyJsonMessageContent();
	}

	private JsonMessageContent onPlayerUnblock(JsonMessage<PlayerUnblockRequest> message) {
		String userId = getUserId(message);
		String tenantId = message.getClientInfo().getTenantId();
		String[] userIds = message.getContent().getUserIds();
		long unblockedPlayers = buddyManager.unblockPlayers(userId, tenantId, userIds);

		InviteEvent inviteEvent = new InviteEvent();
		inviteEvent.setFriendsUnblocked(unblockedPlayers);
		tracker.addEvent(message.getClientInfo(), inviteEvent);

		return new EmptyJsonMessageContent();
	}

	private JsonMessageContent onPlayerList(JsonMessage<PlayerListRequest> message) {
		final PlayerListRequest playerListRequest = message.getContent();
		playerListRequest.validateFilterCriteria(PlayerListRequest.MAX_LIST_LIMIT);
		if (playerListRequest.getLimit() == 0) {
			return new PlayerListResponse(playerListRequest.getLimit(), playerListRequest.getOffset()); // empty response
		}

		String userId = getUserId(message);
		String appId = message.getClientInfo().getAppId();
		String tenantId = message.getClientInfo().getTenantId();
		
		ListResult<ApiPlayerListResult> results = buddyManager.listPlayers(userId, appId, tenantId, 
				playerListRequest.getSearchTerm(), playerListRequest.getSearchBy("email"), PlayerListOrderType.NONE, playerListRequest.isIncludeBuddies(), playerListRequest.getFavorite(),
				playerListRequest.isIncludeContacts(), playerListRequest.isIncludeUnconnectedPlayers(), playerListRequest.isExcludeBuddiesFromContacts(), 
				playerListRequest.isExcludeBuddiesFromUnconnected(), playerListRequest.isExcludeContactsFromUnconnected(), 
				playerListRequest.isExcludeContactsFromBuddies(), true, playerListRequest.getLimit(), playerListRequest.getOffset());

		if (playerListRequest.isIncludeProfileInfo()) {
			List<String> usersIds = results.getItems().stream()
					.filter(p -> p.getProfile() == null)
					.map(p -> p.getUserId())
					.collect(Collectors.toList());
			buddyManager.joinProfiles(results.getItems(), usersIds, ApiProfile::new, (r, p) -> r.setProfile(p));
		}
		
		if (playerListRequest.isIncludeBuddyInfo()) {
			List<String> usersIds = results.getItems().stream()
					.filter(p -> p.getBuddy() == null)
					.map(p -> p.getUserId())
					.collect(Collectors.toList());
			buddyManager.joinBuddies(results.getItems(), userId, usersIds, ApiBuddy::new, (r, b) -> r.setBuddy(b));
		}
		
		return new PlayerListResponse(results.getLimit(), results.getOffset(), results.getTotal(), results.getItems());
	}
	
	private JsonMessageContent onPlayerLeaderboardList(JsonMessage<PlayerLeaderboardListRequest> message) {
		final PlayerLeaderboardListRequest playerListRequest = message.getContent();
		playerListRequest.validateFilterCriteria(PlayerListRequest.MAX_LIST_LIMIT);
		if (playerListRequest.getLimit() == 0) {
			return new PlayerListResponse(playerListRequest.getLimit(), playerListRequest.getOffset()); // empty response
		}

		String userId = getUserId(message);
		String appId = message.getClientInfo().getAppId();
		String tenantId = message.getClientInfo().getTenantId();
		
		ListResult<ApiPlayerListResult> results = buddyManager.listPlayers(userId, appId, tenantId, null, null, PlayerListOrderType.HANDICAP,
				false, null, false, true, false, false, false, false, false, playerListRequest.getLimit(), playerListRequest.getOffset());
		
		return new PlayerLeaderboardListResponse(results.getLimit(), results.getOffset(), results.getTotal(), 
				results.getItems().stream().map(r -> r.getProfile()).collect(Collectors.toList()));
	}
	
	private JsonMessageContent onBuddyAdd(JsonMessage<BuddyAddRequest> message) {
		buddyManager.addBuddies(message.getUserId(), message.getTenantId(), message.getContent().isFavorite(), true, message.getContent().getUserIds());

		InviteEvent inviteEvent = new InviteEvent();
		inviteEvent.setFriendsInvited(message.getContent().getUserIds().length);
		tracker.addEvent(message.getClientInfo(), inviteEvent);

		return new EmptyJsonMessageContent();
	}

	private JsonMessageContent onBuddyAddForUser(JsonMessage<BuddyAddForUserRequest> message) {
		buddyManager.addBuddies(message.getContent().getUserId(), message.getTenantId(), message.getContent().isFavorite(), false, message.getContent().getUserIds());
		return new EmptyJsonMessageContent();
	}
	
	private JsonMessageContent onBuddyRemove(JsonMessage<BuddyRemoveRequest> message) {
		String userId = getUserId(message);
		String tenantId = message.getClientInfo().getTenantId();
		buddyManager.removeBuddies(userId, tenantId, message.getContent().getUserIds());
		return new EmptyJsonMessageContent();
	}

	private JsonMessageContent onBuddyList(JsonMessage<BuddyListRequest> message) {
		BuddyListRequest buddyListRequest = message.getContent();
		buddyListRequest.validateFilterCriteria(BuddyListRequest.MAX_LIST_LIMIT);
		if (buddyListRequest.getLimit() == 0) {
			return new BuddyListResponse(buddyListRequest.getLimit(), buddyListRequest.getOffset()); // empty response
		}

		String userId = getUserId(message);
		String appId = message.getClientInfo().getAppId();
		String tenantId = message.getClientInfo().getTenantId();

		if (StringUtils.isNotBlank(buddyListRequest.getUserId())) {
			if (buddyManager.isBuddy(userId, appId, tenantId, buddyListRequest.getUserId())) {
				userId = buddyListRequest.getUserId();
			} else {
				throw new F4MInsufficientRightsException("Can not list buddies of a person who is not your buddy");
			}
		}
		ListResult<Buddy> buddies = buddyManager.listBuddies(userId, appId, tenantId, buddyListRequest.getLimit(), buddyListRequest.getOffset(), 
				buddyListRequest.getSearchTerm(), buddyListRequest.getSearchBy("email"), buddyListRequest.getOrderType(), buddyListRequest.getIncludedRelationTypes(), buddyListRequest.getExcludedRelationTypes(),
				buddyListRequest.getFavorite());
		
		List<ApiBuddyListResult> results = buddies.getItems().stream()
				.map(buddy -> new ApiBuddyListResult(new ApiBuddy(buddy)))
				.collect(Collectors.toList());
		
		if (buddyListRequest.isIncludeProfileInfo()) {
			List<String> usersIds = results.stream()
					.map(b -> b.getBuddy().getUserId())
					.collect(Collectors.toList());
			buddyManager.joinProfiles(results, usersIds, ApiProfile::new, (r, p) -> r.setProfile(p));
		}
		
		return new BuddyListResponse(buddies.getLimit(), buddies.getOffset(), buddies.getTotal(), results);
	}

	private JsonMessageContent onBuddyLeaderboardList(JsonMessage<BuddyLeaderboardListRequest> message) {
		BuddyLeaderboardListRequest buddyListRequest = message.getContent();
		buddyListRequest.validateFilterCriteria(BuddyListRequest.MAX_LIST_LIMIT);
		if (buddyListRequest.getLimit() == 0) {
			return new BuddyListResponse(buddyListRequest.getLimit(), buddyListRequest.getOffset()); // empty response
		}

		String userId = getUserId(message);
		String appId = message.getClientInfo().getAppId();
		String tenantId = message.getClientInfo().getTenantId();

		// Find the owner position
		Profile owner = profileDao.getProfile(userId);
		if (owner == null) {
			throw new F4MEntryNotFoundException("Could not find profile with userId " + userId);
		}
		long ownerPosition = buddyManager.getRankAmongBuddies(userId, appId, tenantId, owner.getHandicap()).getLeft() - 1;
		long buddiesFrom = buddyListRequest.getOffset();
		long buddiesTo = buddiesFrom + buddyListRequest.getLimit();
		boolean ownerInsertedBeforeWindow = ownerPosition < buddiesFrom;
		boolean ownerInsertedInCurrentWindow = ownerPosition <= buddiesTo && ! ownerInsertedBeforeWindow;
		
		// List buddies ordered by handicap
		ListResult<Buddy> buddies = ownerInsertedInCurrentWindow && buddyListRequest.getLimit() == 1 
				? new ListResult<>(buddyListRequest.getLimit(), buddyListRequest.getOffset())
				: buddyManager.listBuddies(userId, appId, tenantId, 
						ownerInsertedInCurrentWindow ? buddyListRequest.getLimit() - 1 : buddyListRequest.getLimit(), 
						ownerInsertedBeforeWindow ? buddyListRequest.getOffset() - 1 : buddyListRequest.getOffset(),
						null, null, BuddyListOrderType.HANDICAP, new BuddyRelationType[] { BuddyRelationType.BUDDY },
						new BuddyRelationType[] { BuddyRelationType.BLOCKED_BY, BuddyRelationType.BLOCKED }, null);
		List<ApiBuddyListResult> results = buddies.getItems().stream()
				.map(buddy -> new ApiBuddyListResult(new ApiBuddy(buddy)))
				.collect(Collectors.toList());

		// Join profiles
		List<String> usersIds = results.stream()
				.map(b -> b.getBuddy().getUserId())
				.collect(Collectors.toList());
		buddyManager.joinProfiles(results, usersIds, ApiProfile::new, (r, p) -> r.setProfile(p));

		// Add owner, if present in current window
		if (ownerInsertedInCurrentWindow) {
			ApiBuddyListResult ownerResult = new ApiBuddyListResult(null);
			ownerResult.setProfile(new ApiProfile(owner));
			int ownerIndex = (int) (ownerPosition - buddiesFrom);
			results.add(ownerIndex, ownerResult);
		}

		// Return results
		return new BuddyLeaderboardListResponse(buddyListRequest.getLimit(), buddies.getOffset(), buddies.getTotal() + 1, 
				results.stream().map(r -> r.getProfile()).collect(Collectors.toList()));
	}

	private JsonMessageContent onBuddyListAllIds(JsonMessage<BuddyListAllIdsRequest> message) {
		BuddyListAllIdsRequest buddyListAllIdsRequest = message.getContent();
		String userId = buddyListAllIdsRequest.getUserId();
		String appId = message.getClientInfo().getAppId();
		String tenantId = message.getClientInfo().getTenantId();
		List<String> buddyIds = buddyManager.listAllBuddyIds(userId, appId, tenantId, 
				buddyListAllIdsRequest.getIncludedRelationTypes(), buddyListAllIdsRequest.getExcludedRelationTypes());
		return new BuddyListAllIdsResponse(buddyIds.toArray(new String[buddyIds.size()]));
	}

	private JsonMessageContent onGroupCreate(JsonMessage<GroupCreateRequest> message, ClientInfo clientInfo) {
		String userId = getUserId(message);
		GroupCreateRequest groupCreateRequest = message.getContent();
		String tenantId = message.getClientInfo().getTenantId();
		Group group = buddyManager.createGroup(userId, tenantId, groupCreateRequest.getName(), groupCreateRequest.getType(),
				groupCreateRequest.getImage(), clientInfo, groupCreateRequest.getUserIds());
		return new GroupCreateResponse(new ApiGroup(group));
	}

	private JsonMessageContent onGroupGet(JsonMessage<GroupGetRequest> message) {
		final String userId;
		final String tenantId;
		if (message.isAuthenticated()) {
			userId = message.getUserId();
			tenantId = message.getTenantId();
		} else {
			userId = message.getContent().getUserId();
			tenantId = message.getContent().getTenantId();
		}
		final String groupId = message.getContent().getGroupId();
		final Group group = buddyManager.getGroup(userId, tenantId, groupId);
		return new GroupGetResponse(new ApiGroup(group));
	}

	private JsonMessageContent onGroupUpdate(JsonMessage<GroupUpdateRequest> message) {
		final String userId;
		final String tenantId;
		if (message.isAuthenticated()) {
			userId = message.getUserId();
			tenantId = message.getTenantId();
		} else {
			userId = message.getContent().getUserId();
			tenantId = message.getContent().getTenantId();
		}
		final GroupUpdateRequest groupUpdateRequest = message.getContent();
		final Group group = buddyManager.updateGroup(userId, tenantId, groupUpdateRequest.getGroupId(),
				groupUpdateRequest.getName(), groupUpdateRequest.getType(), groupUpdateRequest.getImage(), groupUpdateRequest.getUserIdsToRemove());
		return new GroupUpdateResponse(new ApiGroup(group));
	}

	private JsonMessageContent onGroupDelete(JsonMessage<GroupDeleteRequest> message) {
		String userId = getUserId(message);
		String tenantId = message.getClientInfo().getTenantId();
		buddyManager.deleteGroup(userId, tenantId, message.getContent().getGroupId());
		return new EmptyJsonMessageContent();
	}

	private JsonMessageContent onGroupLeave(JsonMessage<GroupLeaveRequest> message) {
		String userId = getUserId(message);
		String tenantId = message.getClientInfo().getTenantId();
		String groupId = message.getContent().getGroupId();
		if(!buddyManager.isPlayerInGroup(userId, tenantId, groupId)) {
			throw new F4MEntryNotFoundException("Invalid groupId");
		}
		// we need the owner of the group due to the permission checks in update group, but this is ok, since we validated already :
		Group group = buddyManager.getGroup(userId, tenantId, groupId);
		buddyManager.removePlayersFromGroup(group.getUserId(), tenantId, groupId, new String[]{userId});
		return new EmptyJsonMessageContent();
	}

	private JsonMessageContent onGroupList(JsonMessage<GroupListRequest> message) {
		final GroupListRequest groupListRequest = message.getContent();
		groupListRequest.validateFilterCriteria(GroupListRequest.MAX_LIST_LIMIT);
		final String userId = getUserId(message);
		if (groupListRequest.getLimit() == 0) {
			return new GroupListResponse(groupListRequest.getLimit(), groupListRequest.getOffset()); // empty response
		}
		
		final String tenantId = message.getClientInfo().getTenantId();
		ListResult<Group> groups = buddyManager.listGroups(userId, tenantId, groupListRequest.getLimit(), groupListRequest.getOffset());
		return new GroupListResponse(groups.getLimit(), groups.getOffset(), groups.getTotal(), 
				groups.getItems().stream().map(ApiGroup::new).collect(Collectors.toList()));
	}

	private JsonMessageContent onGroupListMemberOf(JsonMessage<GroupListMemberOfRequest> message) {
		final GroupListMemberOfRequest groupListMemberOfRequest = message.getContent();
		groupListMemberOfRequest.validateFilterCriteria(GroupListMemberOfRequest.MAX_LIST_LIMIT);
		final String userId = getUserId(message);
		if (groupListMemberOfRequest.getLimit() == 0) {
			return new GroupListResponse(groupListMemberOfRequest.getLimit(), groupListMemberOfRequest.getOffset()); // empty response
		}
		
		final String tenantId = message.getClientInfo().getTenantId();
		ListResult<Group> groups = buddyManager.listGroupsMemberOf(userId, tenantId, groupListMemberOfRequest.getBlocked(),
				groupListMemberOfRequest.getLimit(), groupListMemberOfRequest.getOffset());
		return new GroupListMemberOfResponse(groups.getLimit(), groups.getOffset(), groups.getTotal(), 
				groups.getItems().stream().map(ApiGroup::new).collect(Collectors.toList()));
	}

	private JsonMessageContent onGroupAddPlayers(JsonMessage<GroupAddPlayersRequest> message, ClientInfo clientInfo) {
		String userId = getUserId(message);
		GroupAddPlayersRequest groupAddPlayersRequest = message.getContent();
		String tenantId = message.getClientInfo().getTenantId();
		Group group = buddyManager.addPlayersToGroup(userId, tenantId, groupAddPlayersRequest.getGroupId(), groupAddPlayersRequest.getUserIds(), clientInfo);
		return new GroupAddPlayersResponse(new ApiGroup(group));
	}

	private JsonMessageContent onGroupRemovePlayers(JsonMessage<GroupRemovePlayersRequest> message) {
		String userId = getUserId(message);
		GroupRemovePlayersRequest groupRemovePlayersRequest = message.getContent();
		String tenantId = message.getClientInfo().getTenantId();
		Group group = buddyManager.removePlayersFromGroup(userId, tenantId, groupRemovePlayersRequest.getGroupId(), groupRemovePlayersRequest.getUserIds());
		return new GroupRemovePlayersResponse(new ApiGroup(group));
	}

	private JsonMessageContent onGroupListPlayers(JsonMessage<GroupListPlayersRequest> message) {
		final GroupListPlayersRequest groupListPlayersRequest = message.getContent();
		groupListPlayersRequest.validateFilterCriteria(GroupListPlayersRequest.MAX_LIST_LIMIT);
		String userId = getUserId(message);
		if (groupListPlayersRequest.getLimit() == 0) {
			return new GroupListPlayersResponse(groupListPlayersRequest.getLimit(), groupListPlayersRequest.getOffset()); // empty response
		}

		final String tenantId = message.getClientInfo().getTenantId();
		ListResult<String> userIds = buddyManager.listGroupPlayers(userId, tenantId, groupListPlayersRequest.getGroupId(),
				groupListPlayersRequest.getLimit(), groupListPlayersRequest.getOffset(), groupListPlayersRequest.isExcludeGroupOwner());
		if (groupListPlayersRequest.isIncludeProfileInfo()) {
			return new GroupListPlayersResponse(userIds.getLimit(), userIds.getOffset(), userIds.getTotal(), 
					buddyManager.getProfiles(userIds.getItems()));
		} else {
			return new GroupListPlayersResponse(userIds.getLimit(), userIds.getOffset(), userIds.getTotal(), 
					userIds.getItems().stream().map(ApiProfile::new).collect(Collectors.toList()));
		}
	}

	private JsonMessageContent onGroupBlock(JsonMessage<GroupBlockRequest> message) {
		String userId = getUserId(message);
		String tenantId = message.getClientInfo().getTenantId();
		String[] groupIds = message.getContent().getGroupIds();
		buddyManager.blockGroups(userId, tenantId, groupIds);
		return new EmptyJsonMessageContent();
	}

	private JsonMessageContent onGroupUnblock(JsonMessage<GroupUnblockRequest> message) {
		String userId = getUserId(message);
		String tenantId = message.getClientInfo().getTenantId();
		String[] groupIds = message.getContent().getGroupIds();
		buddyManager.unblockGroups(userId, tenantId, groupIds);
		return new EmptyJsonMessageContent();
	}

	private JsonMessageContent onPlayerIsGroupMember(JsonMessage<PlayerIsGroupMemberRequest> message) {
		final String userId;
		final String tenantId;
		if (message.isAuthenticated()) {
			userId = message.getUserId();
			tenantId = message.getTenantId();
		} else {
			userId = message.getContent().getUserId();
			tenantId = message.getContent().getTenantId();
		}
		final String groupId = message.getContent().getGroupId();
		final boolean isPlayerInGroup = buddyManager.isPlayerInGroup(userId, tenantId, groupId);
		return new PlayerIsGroupMemberResponse(isPlayerInGroup);
	}

	private JsonMessageContent onLeaderboardPosition(JsonMessage<LeaderboardPositionRequest> message) {
		String userId = getUserId(message);
		String appId = message.getClientInfo().getAppId();
		String tenantId = message.getClientInfo().getTenantId();
		List<ApiProfile> profiles = buddyManager.getProfiles(Collections.singletonList(userId));
		ApiProfile profile = CollectionUtils.isEmpty(profiles) ? null : profiles.get(0);
		Double handicap = profile == null ? null : profile.getHandicap();
		Pair<Long, Long> buddiesRank = buddyManager.getRankAmongBuddies(userId, appId, tenantId, handicap);
		Pair<Long, Long> playersRank = buddyManager.getRankAmongPlayers(appId, userId, handicap);
		return new LeaderboardPositionResponse(playersRank.getLeft(), playersRank.getRight(), 
				buddiesRank.getLeft(), buddiesRank.getRight());
	}

	private JsonMessageContent onMoveBuddies(MoveBuddiesRequest request) {
		buddyManager.moveBuddies(request.getSourceUserId(), request.getTargetUserId());
		return new EmptyJsonMessageContent();
	}

	private JsonMessageContent onMoveContacts(MoveContactsRequest request) {
		Profile sourceUser = profileDao.getProfile(request.getSourceUserId());
		Profile targetUser = profileDao.getProfile(request.getTargetUserId());
		Collection<String> tenantIds = (sourceUser == null ? targetUser : sourceUser).getTenants();
		buddyManager.moveContacts(request.getSourceUserId(), targetUser, tenantIds);
		return new EmptyJsonMessageContent();
	}

	private JsonMessageContent onMoveGroups(MoveGroupsRequest request) {
		Profile sourceUser = profileDao.getProfile(request.getSourceUserId());
		Profile targetUser = profileDao.getProfile(request.getTargetUserId());
		Collection<String> tenantIds = (sourceUser == null ? targetUser : sourceUser).getTenants();
		buddyManager.moveGroups(request.getSourceUserId(), targetUser, tenantIds);
		return new EmptyJsonMessageContent();
	}

	private JsonMessageContent onElasticResync() {
		Pair<Integer, Integer> resyncedContactAndBuddyCount = buddyManager.resyncIndexes();
		return new ElasticResyncResponse(resyncedContactAndBuddyCount.getLeft(), resyncedContactAndBuddyCount.getRight());
	}

	private JsonMessageContent onLastDuelInvitationList(JsonMessage<EmptyJsonMessageContent> message) {
		String userId = getUserId(message);
		String appId = message.getClientInfo().getAppId();

		List<String> invitedUserIds = profileDao.getLastInvitedDuelOpponents(userId, appId);
		List<ApiProfile> invitedApiProfiles = profileDao.getProfiles(invitedUserIds).stream().map(ApiProfile::new).collect(Collectors.toList());

		List<String> pausedUserIds = profileDao.getPausedDuelOpponents(userId, appId);
		List<ApiProfile> pausedApiProfiles = profileDao.getProfiles(pausedUserIds).stream().map(ApiProfile::new).collect(Collectors.toList());

		return new LastDuelInvitationListResponse(invitedApiProfiles, pausedApiProfiles);
	}

	private String getUserId(JsonMessage<?> message) {
		final ClientInfo clientInfo = message.getClientInfo();
		if (clientInfo == null || message.getClientInfo().getUserId() == null) {
			throw new F4MInsufficientRightsException("User has to be authenticated");
		}
		return message.getClientInfo().getUserId();
	}

}
