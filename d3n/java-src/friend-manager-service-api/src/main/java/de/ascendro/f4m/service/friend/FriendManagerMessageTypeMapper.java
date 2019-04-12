package de.ascendro.f4m.service.friend;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.friend.model.api.AppRatedResponse;
import de.ascendro.f4m.service.friend.model.api.AppSharedResponse;
import de.ascendro.f4m.service.friend.model.api.DailyLoginResponse;
import de.ascendro.f4m.service.friend.model.api.ElasticResyncRequest;
import de.ascendro.f4m.service.friend.model.api.ElasticResyncResponse;
import de.ascendro.f4m.service.friend.model.api.LastDuelInvitationListResponse;
import de.ascendro.f4m.service.friend.model.api.LeaderboardPositionRequest;
import de.ascendro.f4m.service.friend.model.api.LeaderboardPositionResponse;
import de.ascendro.f4m.service.friend.model.api.MoveBuddiesRequest;
import de.ascendro.f4m.service.friend.model.api.MoveContactsRequest;
import de.ascendro.f4m.service.friend.model.api.MoveGroupsRequest;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyAddForUserRequest;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyAddRequest;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyLeaderboardListRequest;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyLeaderboardListResponse;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListAllIdsRequest;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListAllIdsResponse;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListRequest;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListResponse;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyRemoveRequest;
import de.ascendro.f4m.service.friend.model.api.contact.ContactImportFacebookRequest;
import de.ascendro.f4m.service.friend.model.api.contact.ContactImportFacebookResponse;
import de.ascendro.f4m.service.friend.model.api.contact.ContactImportPhonebookRequest;
import de.ascendro.f4m.service.friend.model.api.contact.ContactImportPhonebookResponse;
import de.ascendro.f4m.service.friend.model.api.contact.ContactInviteNewRequest;
import de.ascendro.f4m.service.friend.model.api.contact.ContactInviteNewResponse;
import de.ascendro.f4m.service.friend.model.api.contact.ContactInviteRequest;
import de.ascendro.f4m.service.friend.model.api.contact.ContactInviteResponse;
import de.ascendro.f4m.service.friend.model.api.contact.ContactListRequest;
import de.ascendro.f4m.service.friend.model.api.contact.ContactListResponse;
import de.ascendro.f4m.service.friend.model.api.contact.ContactRemoveInvitationRequest;
import de.ascendro.f4m.service.friend.model.api.contact.ContactResendInvitationRequest;
import de.ascendro.f4m.service.friend.model.api.contact.ContactResendInvitationResponse;
import de.ascendro.f4m.service.friend.model.api.contact.ResyncRequest;
import de.ascendro.f4m.service.friend.model.api.contact.ResyncResponse;
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
import de.ascendro.f4m.service.friend.model.api.player.PlayerBlockRequest;
import de.ascendro.f4m.service.friend.model.api.player.PlayerLeaderboardListRequest;
import de.ascendro.f4m.service.friend.model.api.player.PlayerLeaderboardListResponse;
import de.ascendro.f4m.service.friend.model.api.player.PlayerListRequest;
import de.ascendro.f4m.service.friend.model.api.player.PlayerListResponse;
import de.ascendro.f4m.service.friend.model.api.player.PlayerUnblockRequest;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;

public class FriendManagerMessageTypeMapper extends JsonMessageTypeMapImpl {
	private static final long serialVersionUID = -4575812985735172610L;

	public FriendManagerMessageTypeMapper() {
		init();
	}

	protected void init() {
		// Contact calls
		register(FriendManagerMessageTypes.CONTACT_IMPORT_FACEBOOK, new TypeToken<ContactImportFacebookRequest>() {});
		register(FriendManagerMessageTypes.CONTACT_IMPORT_FACEBOOK_RESPONSE, new TypeToken<ContactImportFacebookResponse>() {});

		register(FriendManagerMessageTypes.CONTACT_IMPORT_PHONEBOOK, new TypeToken<ContactImportPhonebookRequest>() {});
		register(FriendManagerMessageTypes.CONTACT_IMPORT_PHONEBOOK_RESPONSE, new TypeToken<ContactImportPhonebookResponse>() {});

		register(FriendManagerMessageTypes.CONTACT_LIST, new TypeToken<ContactListRequest>() {});
		register(FriendManagerMessageTypes.CONTACT_LIST_RESPONSE, new TypeToken<ContactListResponse>() {});

		register(FriendManagerMessageTypes.CONTACT_INVITE, new TypeToken<ContactInviteRequest>() {});
		register(FriendManagerMessageTypes.CONTACT_INVITE_RESPONSE, new TypeToken<ContactInviteResponse>() {});
		
		register(FriendManagerMessageTypes.CONTACT_INVITE_NEW, new TypeToken<ContactInviteNewRequest>() {});
		register(FriendManagerMessageTypes.CONTACT_INVITE_NEW_RESPONSE, new TypeToken<ContactInviteNewResponse>() {});
		
		register(FriendManagerMessageTypes.CONTACT_REMOVE_INVITATION, new TypeToken<ContactRemoveInvitationRequest>() {});
		register(FriendManagerMessageTypes.CONTACT_REMOVE_INVITATION_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});
		
