package de.ascendro.f4m.service.friend;

import static de.ascendro.f4m.service.friend.model.api.player.PlayerListResultType.BUDDY;
import static de.ascendro.f4m.service.friend.model.api.player.PlayerListResultType.CONTACT;
import static de.ascendro.f4m.service.friend.model.api.player.PlayerListResultType.PLAYER;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.elastic.ElasticsearchTestNode;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDao;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileElasticDao;
import de.ascendro.f4m.server.profile.CommonProfileElasticDaoImpl;
import de.ascendro.f4m.server.profile.PlayerListOrderType;
import de.ascendro.f4m.server.util.ElasticUtil;
import de.ascendro.f4m.service.friend.config.FriendManagerConfig;
import de.ascendro.f4m.service.friend.dao.BuddyElasticDao;
import de.ascendro.f4m.service.friend.dao.BuddyElasticDaoImpl;
import de.ascendro.f4m.service.friend.dao.ContactElasticDao;
import de.ascendro.f4m.service.friend.dao.ContactElasticDaoImpl;
import de.ascendro.f4m.service.friend.model.Buddy;
import de.ascendro.f4m.service.friend.model.Contact;
import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.friend.model.api.player.PlayerListResultType;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.util.register.ServiceMonitoringRegister;

public class BuddyManagerImplTest {

	private static final int ELASTIC_PORT = 9208;

	private static final Pair<PlayerListResultType, String> P1 = Pair.of(PLAYER, "uid1");
	private static final Pair<PlayerListResultType, String> P2 = Pair.of(PLAYER, "uid2");
	private static final Pair<PlayerListResultType, String> P3 = Pair.of(PLAYER, "uid3");
	private static final Pair<PlayerListResultType, String> P4 = Pair.of(PLAYER, "uid4");
	private static final Pair<PlayerListResultType, String> P5 = Pair.of(PLAYER, "uid5");
	
	private static final Pair<PlayerListResultType, String> C1 = Pair.of(CONTACT, "c1");
	private static final Pair<PlayerListResultType, String> C2 = Pair.of(CONTACT, "c2");
	private static final Pair<PlayerListResultType, String> C3 = Pair.of(CONTACT, "c3");
	private static final Pair<PlayerListResultType, String> C4 = Pair.of(CONTACT, "c4");

	private static final Pair<PlayerListResultType, String> B1 = Pair.of(BUDDY, P1.getRight());
	private static final Pair<PlayerListResultType, String> B2 = Pair.of(BUDDY, P2.getRight());
	private static final Pair<PlayerListResultType, String> B3 = Pair.of(BUDDY, P3.getRight());
	private static final Pair<PlayerListResultType, String> B4 = Pair.of(BUDDY, P4.getRight());
	
	private static final String APP_ID = "1";
	private static final String TENANT_ID = "tenant1";

	private static final String GROUP_ID = "g1";
	
	@Rule
	public ElasticsearchTestNode elastic = new ElasticsearchTestNode(ELASTIC_PORT);
	
	private ElasticClient client;
	
	private ContactElasticDao contactDao;
	private BuddyElasticDao buddyDao;
	private CommonBuddyElasticDao commonBuddyDao;
	private CommonProfileElasticDao profileDao;
	private BuddyManager buddyManager;
	
	@Before
	public void setUp() throws Exception {
		FriendManagerConfig config = new FriendManagerConfig();
		config.setProperty(ElasticConfigImpl.ELASTIC_SERVER_HOSTS, "http://localhost:" + ELASTIC_PORT);
		config.setProperty(ElasticConfigImpl.ELASTIC_HEADER_REFRESH, ElasticConfigImpl.ELASTIC_HEADER_VALUE_WAIT_FOR);
		ElasticUtil elasticUtil = new ElasticUtil();
		client = new ElasticClient(config, elasticUtil, new ServiceMonitoringRegister());
		profileDao = new CommonProfileElasticDaoImpl(client, config, elasticUtil);
		contactDao = new ContactElasticDaoImpl(client, config, elasticUtil);
		buddyDao = new BuddyElasticDaoImpl(client, config, elasticUtil);
		commonBuddyDao = new CommonBuddyElasticDaoImpl(client, config);
		buddyManager = new BuddyManagerImpl(null, null, profileDao, null, contactDao, null, buddyDao, commonBuddyDao, null, null);
	}
	
	@After
	public void shutdown() {
		client.close();
	}
	
