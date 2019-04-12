package de.ascendro.f4m.service.friend;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import de.ascendro.f4m.server.profile.PlayerListOrderType;
import de.ascendro.f4m.service.friend.model.Buddy;
import de.ascendro.f4m.service.friend.model.Contact;
import de.ascendro.f4m.service.friend.model.Group;
import de.ascendro.f4m.service.friend.model.api.ApiBuddy;
import de.ascendro.f4m.service.friend.model.api.ApiProfile;
import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.friend.model.api.buddy.BuddyListOrderType;
import de.ascendro.f4m.service.friend.model.api.contact.ApiInvitee;
import de.ascendro.f4m.service.friend.model.api.contact.ApiPhonebookEntry;
import de.ascendro.f4m.service.friend.model.api.contact.ResyncResponse;
import de.ascendro.f4m.service.friend.model.api.player.ApiPlayerListResult;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.session.SessionWrapper;

public interface BuddyManager {

	/** 
	 * Import facebook contacts for given user.
	 * @return Number of imported contacts
	 */
	int importFacebookContacts(String userId, String tenantId, String token);

	/** 
	 * Import phonebook contacts for given user.
	 * @return Number of imported contacts
	 */
	
	int importPhonebookContacts(String userId, String tenantId, ApiPhonebookEntry[] contacts);
	
	void createOrUpdateContact(Contact contact);
	
	Contact getContact(String userId, String contactId);
	
	/** List contacts for the given user with given search criteria. */
	ListResult<Contact> listContacts(String userId, String appId, boolean hasApp, String tenantId, String searchTerm, 
			Boolean hasInvitation, Boolean hasProfile, int limit, long offset);

	/** 
	 * Invite players to join the app for given user from user's contacts. 
	 * @return array of not invited contacts IDs 
	 */
	String[] inviteContacts(JsonMessage<?> sourceMessage, SessionWrapper sourceSession, String[] contactIds, 
			String invitationText, String groupId);

	/** Invite player to join the app for given user. */
	void inviteNewContact(JsonMessage<?> sourceMessage, SessionWrapper sourceSession, ApiInvitee invitee, 
			String invitationText, String groupId);

	/** Remove invitations for given user. */
	void removeInvitations(String userId, String appId, String[] contactIds);

	/** 
	 * Resend invitations for given user. 
	 * @return array of not invited contacts IDs 
	 */
	String[] resendInvitations(JsonMessage<?> sourceMessage, SessionWrapper sourceSession, String[] contactIds, 
			String invitationText, String groupId);

	/** 
	 * Resynchronize contacts to find matching profiles / remove lost matches.
	 * Resynchronize buddies to update searchName and remove deleted buddies.
	 */
	ResyncResponse resync(String userId);

	/** Add blocked players for given user. */
	long blockPlayers(String userId, String tenantId, String[] userIds);

	/** Remove blocked players for given user. */
	long unblockPlayers(String userId, String tenantId, String[] userIds);

	/** List players / buddies / contacts of given user with given search criteria. */
	ListResult<ApiPlayerListResult> listPlayers(String userId, String appId, String tenantId, String searchTerm, String email, PlayerListOrderType orderType,
			boolean includeBuddies, Boolean favorite, boolean includeContacts, 
			boolean includeUnconnectedPlayers, boolean excludeBuddiesFromContacts, boolean excludeBuddiesFromUnconnected,
			boolean excludeContactsFromUnconnected, boolean excludeContactsFromBuddies, boolean excludeSelf, int limit, long offset);

	/** Add buddies for given user. */
	void addBuddies(String userId, String tenantId, Boolean favorite, boolean unblock, String... userIds);

	/** Remove buddies for given user. */
	void removeBuddies(String userId, String tenantId, String[] userIds);

	/** List buddies of given user with given search criteria. */
	ListResult<Buddy> listBuddies(String userId, String appId, String tenantId, Integer limit, Long offset, String searchTerm, String email, BuddyListOrderType orderType,
			BuddyRelationType[] includedRelationTypes, BuddyRelationType[] excludedRelationTypes, Boolean favorite);

	/** List all buddy IDs of given user, appId and tenantId. */
	List<String> listAllBuddyIds(String userId, String appId, String tenantId, BuddyRelationType[] includedRelationTypes, BuddyRelationType[] excludedRelationTypes);

	/** Determine if given user ID is a buddy. */
	boolean isBuddy(String userId, String appId, String tenantId, String buddyUserId);

	/** Create group for given user. */
    Group createGroup(String userId, String tenantId, String name, String type, String image, ClientInfo clientInfo, String... initialUserIds);

	/** Get group for given user. */
    Group getGroup(String userId, String tenantId, String groupId);

	/** Update group for given user. */
    Group updateGroup(String userId, String tenantId, String groupId, String name, String type, String image, String... userIdsToRemove);

    /** Delete group for given user. */
    void deleteGroup(String userId, String tenantId, String groupId);

    /** List groups of given user. */
	ListResult<Group> listGroups(String userId, String tenantId, int limit, long offset);

    /** List groups given user is member of. */
	ListResult<Group> listGroupsMemberOf(String userId, String tenantId, Boolean blocked, int limit, long offset);

	/** Add players to the group for given user. */
	Group addPlayersToGroup(String userId, String tenantId, String groupId, String[] userIds, ClientInfo clientIfo);

	/** Remove players from the group for given user. */
	Group removePlayersFromGroup(String userId, String tenantId, String groupId, String[] userIds);

	/** List group players for given user. */
	ListResult<String> listGroupPlayers(String userId, String tenantId, String groupId, int limit, long offset, boolean excludeGroupOwner);

	/** Determine if player is in group. */
	boolean isPlayerInGroup(String userId, String tenantId, String groupId);

	/** Block groups for a player. */
	void blockGroups(String userId, String tenantId, String[] groupIds);

	/** Unblock groups for a player. */
	void unblockGroups(String userId, String tenantId, String[] groupIds);

	<R> void joinBuddies(List<R> results, String userId, List<String> usersIds, Function<Buddy, ApiBuddy> initApiElement,
			BiConsumer<R, ApiBuddy> setter);

	<R> void joinProfiles(List<R> results, List<String> usersIds, Function<Profile, ApiProfile> initApiElement,
			BiConsumer<R, ApiProfile> setter);

	List<ApiProfile> getProfiles(List<String> usersIds);

	Pair<Integer, Integer> resyncIndexes();

	/** Get number of players with higher handicap (left) among buddies (total buddy count on right). */
	Pair<Long, Long> getRankAmongBuddies(String userId, String appId, String tenantId, Double handicap);

	/** Get number of players with higher handicap (left) among all players (total player count on right). */
	Pair<Long, Long> getRankAmongPlayers(String appId, String userId, Double handicap);

	/** Move buddies from source user to target user. */
	void moveBuddies(String sourceUserId, String targetUserId);

	/** Move contacts from source user to target user. */
	void moveContacts(String sourceUserId, Profile targetUser, Collection<String> tenantIds);

	/** Move groups from source user to target user. */
	void moveGroups(String sourceUserId, Profile targetUser, Collection<String> tenantIds);

}