		register(FriendManagerMessageTypes.CONTACT_RESEND_INVITATION, new TypeToken<ContactResendInvitationRequest>() {});
		register(FriendManagerMessageTypes.CONTACT_RESEND_INVITATION_RESPONSE, new TypeToken<ContactResendInvitationResponse>() {});
		
		// Player calls
		register(FriendManagerMessageTypes.PLAYER_BLOCK, new TypeToken<PlayerBlockRequest>() {});
		register(FriendManagerMessageTypes.PLAYER_BLOCK_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});

		register(FriendManagerMessageTypes.PLAYER_UNBLOCK, new TypeToken<PlayerUnblockRequest>() {});
		register(FriendManagerMessageTypes.PLAYER_UNBLOCK_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});

		register(FriendManagerMessageTypes.PLAYER_IS_GROUP_MEMBER, new TypeToken<PlayerIsGroupMemberRequest>() {});
		register(FriendManagerMessageTypes.PLAYER_IS_GROUP_MEMBER_RESPONSE, new TypeToken<PlayerIsGroupMemberResponse>() {});

		register(FriendManagerMessageTypes.PLAYER_LIST, new TypeToken<PlayerListRequest>() {});
		register(FriendManagerMessageTypes.PLAYER_LIST_RESPONSE, new TypeToken<PlayerListResponse>() {});

		register(FriendManagerMessageTypes.PLAYER_LEADERBOARD_LIST, new TypeToken<PlayerLeaderboardListRequest>() {});
		register(FriendManagerMessageTypes.PLAYER_LEADERBOARD_LIST_RESPONSE, new TypeToken<PlayerLeaderboardListResponse>() {});

		// Buddy calls
		register(FriendManagerMessageTypes.BUDDY_ADD, new TypeToken<BuddyAddRequest>() {});
		register(FriendManagerMessageTypes.BUDDY_ADD_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});

		register(FriendManagerMessageTypes.BUDDY_ADD_FOR_USER, new TypeToken<BuddyAddForUserRequest>() {});
		register(FriendManagerMessageTypes.BUDDY_ADD_FOR_USER_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});

		register(FriendManagerMessageTypes.BUDDY_REMOVE, new TypeToken<BuddyRemoveRequest>() {});
		register(FriendManagerMessageTypes.BUDDY_REMOVE_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});

		register(FriendManagerMessageTypes.BUDDY_LIST, new TypeToken<BuddyListRequest>() {});
		register(FriendManagerMessageTypes.BUDDY_LIST_RESPONSE, new TypeToken<BuddyListResponse>() {});

		register(FriendManagerMessageTypes.BUDDY_LEADERBOARD_LIST, new TypeToken<BuddyLeaderboardListRequest>() {});
		register(FriendManagerMessageTypes.BUDDY_LEADERBOARD_LIST_RESPONSE, new TypeToken<BuddyLeaderboardListResponse>() {});

		register(FriendManagerMessageTypes.BUDDY_LIST_ALL_IDS, new TypeToken<BuddyListAllIdsRequest>() {});
		register(FriendManagerMessageTypes.BUDDY_LIST_ALL_IDS_RESPONSE, new TypeToken<BuddyListAllIdsResponse>() {});

		// Group calls
		register(FriendManagerMessageTypes.GROUP_CREATE, new TypeToken<GroupCreateRequest>() {});
		register(FriendManagerMessageTypes.GROUP_CREATE_RESPONSE, new TypeToken<GroupCreateResponse>() {});

		register(FriendManagerMessageTypes.GROUP_GET, new TypeToken<GroupGetRequest>() {});
		register(FriendManagerMessageTypes.GROUP_GET_RESPONSE, new TypeToken<GroupGetResponse>() {});

		register(FriendManagerMessageTypes.GROUP_UPDATE, new TypeToken<GroupUpdateRequest>() {});
		register(FriendManagerMessageTypes.GROUP_UPDATE_RESPONSE, new TypeToken<GroupUpdateResponse>() {});

		register(FriendManagerMessageTypes.GROUP_DELETE, new TypeToken<GroupDeleteRequest>() {});
		register(FriendManagerMessageTypes.GROUP_DELETE_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});
		
		register(FriendManagerMessageTypes.GROUP_LIST, new TypeToken<GroupListRequest>() {});
		register(FriendManagerMessageTypes.GROUP_LIST_RESPONSE, new TypeToken<GroupListResponse>() {});
		
		register(FriendManagerMessageTypes.GROUP_LIST_MEMBER_OF, new TypeToken<GroupListMemberOfRequest>() {});
		register(FriendManagerMessageTypes.GROUP_LIST_MEMBER_OF_RESPONSE, new TypeToken<GroupListMemberOfResponse>() {});

		register(FriendManagerMessageTypes.GROUP_BLOCK, new TypeToken<GroupBlockRequest>() {});
		register(FriendManagerMessageTypes.GROUP_BLOCK_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});
		
		register(FriendManagerMessageTypes.GROUP_UNBLOCK, new TypeToken<GroupUnblockRequest>() {});
		register(FriendManagerMessageTypes.GROUP_UNBLOCK_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});
		
		register(FriendManagerMessageTypes.GROUP_ADD_PLAYERS, new TypeToken<GroupAddPlayersRequest>() {});
		register(FriendManagerMessageTypes.GROUP_ADD_PLAYERS_RESPONSE, new TypeToken<GroupAddPlayersResponse>() {});
		
		register(FriendManagerMessageTypes.GROUP_REMOVE_PLAYERS, new TypeToken<GroupRemovePlayersRequest>() {});
		register(FriendManagerMessageTypes.GROUP_REMOVE_PLAYERS_RESPONSE, new TypeToken<GroupRemovePlayersResponse>() {});
		
		register(FriendManagerMessageTypes.MOVE_BUDDIES, new TypeToken<MoveBuddiesRequest>() {});
		register(FriendManagerMessageTypes.MOVE_BUDDIES_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});
		
		register(FriendManagerMessageTypes.MOVE_CONTACTS, new TypeToken<MoveContactsRequest>() {});
		register(FriendManagerMessageTypes.MOVE_CONTACTS_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});
		
		register(FriendManagerMessageTypes.MOVE_GROUPS, new TypeToken<MoveGroupsRequest>() {});
		register(FriendManagerMessageTypes.MOVE_GROUPS_RESPONSE, new TypeToken<EmptyJsonMessageContent>() {});
		
		register(FriendManagerMessageTypes.GROUP_LIST_PLAYERS, new TypeToken<GroupListPlayersRequest>() {});
		register(FriendManagerMessageTypes.GROUP_LIST_PLAYERS_RESPONSE, new TypeToken<GroupListPlayersResponse>() {});

		register(FriendManagerMessageTypes.GROUP_LEAVE, new TypeToken<GroupLeaveRequest>() {});

		// Leaderboard calls
		register(FriendManagerMessageTypes.LEADERBOARD_POSITION, new TypeToken<LeaderboardPositionRequest>() {});
		register(FriendManagerMessageTypes.LEADERBOARD_POSITION_RESPONSE, new TypeToken<LeaderboardPositionResponse>() {});

		// Payout calls
		register(FriendManagerMessageTypes.DAILY_LOGIN, new TypeToken<EmptyJsonMessageContent>() {});
		register(FriendManagerMessageTypes.DAILY_LOGIN_RESPONSE, new TypeToken<DailyLoginResponse>() {});

		register(FriendManagerMessageTypes.APP_RATED, new TypeToken<EmptyJsonMessageContent>() {});
		register(FriendManagerMessageTypes.APP_RATED_RESPONSE, new TypeToken<AppRatedResponse>() {});

		register(FriendManagerMessageTypes.APP_SHARED, new TypeToken<EmptyJsonMessageContent>() {});
		register(FriendManagerMessageTypes.APP_SHARED_RESPONSE, new TypeToken<AppSharedResponse>() {});

		// Resync calls
		register(FriendManagerMessageTypes.RESYNC, new TypeToken<ResyncRequest>() {});
		register(FriendManagerMessageTypes.RESYNC_RESPONSE, new TypeToken<ResyncResponse>() {});
		
		register(FriendManagerMessageTypes.ELASTIC_RESYNC, new TypeToken<ElasticResyncRequest>() {});
		register(FriendManagerMessageTypes.ELASTIC_RESYNC_RESPONSE, new TypeToken<ElasticResyncResponse>() {});

		register(FriendManagerMessageTypes.LAST_DUEL_INVITATION_LIST, new TypeToken<EmptyJsonMessageContent>() {});
		register(FriendManagerMessageTypes.LAST_DUEL_INVITATION_LIST_RESPONSE, new TypeToken<LastDuelInvitationListResponse>() {});
	}
	
}