	@Test
	public void testPlayerList() {
		prepareData();

		// Buddies:
		// B1 => BLOCKED
		// B2 => BUDDY, BLOCKED
		// B3 => BLOCKED_BY
		// B4 => BUDDY, BLOCKED_BY
		
		// Contacts:
		// C1 => Con ela kole mar => P1
		// C2 => Peter None meee => P2
		// C3 => Ozzy Molos => null
		// C4 => Misha Toto => P4

		// Players:
		// P1 => Con ela kole mar
		// P2 => Peter None meee
		// P3 => Ozzy Molos
		// P4 => Superman
		// P5 => Noman

		// Test include/exclude types
		assertContents(null, true, true, true, false /* excludeBuddiesFromContacts */, false /* excludeBuddiesFromUnconnected */, 
				false /* excludeContactsFromUnconnected */,  false /* excludeContactsFromBuddies */, 20, 0, B4, C1, C4, C3, C2, P1, P5, P3, P2, P4);
		assertContents(null, false, true, true, false /* excludeBuddiesFromContacts */, false /* excludeBuddiesFromUnconnected */, 
				false /* excludeContactsFromUnconnected */,  false /* excludeContactsFromBuddies */, 20, 0, C1, C4, C3, C2, P1, P5, P3, P2, P4);
		assertContents(null, true, false, true, false /* excludeBuddiesFromContacts */, false /* excludeBuddiesFromUnconnected */, 
				false /* excludeContactsFromUnconnected */,  false /* excludeContactsFromBuddies */, 20, 0, B4, P1, P5, P3, P2, P4);
		assertContents(null, true, true, false, false /* excludeBuddiesFromContacts */, false /* excludeBuddiesFromUnconnected */, 
				false /* excludeContactsFromUnconnected */,  false /* excludeContactsFromBuddies */, 20, 0, B4, C1, C4, C3, C2);
		
		// Test offset/limit
		assertContents(null, false, false, true, false /* excludeBuddiesFromContacts */, false /* excludeBuddiesFromUnconnected */, 
				false /* excludeContactsFromUnconnected */,  false /* excludeContactsFromBuddies */, 2, 1, P5, P3);
		assertContents(null, true, true, true, false /* excludeBuddiesFromContacts */, false /* excludeBuddiesFromUnconnected */, 
				false /* excludeContactsFromUnconnected */,  false /* excludeContactsFromBuddies */, 4, 3, C3, C2, P1, P5);

		// Test search term
		assertContents("pet", true, true, true, false /* excludeBuddiesFromContacts */, false /* excludeBuddiesFromUnconnected */, 
				false /* excludeContactsFromUnconnected */,  false /* excludeContactsFromBuddies */, 20, 0, C2, P2);
		assertContents("super", true, true, true, false /* excludeBuddiesFromContacts */, false /* excludeBuddiesFromUnconnected */, 
				false /* excludeContactsFromUnconnected */,  false /* excludeContactsFromBuddies */, 20, 0, B4, P4);
		
		// Test exclude results of one list from results of other
		assertContents(null, true, true, true, true /* excludeBuddiesFromContacts */, false /* excludeBuddiesFromUnconnected */, 
				false /* excludeContactsFromUnconnected */,  false /* excludeContactsFromBuddies */, 20, 0, B4, C1, C3, C2, P1, P5, P3, P2, P4);
		assertContents(null, true, true, true, true /* excludeBuddiesFromContacts */, false /* excludeBuddiesFromUnconnected */, 
				false /* excludeContactsFromUnconnected */,  true /* excludeContactsFromBuddies */, 20, 0, B4, C1, C3, C2, P1, P5, P3, P2, P4);
		assertContents(null, true, true, true, false /* excludeBuddiesFromContacts */, false /* excludeBuddiesFromUnconnected */, 
				false /* excludeContactsFromUnconnected */,  true /* excludeContactsFromBuddies */, 20, 0, C1, C4, C3, C2, P1, P5, P3, P2, P4);
		assertContents(null, true, true, true, false /* excludeBuddiesFromContacts */, true /* excludeBuddiesFromUnconnected */, 
				false /* excludeContactsFromUnconnected */,  false /* excludeContactsFromBuddies */, 20, 0, B4, C1, C4, C3, C2, P1, P5, P3, P2);
		assertContents(null, true, true, true, false /* excludeBuddiesFromContacts */, false /* excludeBuddiesFromUnconnected */, 
				true /* excludeContactsFromUnconnected */,  false /* excludeContactsFromBuddies */, 20, 0, B4, C1, C4, C3, C2, P5, P3);
		assertContents(null, true, true, true, true /* excludeBuddiesFromContacts */, true /* excludeBuddiesFromUnconnected */, 
				true /* excludeContactsFromUnconnected */,  true /* excludeContactsFromBuddies */, 20, 0, B4, C1, C3, C2, P5, P3);
	}

