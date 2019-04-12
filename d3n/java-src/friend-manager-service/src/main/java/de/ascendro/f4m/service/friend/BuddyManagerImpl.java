package de.ascendro.f4m.service.friend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileElasticDao;
import de.ascendro.f4m.server.profile.PlayerListOrderType;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.friend.client.AuthServiceCommunicator;
import de.ascendro.f4m.service.friend.client.UserMessageServiceCommunicator;
import de.ascendro.f4m.service.friend.dao.BuddyAerospikeDao;
import de.ascendro.f4m.service.friend.dao.BuddyElasticDao;
import de.ascendro.f4m.service.friend.dao.ContactAerospikeDao;
import de.ascendro.f4m.service.friend.dao.ContactElasticDao;
import de.ascendro.f4m.service.friend.dao.GroupAerospikeDAO;
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
import de.ascendro.f4m.service.friend.notification.PlayerAddedToGroupNotification;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileIdentifierType;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.usermessage.translation.Messages;

public class BuddyManagerImpl implements BuddyManager {

	private final GroupAerospikeDAO groupDao;
	private final CommonProfileAerospikeDao profileDao;
	private final CommonProfileElasticDao profileElasticDao;
	private final ContactAerospikeDao contactDao;
	private final ContactElasticDao contactElasticDao;
	private final BuddyAerospikeDao buddyDao;
	private final BuddyElasticDao buddyElasticDao;
	private final CommonBuddyElasticDao commonBuddyElasticDao;
	private final AuthServiceCommunicator authServiceCommunicator;
	private final UserMessageServiceCommunicator userMessageCommunicator;

	private static final int BATCH_SIZE = 100;
	
	@Inject
	public BuddyManagerImpl(GroupAerospikeDAO groupDao, CommonProfileAerospikeDao profileDao,
			CommonProfileElasticDao profileElasticDao, ContactAerospikeDao contactDao,
			ContactElasticDao contactElasticDao, BuddyAerospikeDao buddyDao, BuddyElasticDao buddyElasticDao,
			CommonBuddyElasticDao commonBuddyElasticDao, AuthServiceCommunicator authServiceCommunicator,
			UserMessageServiceCommunicator userMessageCommunicator) {
		this.groupDao = groupDao;
		this.profileDao = profileDao;
		this.profileElasticDao = profileElasticDao;
		this.contactDao = contactDao;
		this.contactElasticDao = contactElasticDao;
		this.buddyDao = buddyDao;
		this.buddyElasticDao = buddyElasticDao;
		this.authServiceCommunicator = authServiceCommunicator;
		this.commonBuddyElasticDao = commonBuddyElasticDao;
		this.userMessageCommunicator = userMessageCommunicator;
	}

	@Override
	public int importFacebookContacts(String userId, String tenantId, String token) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);
		// TODO Implement
		return 28;
	}

	@Override
	public int importPhonebookContacts(String userId, String tenantId, ApiPhonebookEntry[] contacts) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);
		List<Contact> existingContacts = contactElasticDao.listAllContacts(userId);
		AtomicInteger importedItemCount = new AtomicInteger();
		if (contacts != null) {
			Arrays.stream(contacts)
					.filter(c -> StringUtils.isNotEmpty(c.getFirstName()) || StringUtils.isNotEmpty(c.getLastName()))
					.forEach(c -> {
						storeContact(userId, tenantId, c, existingContacts);
						importedItemCount.incrementAndGet();
					});
		}
		return importedItemCount.get();
	}
	
	@Override
	public void createOrUpdateContact(Contact contact) {
		contactDao.createOrUpdateContact(contact);
	}
	
	@Override
	public Contact getContact(String userId, String contactId) {
		return contactDao.getContact(userId, contactId);
	}

	private void storeContact(String ownerId, String tenantId, ApiPhonebookEntry item, List<Contact> existingContacts) {
		Optional<Contact> existing = existingContacts.stream()
				.filter(c -> ArrayUtils.contains(c.getTenantIds(), tenantId) && matches(c, item)) // Contacts are stored per-tenant, so each tenant will get a contact copy
				.findAny();
		Contact contact = existing.isPresent() ? existing.get() : new Contact(ownerId, tenantId, item);
		if (existing.isPresent()) {
			contact.merge(tenantId, item);
		} else {
			contact.setContactId(PrimaryKeyUtil.createGeneratedId());
		}
		
		matchProfile(contact);
		
		contactDao.createOrUpdateContact(contact);
	}

	private boolean matches(Contact contact, ApiPhonebookEntry phonebookEntry) {
		if (hasCommonElement(contact.getPhoneNumbers(), phonebookEntry.getPhoneNumbers())
				|| hasCommonElement(contact.getEmails(), phonebookEntry.getEmails())) {
			// If there is a match in phones / emails => verify that at least one of firstName / lastName matches too
			return areEqual(contact.getFirstName(), phonebookEntry.getFirstName())
					|| areEqual(contact.getLastName(), phonebookEntry.getLastName());
		} else {
			// Otherwise require both firstName and lastName to match
			return areEqual(contact.getFirstName(), phonebookEntry.getFirstName())
					&& areEqual(contact.getLastName(), phonebookEntry.getLastName());
		}
	}

	private boolean areEqual(String value1, String value2) {
		return StringUtils.equals(normalize(value1), normalize(value2));
	}
	
	private boolean hasCommonElement(String[] array1, String[] array2) {
		if (ArrayUtils.isEmpty(array1) || ArrayUtils.isEmpty(array2)) {
			return false;
		}
		Set<String> array1Set = new HashSet<>(Arrays.stream(array1).map(this::normalize).collect(Collectors.toList()));
		return Arrays.stream(array2).anyMatch(s -> array1Set.contains(normalize(s)));
	}

	private String normalize(String value) {
		return StringUtils.lowerCase(StringUtils.trim(value));
	}

	@Override
	public ListResult<Contact> listContacts(String userId, String appId, boolean hasApp, String tenantId, String searchTerm, 
			Boolean hasInvitation, Boolean hasProfile, int limit, long offset) {
		Validate.notNull(userId);
		Validate.notNull(appId);
		Validate.notNull(tenantId); // Expected to always list by tenant 
		return contactElasticDao.getContactList(userId, appId, hasApp, tenantId, searchTerm, hasInvitation, hasProfile, null, null, limit, offset);
	}

	@Override
	public String[] inviteContacts(JsonMessage<?> sourceMessage, SessionWrapper sourceSession, String[] contactIds, String invitationText, String groupId) {
		Predicate<Contact> hasEmail = c -> ArrayUtils.isNotEmpty(c.getEmails());
		return inviteContacts(sourceMessage, sourceSession, contactIds, invitationText, groupId, hasEmail);
	}

	@Override
	public void inviteNewContact(JsonMessage<?> sourceMessage, SessionWrapper sourceSession, ApiInvitee invitee, String invitationText, String groupId) {
		final ClientInfo clientInfo = sourceMessage.getClientInfo();
		final String tenantId = clientInfo.getTenantId();
		final String userId = clientInfo.getUserId();
		Contact contact = new Contact(userId, invitee);
		contact.setContactId(PrimaryKeyUtil.createGeneratedId());
		contact.setTenantIds(tenantId);
		contactDao.createOrUpdateContact(contact);

		authServiceCommunicator.requestInviteUserByEmail(contact, invitationText, groupId, sourceMessage, sourceSession);
	}

	@Override
	public void removeInvitations(String userId, String appId, String[] contactIds) {
		List<Contact> contacts = contactElasticDao.listContacts(userId, contactIds);
		contacts.forEach(c -> {
			c.setSentInvitationTextAndGroup(appId, null, null);
			contactDao.createOrUpdateContact(c);
		});
	}

	@Override
	public String[] resendInvitations(JsonMessage<?> sourceMessage, SessionWrapper sourceSession, String[] contactIds, String invitationText, String groupId) {
		final String appId = sourceMessage.getClientInfo().getAppId();
		Predicate<Contact> hasEmailAndInvitation = c -> ArrayUtils.isNotEmpty(c.getEmails()) && c.getSentInvitationTextAndGroup(appId).getLeft() != null;
		return inviteContacts(sourceMessage, sourceSession, contactIds, invitationText, groupId, hasEmailAndInvitation);
	}

	private String[] inviteContacts(JsonMessage<?> sourceMessage, SessionWrapper sourceSession, String[] contactIds,
			String invitationText, String groupId, Predicate<Contact> partitionPredicate) {
		final ClientInfo clientInfo = sourceMessage.getClientInfo();
		Map<Boolean, List<Contact>> partitionedContacts = getPartitionedContacts(clientInfo.getUserId(), contactIds, partitionPredicate);

		partitionedContacts.get(true).forEach(c -> {
			Pair<String, String> invitationTextAndGroup = c.getSentInvitationTextAndGroup(clientInfo.getAppId());
			String text = invitationText != null ? invitationText : invitationTextAndGroup.getLeft();
			String group = groupId != null ? groupId : invitationTextAndGroup.getRight();
			authServiceCommunicator.requestInviteUserByEmail(c, text, group, sourceMessage, sourceSession);
		});
		
		return partitionedContacts.get(false).stream()
				.map(Contact::getContactId)
				.toArray(size -> new String[size]);
	}

	private Map<Boolean, List<Contact>> getPartitionedContacts(String userId, String[] contactIds, Predicate<Contact> partitionPredicate) {
		List<Contact> contacts = contactElasticDao.listContacts(userId, contactIds);
		return contacts.stream().collect(Collectors.partitioningBy(partitionPredicate));
	}

	@Override
	public ResyncResponse resync(String ownerId) {
		Validate.notNull(ownerId);
		AtomicInteger newContactMatchesCount = new AtomicInteger();
		AtomicInteger lostContactMatchesCount = new AtomicInteger();
		AtomicInteger lostBuddyCount = new AtomicInteger();
		List<Contact> existingContacts = contactElasticDao.listAllContacts(ownerId);
		existingContacts.forEach(c -> resyncContact(c, newContactMatchesCount, lostContactMatchesCount));
		List<String> existingBuddyIds = commonBuddyElasticDao.getAllBuddyIds(ownerId, null, null, null, null, null);
		existingBuddyIds.forEach(b -> resyncBuddy(ownerId, b, lostBuddyCount));
		return new ResyncResponse(existingContacts.size(), newContactMatchesCount.get(), 
				lostContactMatchesCount.get(), existingBuddyIds.size(), lostBuddyCount.get());
	}

	/** Resync buddy searchName and delete lost buddies */
	private void resyncBuddy(String ownerId, String buddyId, AtomicInteger lostBuddyCount) {
		if (profileDao.exists(buddyId)) {
			List<Buddy> buddies = buddyDao.getBuddies(ownerId, Collections.singletonList(buddyId));
			if (! buddies.isEmpty()) {
				Buddy buddy = buddies.get(0);
				buddyDao.createOrUpdateBuddy(ownerId, buddy.getTenantIds()[0], buddy); // This will update searchName
			}
		} else {
			buddyDao.deleteBuddy(ownerId, buddyId); // Delete lost buddy
			lostBuddyCount.incrementAndGet();
		}
	}

	/** Resync contact match to profile */
	private void resyncContact(Contact contact, AtomicInteger newContactMatchesCount, AtomicInteger lostContactMatchesCount) {
		if (contact.getUserId() != null) {
			// Check if still matches
			if (!profileDao.exists(contact.getUserId())) {
				contact.setUserId(null);
				contactDao.createOrUpdateContact(contact);
				lostContactMatchesCount.incrementAndGet();
			}
		} else if (matchProfile(contact)) {
			// Check if new match can be found
			contactDao.createOrUpdateContact(contact);
			newContactMatchesCount.incrementAndGet();
		}
	}
	
	/**
	 * Match contact to existing profiles 
	 * @return <code>true</code> if contact was changed (match found)
	 */
	private boolean matchProfile(Contact contact) {
		if (contact.getUserId() != null) {
			return false; // For already matched contacts do nothing
		}
		
		// Search by phone numbers
		if (findProfile(contact, contact.getPhoneNumbers(), ProfileIdentifierType.PHONE)) {
			return true;
		}
		
		// Search by emails
		if (findProfile(contact, contact.getEmails(), ProfileIdentifierType.EMAIL)) {
			return true;
		}
		
		return false;
	}

	private boolean findProfile(Contact contact, String[] identifiers, ProfileIdentifierType idType) {
		if (identifiers != null) {
			for (String id : identifiers) {
				Profile profile = profileDao.findByIdentifierWithCleanup(idType, id);
				if (profile != null) {
					contact.setUserId(profile.getUserId());
					Set<String> profileTenantIds = profile.getTenants();
					if (profileTenantIds != null) {
						Arrays.stream(contact.getTenantIds()).filter(profileTenantIds::contains).forEach(tenantId -> 
								addBuddies(contact.getOwnerId(), tenantId, null, false, contact.getUserId()));
					}
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public long blockPlayers(String userId, String tenantId, String[] userIds) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);
		return Arrays.stream(userIds)
				.filter(id -> buddyDao.createBuddyRelation(userId, id, tenantId, BuddyRelationType.BLOCKED, null))
				.count();
	}

	@Override
	public long unblockPlayers(String userId, String tenantId, String[] userIds) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);
		return Arrays.stream(userIds)
				.filter(id -> buddyDao.deleteBuddyRelation(userId, id, tenantId, BuddyRelationType.BLOCKED))
				.count();
	}

	@Override
	public ListResult<ApiPlayerListResult> listPlayers(String userId, String appId, String tenantId, String searchTerm, String email, PlayerListOrderType orderType,
			boolean includeBuddies, Boolean favorite, boolean includeContacts, boolean includeUnconnectedPlayers, boolean excludeBuddiesFromContacts, 
			boolean excludeBuddiesFromUnconnected, boolean excludeContactsFromUnconnected, boolean excludeContactsFromBuddies, boolean excludeSelf, 
			int limit, long offset) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);
		Validate.notNull(appId);

		long listOffset = offset;
		int listLimit = limit;
		long total = 0;

		List<ApiPlayerListResult> result = new ArrayList<>();

		if (excludeBuddiesFromContacts) {
			// Make sure we don't exclude buddies from contacts and vice versa
			excludeContactsFromBuddies = false;
		}
		
		Set<String> contactUserIds = includeBuddies && excludeContactsFromBuddies || includeUnconnectedPlayers && excludeContactsFromUnconnected
				? contactElasticDao.listAllContacts(userId).stream().filter(c -> c.getUserId() != null).map(c -> c.getUserId()).collect(Collectors.toSet()) 
				: Collections.emptySet();
		Set<String> buddyUserIds = includeContacts && excludeBuddiesFromContacts || includeUnconnectedPlayers && excludeBuddiesFromUnconnected
				? new HashSet<>(commonBuddyElasticDao.getAllBuddyIds(userId, appId, tenantId, new BuddyRelationType[] { BuddyRelationType.BUDDY }, 
						new BuddyRelationType[] { BuddyRelationType.BLOCKED }, null))
				: Collections.emptySet();
				
				
		Set<String> excludedBuddies = includeBuddies && excludeContactsFromBuddies ? contactUserIds : Collections.emptySet();
		Set<String> excludedContacts = includeContacts && excludeBuddiesFromContacts ? buddyUserIds : Collections.emptySet();
		Set<String> excludedPlayers = new HashSet<>();
		if (excludeBuddiesFromUnconnected) {
			excludedPlayers.addAll(buddyUserIds);
		}
		if (excludeContactsFromUnconnected) {
			excludedPlayers.addAll(contactUserIds);
		}
		if (excludeSelf) {
			excludedPlayers.add(userId);
		}
		
		// First search is by buddies
		if (includeBuddies) {
			ListResult<Buddy> buddies = buddyElasticDao.getBuddyList(userId, appId, tenantId, searchTerm, email, new BuddyRelationType[] { BuddyRelationType.BUDDY },
					new BuddyRelationType[] { BuddyRelationType.BLOCKED }, excludedBuddies.toArray(new String[excludedBuddies.size()]), 
					favorite, BuddyListOrderType.NONE, listLimit, listOffset);
			result.addAll(buddies.getItems().stream().map(ApiPlayerListResult::new).collect(Collectors.toList()));
			listLimit = buddies.getSize() >= listLimit ? 0 : listLimit - buddies.getSize();
			listOffset = buddies.getTotal() >= listOffset ? 0 : listOffset - buddies.getTotal();
			total += buddies.getTotal();
		}

		// Second, by contacts
		if (includeContacts) {
			ListResult<Contact> contacts = contactElasticDao.getContactList(userId, appId, false, tenantId, searchTerm, null, null, null, 
					excludedContacts.toArray(new String[excludedContacts.size()]), listLimit, listOffset);
			result.addAll(contacts.getItems().stream().map(c -> new ApiPlayerListResult(appId, c)).collect(Collectors.toList()));
			listLimit = contacts.getSize() >= listLimit ? 0 : listLimit - contacts.getSize();
			listOffset = contacts.getTotal() >= listOffset ? 0 : listOffset - contacts.getTotal();
			total += contacts.getTotal();
		}
		
		// Third, by all players
		if (includeUnconnectedPlayers) {
			ListResult<Profile> players = profileElasticDao.searchProfiles(appId, searchTerm, null, 
					excludedPlayers.toArray(new String[excludedPlayers.size()]), orderType, listLimit, listOffset);
			result.addAll(players.getItems().stream().map(ApiPlayerListResult::new).collect(Collectors.toList()));
			total += players.getTotal();
		}		
		
		return new ListResult<>(limit, offset, total, result);
	}

	@Override
	public void addBuddies(String userId, String tenantId, Boolean favorite, boolean unblock, String... userIds) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);
		
		Arrays.stream(userIds).forEach(id -> 
			buddyDao.createBuddyRelation(userId, id, tenantId, BuddyRelationType.BUDDY, favorite)
		);
		
		if (unblock) {
			// Also, unblock
			unblockPlayers(userId, tenantId, userIds);
		}
	}

	@Override
	public void removeBuddies(String userId, String tenantId, String[] userIds) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);
		groupDao.getGroupsList(userId, tenantId).stream().forEach(group -> {
			String groupId = group.getGroupId();
			removePlayersFromGroup(userId, tenantId, groupId, userIds);
		});
		profileDao.removeUsersFromLastInvitedDuelOponents(userId, Arrays.asList(userIds));
		profileDao.removeUsersFromPausedDuelOponents(userId, Arrays.asList(userIds));
		Arrays.stream(userIds).forEach(id -> buddyDao.deleteBuddyRelation(userId, id, tenantId, BuddyRelationType.BUDDY));
	}

	@Override
	public ListResult<Buddy> listBuddies(String userId, String appId, String tenantId, Integer limit, Long offset, String searchTerm, String email, BuddyListOrderType orderType,
			BuddyRelationType[] includedRelationTypes, BuddyRelationType[] excludedRelationTypes, Boolean favorite) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);
		Validate.notNull(appId);
		return buddyElasticDao.getBuddyList(userId, appId, tenantId, searchTerm, email, includedRelationTypes, excludedRelationTypes, new String[] { userId }, favorite, orderType, limit, offset);
	}

	@Override
	public List<String> listAllBuddyIds(String userId, String appId, String tenantId,
			BuddyRelationType[] includedRelationTypes, BuddyRelationType[] excludedRelationTypes) {
		Validate.notNull(userId);
		Validate.notNull(appId);
		Validate.notNull(tenantId);
		return commonBuddyElasticDao.getAllBuddyIds(userId, appId, tenantId, includedRelationTypes, excludedRelationTypes, null);
	}

	@Override
	public boolean isBuddy(String userId, String appId, String tenantId, String buddyUserId) {
		Validate.notNull(userId);
		Validate.notNull(appId);
		Validate.notNull(tenantId);
		return CollectionUtils.isNotEmpty(commonBuddyElasticDao.getAllBuddyIds(userId, appId, tenantId, new BuddyRelationType[] { BuddyRelationType.BUDDY }, 
				new BuddyRelationType[] { BuddyRelationType.BLOCKED }, buddyUserId));
	}

	@Override
	public Group createGroup(String userId, String tenantId, String name, String type, String image, ClientInfo clientInfo, String... initialUserIds) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);
		Group group = groupDao.createGroup(userId, tenantId, name, type, image, getMemberUserIds(initialUserIds));
		
		//send notification to all added players
		notifyPlayers(initialUserIds, userId, tenantId, group.getGroupId(), clientInfo);
		return group;
	}

	@Override
	public Group getGroup(String userId, String tenantId, String groupId) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);
		return groupDao.getGroup(userId, tenantId, groupId, false);
	}

	@Override
	public Group updateGroup(String userId, String tenantId, String groupId, String name, String type, String image, String... userIdsToRemove) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);
		return groupDao.updateGroup(userId, tenantId, groupId, false, name, type, image, null, userIdsToRemove);
	}

	@Override
	public void deleteGroup(String userId, String tenantId, String groupId) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);
		groupDao.deleteGroup(userId, tenantId, groupId);
	}

	@Override
	public ListResult<Group> listGroups(String userId, String tenantId, int limit, long offset) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);
		return groupDao.getGroupsList(userId, tenantId, limit, offset);
	}

	@Override
	public ListResult<Group> listGroupsMemberOf(String userId, String tenantId, Boolean blocked,  int limit, long offset) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);
		return groupDao.getGroupsListMemberOf(userId, tenantId, blocked, limit, offset);
	}

	@Override
	public Group addPlayersToGroup(String userId, String tenantId, String groupId, String[] userIds, ClientInfo clientInfo) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);
		
		String[] addedUserIds = extractPlayersNotAddedAlready(userId, tenantId, groupId, userIds);
		
		Group group = groupDao.updateGroup(userId, tenantId, groupId, true, null, null, null, getMemberUserIds(addedUserIds));
		
		//send notification to all added players
		notifyPlayers(addedUserIds, userId, tenantId, groupId, clientInfo);
		
		return group;
	}

	@Override
	public Group removePlayersFromGroup(String userId, String tenantId, String groupId, String[] userIds) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);
		return groupDao.updateGroup(userId, tenantId, groupId, true, null, null, null, null, userIds);
	}

	@Override
	public ListResult<String> listGroupPlayers(String userId, String tenantId, String groupId, int limit, long offset,
											   boolean excludeGroupOwner) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);

		ListResult<String> groupPlayers;
		if (excludeGroupOwner) {
			groupPlayers = groupDao.listGroupUsers(userId, tenantId, groupId, null, false, limit, offset);
		} else {
			String groupOwnerId = groupDao.getGroup(userId, tenantId, groupId, false).getUserId();
			Profile ownerProfile = profileDao.getProfile(groupOwnerId);
			if (ownerProfile == null) {
				throw new F4MEntryNotFoundException("Profile for group owner with ID " + groupOwnerId + " not found.");
			}
			groupPlayers = groupDao.listGroupUsers(userId, tenantId, groupId, ownerProfile.getFullNameOrNickname(), true, limit, offset);
		}
        return groupPlayers;
	}

	@Override
	public boolean isPlayerInGroup(String userId, String tenantId, String groupId) {
		Validate.notNull(userId);
		Validate.notNull(groupId);
		return groupDao.isPlayerInGroup(userId, tenantId, groupId);
	}

	@Override
	public void blockGroups(String userId, String tenantId, String[] groupIds) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);
		Profile profile = profileDao.getProfile(userId);
		if (profile == null) {
			throw new F4MEntryNotFoundException("Profile with ID " + userId + " not found");
		}
		groupDao.addBlockedGroups(userId, tenantId, groupIds, profile.getFullNameOrNickname());
	}

	@Override
	public void unblockGroups(String userId, String tenantId, String[] groupIds) {
		Validate.notNull(userId);
		Validate.notNull(tenantId);
		groupDao.removeBlockedGroups(userId, tenantId, groupIds);
	}

	@Override
	public <R> void joinBuddies(List<R> results, String userId, List<String> usersIds,
			Function<Buddy, ApiBuddy> initApiElement, BiConsumer<R, ApiBuddy> setter) {
		List<Buddy> buddies = buddyDao.getBuddies(userId, usersIds);
		Map<String, Integer> positions = new HashMap<>(usersIds.size());
		for (int i = 0 ; i < usersIds.size() ; i++) {
			positions.put(usersIds.get(i), i);
		}
		for (Buddy buddy : buddies) {
			if (buddy != null) {
				Integer position = positions.get(buddy.getUserId());
				if (position != null) {
					setter.accept(results.get(position), initApiElement.apply(buddy));
				}
			}
		}
	}

	@Override
	public <R> void joinProfiles(List<R> results, List<String> usersIds, Function<Profile, ApiProfile> initApiElement,
			BiConsumer<R, ApiProfile> setter) {
		List<Profile> profiles = profileDao.getProfiles(usersIds);
		Map<String, Integer> positions = new HashMap<>(usersIds.size());
		for (int i = 0 ; i < usersIds.size() ; i++) {
			positions.put(usersIds.get(i), i);
		}
		for (Profile profile : profiles) {
			Integer position = positions.get(profile.getUserId());
			if (position != null) {
				setter.accept(results.get(position), initApiElement.apply(profile));
			}
		}
	}

	@Override
	public List<ApiProfile> getProfiles(List<String> usersIds) {
		List<Profile> profiles = profileDao.getProfiles(usersIds);
		return profiles.stream().filter(p -> p != null).map(ApiProfile::new).collect(Collectors.toList());
	}

	private Map<String, String> getMemberUserIds(String... userIds) {
		Map<String, String> memberUserIds = new HashMap<>(userIds == null ? 0 : userIds.length);
		Arrays.stream(userIds).forEach(userId -> {
			Profile profile = profileDao.getProfile(userId);
			if (profile == null) {
				throw new F4MEntryNotFoundException("Profile with ID " + userId + " not found");
			}
			memberUserIds.put(userId, profile.getFullNameOrNickname());
		});
		return memberUserIds;
	}

	@Override
	public Pair<Integer, Integer> resyncIndexes() {
		int resyncedContactCount = contactDao.resyncIndex();
		int resyncedBuddyCount = buddyDao.resyncIndex();
		return Pair.of(resyncedContactCount, resyncedBuddyCount);
	}

	@Override
	public Pair<Long, Long> getRankAmongBuddies(String userId, String appId, String tenantId, Double handicap) {
		return buddyElasticDao.getRank(userId, appId, tenantId, handicap);
	}

	@Override
	public Pair<Long, Long> getRankAmongPlayers(String appId, String userId, Double handicap) {
		return profileElasticDao.getRank(appId, userId, handicap);
	}

	@Override
	public void moveBuddies(String sourceUserId, String targetUserId) {
		commonBuddyElasticDao.getAllBuddyIds(sourceUserId, null, null, null, null, null).forEach(buddyId -> 
			buddyDao.moveBuddy(sourceUserId, buddyId, targetUserId, buddyId));
		buddyElasticDao.getAllOwnerIds(sourceUserId).forEach(ownerId -> 
			buddyDao.moveBuddy(ownerId, sourceUserId, ownerId, targetUserId));
	}

	@Override
	public void moveContacts(String sourceUserId, Profile targetUser, Collection<String> tenantIds) {
		tenantIds.forEach(tenantId -> {
			long offset = 0;
			List<Contact> contacts;
			// Move contacts to new owner
			do {
				contacts = contactElasticDao.getContactList(sourceUserId, null, false, tenantId, null, null, null, null, null, BATCH_SIZE, offset).getItems();
				contactDao.moveContacts(contacts, targetUser.getUserId());
				offset += BATCH_SIZE;
			} while (contacts.size() == BATCH_SIZE); // Continue while whole batch was retrieved
			
			// Update references to moved user in contacts
			offset = 0;
			do {
				contacts = contactElasticDao.getContactList(null, null, false, tenantId, null, null, null, new String[] { sourceUserId }, null, BATCH_SIZE, offset).getItems();
				contacts.forEach(c -> {
					boolean movingContactFromCorrectUser = c.getUserId().equals(sourceUserId);
					assert(movingContactFromCorrectUser);
					c.setUserId(targetUser.getUserId());
					contactDao.createOrUpdateContact(c);
				});
			} while (contacts.size() == BATCH_SIZE); // Continue while whole batch was retrieved
		});
	}

	@Override
	public void moveGroups(String sourceUserId, Profile targetUser, Collection<String> tenantIds) {
		groupDao.moveGroups(sourceUserId, targetUser.getUserId(), tenantIds, targetUser.getFullNameOrNickname());
	}

	private String[] extractPlayersNotAddedAlready(String userId, String tenantId, String groupId, String[] userIds) {
		Group existingGroup = groupDao.getGroup(userId, tenantId, groupId, false);
		Set<String> existingPlayers = existingGroup.getMemberUserIds().keySet();
		List<String> newPlayers = new ArrayList<>(Arrays.asList(userIds));
		newPlayers.removeAll(existingPlayers);
		String[] addedUserIds = newPlayers.toArray(new String[newPlayers.size()]);
		return addedUserIds;
	}

	private void notifyPlayers(String[] playerIds, String inviterId, String tenantId, String groupId,
			ClientInfo clientInfo) {
		Stream.of(playerIds).forEach(playerId -> notifyPlayer(tenantId, playerId, groupId, clientInfo, inviterId));
	}

	private void notifyPlayer(String tenantId, String playerId, String groupId, ClientInfo clientInfo, String userId) {
		PlayerAddedToGroupNotification notification = new PlayerAddedToGroupNotification(tenantId, playerId, groupId);
		userMessageCommunicator.pushMessageToUser(playerId, notification, clientInfo, Messages.FRIEND_PLAYER_ADDED_TO_GROUP_PUSH, groupId, userId);
	}

}