	private void assertContents(String searchTerm, boolean includeBuddies, boolean includeContacts, boolean includeUnconnectedPlayers, boolean excludeBuddiesFromContacts, 
			boolean excludeBuddiesFromUnconnected, boolean excludeContactsFromUnconnected, boolean excludeContactsFromBuddies, int limit, long offset, 
			@SuppressWarnings("rawtypes") Pair... expected) {
		assertThat(buddyManager.listPlayers(P1.getRight(), APP_ID, TENANT_ID, searchTerm, PlayerListOrderType.NONE, includeBuddies, null, includeContacts, includeUnconnectedPlayers, 
				excludeBuddiesFromContacts, excludeBuddiesFromUnconnected, excludeContactsFromUnconnected, excludeContactsFromBuddies, false, limit, offset).getItems()
				.stream().map(i -> Pair.of(i.getResultType(), i.getId())).collect(Collectors.toList()),
				expected == null || expected.length == 0 ? hasSize(0) : contains(expected));
	}

	private void prepareData() {
		prepareProfileData();
		prepareContactData();
		prepareBuddyData();
	}
	
	private void prepareProfileData() {
		prepareProfile(P1, true, "Con ela", "kole mar", "Nikki");
		prepareProfile(P2, true, "Peter", "None meee", "Nikki");
		prepareProfile(P3, true, "Ozzy", "Molos", "Nikki");
		prepareProfile(P4, false, "Anatoly", "Hole", "Superman");
		prepareProfile(P5, false, "Noman", "Noman", "Noman");
	}

	private void prepareProfile(Pair<PlayerListResultType, String> userId, boolean showFullName, String firstName, String lastName, String nickname) {
		Profile profile = new Profile();
		profile.setUserId(userId.getRight());
		profile.setShowFullName(showFullName);
		profile.addApplication(APP_ID);
		
		ProfileUser person = new ProfileUser();
		person.setFirstName(firstName);
		person.setLastName(lastName);
		person.setNickname(nickname);
		profile.setPersonWrapper(person);
		
		ProfileAddress address = new ProfileAddress();
		address.setCountry("LV");
		address.setCity("Riga");
		profile.setAddress(address);
		
		profileDao.createOrUpdate(profile);
	}
	
	private void prepareContactData() {
		prepareContact(C1, "Con ela", "kole mar", P1);
		prepareContact(C2, "Peter", "None meee", P2);
		prepareContact(C3, "Ozzy", "Molos", null);
		prepareContact(C4, "Misha", "Toto", P4);
	}

	private void prepareContact(Pair<PlayerListResultType, String> id, String firstName, String lastName, 
			Pair<PlayerListResultType, String> userId) {
		Contact contact = new Contact();
		contact.setOwnerId(P1.getRight());
		contact.setContactId(id.getRight());
		contact.setEmails("some@email.lv", "oth@oo.com");
		contact.setFirstName(firstName);
		contact.setLastName(lastName);
		contact.setPhoneNumbers("291589125");
		contact.setSentInvitationTextAndGroup(APP_ID, "Please join", GROUP_ID);
		contact.setUserId(userId == null ? null : userId.getRight());
		contact.setTenantIds(TENANT_ID);
		contactDao.createOrUpdate(contact);
	}
	
	private void prepareBuddyData() {
		prepareBuddy(B1, new BuddyRelationType[] { BuddyRelationType.BLOCKED });
		prepareBuddy(B2, new BuddyRelationType[] { BuddyRelationType.BUDDY, BuddyRelationType.BLOCKED });
		prepareBuddy(B3, new BuddyRelationType[] { BuddyRelationType.BLOCKED_BY });
		prepareBuddy(B4, new BuddyRelationType[] { BuddyRelationType.BUDDY, BuddyRelationType.BLOCKED_BY });
	}

	private void prepareBuddy(Pair<PlayerListResultType, String> userId, BuddyRelationType[] relationTypes) {
		Profile profile = profileDao.searchProfiles(APP_ID, null, new String[] { userId.getRight() }, null, 
				PlayerListOrderType.NONE, 1, 0).getItems().get(0);

		Buddy buddy = new Buddy();
		buddy.setOwnerId(P1.getRight());
		buddy.setUserId(userId.getRight());
		buddy.addRelationTypes(relationTypes);
		buddy.setInteractionCount(5);
		buddy.setLastInteractionTimestamp(ZonedDateTime.of(2015, 11, 1, 10, 0, 0, 0, ZoneOffset.UTC));
		buddy.setLastPlayedGameInstanceId("gameInstanceId");
		buddy.addTenantIds(TENANT_ID);
		buddyDao.createOrUpdate(profile.getSearchName(), buddy);
		
	}
	
}
