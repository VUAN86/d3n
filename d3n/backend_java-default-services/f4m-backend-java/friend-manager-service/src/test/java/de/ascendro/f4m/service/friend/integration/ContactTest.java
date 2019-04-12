package de.ascendro.f4m.service.friend.integration;

import static de.ascendro.f4m.service.friend.builder.BuddyBuilder.createBuddy;
import static de.ascendro.f4m.service.friend.builder.ContactBuilder.createContact;
import static de.ascendro.f4m.service.friend.builder.ContactImportPhonebookRequestBuilder.createImportPhonebookRequest;
import static de.ascendro.f4m.service.friend.builder.ContactInviteNewRequestBuilder.createContactInviteNewRequest;
import static de.ascendro.f4m.service.friend.builder.ContactInviteOrResendRequestBuilder.createContactInviteOrResendRequest;
import static de.ascendro.f4m.service.friend.builder.ContactListRequestBuilder.createContactListRequest;
import static de.ascendro.f4m.service.friend.builder.ProfileBuilder.createProfile;
import static de.ascendro.f4m.service.integration.RetriedAssert.assertWithWait;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.APP_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticsearchTestNode;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDao;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileElasticDao;
import de.ascendro.f4m.server.profile.model.AppConfig;
import de.ascendro.f4m.server.profile.model.AppConfigBuilder;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.service.auth.AuthMessageTypes;
import de.ascendro.f4m.service.auth.model.register.InviteUserByEmailRequest;
import de.ascendro.f4m.service.auth.model.register.InviteUserByEmailResponse;
import de.ascendro.f4m.service.exception.UnexpectedTestException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypes;
import de.ascendro.f4m.service.friend.dao.BuddyAerospikeDao;
import de.ascendro.f4m.service.friend.dao.ContactAerospikeDao;
import de.ascendro.f4m.service.friend.dao.ContactElasticDao;
import de.ascendro.f4m.service.friend.dao.GroupAerospikeDAO;
import de.ascendro.f4m.service.friend.model.Buddy;
import de.ascendro.f4m.service.friend.model.Contact;
import de.ascendro.f4m.service.friend.model.Group;
import de.ascendro.f4m.service.friend.model.api.BuddyRelationType;
import de.ascendro.f4m.service.friend.model.api.contact.ApiContactListResult;
import de.ascendro.f4m.service.friend.model.api.contact.ApiInvitee;
import de.ascendro.f4m.service.friend.model.api.contact.ContactImportPhonebookResponse;
import de.ascendro.f4m.service.friend.model.api.contact.ContactInviteNewResponse;
import de.ascendro.f4m.service.friend.model.api.contact.ContactInviteResponse;
import de.ascendro.f4m.service.friend.model.api.contact.ContactListResponse;
import de.ascendro.f4m.service.friend.model.api.contact.ContactResendInvitationResponse;
import de.ascendro.f4m.service.friend.model.api.contact.ResyncResponse;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileIdentifierType;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class ContactTest extends FriendManagerTestBase {
	
	private static final String DOWNLOAD_URL = "https://play.google.com/store/apps/details?id=com.quizdom";

	private static final Logger LOGGER = LoggerFactory.getLogger(ContactTest.class);
	
	private static final int ELASTIC_PORT = 9206;
	
	private static final String REQ_INVITE = "contactInvite";
	private static final String REQ_RESEND = "contactResendInvitation";

	private static final String CONTACT_ID_1 = "contact_id_1";
	private static final String CONTACT_ID_2 = "contact_id_2";
	private static final String CONTACT_ID_3 = "contact_id_3";
	private static final String CONTACT_ID_4 = "contact_id_4";
	private static final String EMAIL_1 = "test_email_1@example.com";
	private static final String EMAIL_2 = "test_email_2@example.com";
	private static final String EMAIL_3 = "test_email_3@example.com";
	private static final String INVITATION_TEXT = "Join us!";
	
	private static final String USER_ID_1 = "user_id_1";
	private static final String USER_ID_2 = "user_id_2";
	private static final String USER_ID_3 = "user_id_3";
	
	private ContactAerospikeDao contactDao;
	private ContactElasticDao contactElasticDao;
	private CommonProfileElasticDao profileDao;
	private BuddyAerospikeDao buddyDao;
	private CommonBuddyElasticDao buddyElasticDao;
	private GroupAerospikeDAO groupDao;
	private ProfilePrimaryKeyUtil profilePrimaryKeyUtil;
	private ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao;
	private JsonMessage<? extends JsonMessageContent> recievedInviteMessage;
	
	@BeforeClass
	public static void setUpClass() {
		F4MServiceWithMockIntegrationTestBase.setUpClass();
		System.setProperty(ElasticConfigImpl.ELASTIC_SERVER_HOSTS, "http://localhost:" + ELASTIC_PORT);
		System.setProperty(ElasticConfigImpl.ELASTIC_HEADER_REFRESH, ElasticConfigImpl.ELASTIC_HEADER_VALUE_WAIT_FOR);
	}
	
	@Rule
	public ElasticsearchTestNode elastic = new ElasticsearchTestNode(ELASTIC_PORT);
	
	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		contactDao = injector.getInstance(ContactAerospikeDao.class);
		contactElasticDao = injector.getInstance(ContactElasticDao.class);
		profileDao = injector.getInstance(CommonProfileElasticDao.class);
		groupDao = injector.getInstance(GroupAerospikeDAO.class);
		buddyDao = injector.getInstance(BuddyAerospikeDao.class);
		buddyElasticDao = injector.getInstance(CommonBuddyElasticDao.class);
		profilePrimaryKeyUtil = injector.getInstance(ProfilePrimaryKeyUtil.class);
		applicationConfigurationAerospikeDao = injector.getInstance(ApplicationConfigurationAerospikeDao.class);
	}
	
	@Test
	public void testEmptyContactList() throws Exception {
		String requestJson = createContactListRequest().buildRequestJson();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.CONTACT_LIST_RESPONSE);

		JsonMessage<ContactListResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.CONTACT_LIST_RESPONSE);
		ContactListResponse content = response.getContent();
		assertNotNull(content);
		assertTrue(content.getItems().isEmpty());
	}
	
	@Test
	public void testContactListWithoutAdditionalInfo() throws Exception {
		Contact contact1 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_1, TENANT_ID).buildContact();
		Contact contact2 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_2, TENANT_ID).buildContact();
		saveContacts(contact1, contact2);
		
		List<Contact> contacts = Arrays.asList(contact1, contact2);
		
		String requestJson = createContactListRequest().buildRequestJson();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.CONTACT_LIST_RESPONSE);
		
		testContactListResponse(contacts, Collections.emptyList(), Collections.emptyList());
	}
	
	@Test
	public void testContactListHasApp() throws Exception {
		Profile profile1 = createProfile(USER_ID_1).withApplications(APP_ID, "another_app_id").buildProfile();
		Profile profile2 = createProfile(USER_ID_2).withApplications("another_app_id", APP_ID).buildProfile();
		saveProfiles(profile1, profile2);
		
		Contact contact1 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_1, TENANT_ID).withUserId(USER_ID_1).buildContact();
		Contact contact2 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_2, TENANT_ID).withUserId(USER_ID_3).buildContact();
		Contact contact3 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_3, TENANT_ID).withUserId(USER_ID_2).buildContact();
		saveContacts(contact1, contact2, contact3);
		
		List<Contact> contactsWithApp = Arrays.asList(contact1, contact3);
		
		String requestJson = createContactListRequest().withHasApp().buildRequestJson();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.CONTACT_LIST_RESPONSE);
		
		testContactListResponse(contactsWithApp, Collections.emptyList(), Collections.emptyList());
	}
	
	@Test
	public void testContactListHasProfile() throws Exception {
		Contact contact1 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_1, TENANT_ID).buildContact();
		Contact contact2 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_2, TENANT_ID).withUserId(USER_ID_1).buildContact();
		Contact contact3 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_3, TENANT_ID).withUserId(USER_ID_2).buildContact();
		saveContacts(contact1, contact2, contact3);
		
		List<Contact> contactsWithProfile = Arrays.asList(contact2, contact3);
		
		String requestJson = createContactListRequest().withHasProfile().buildRequestJson();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.CONTACT_LIST_RESPONSE);
		
		testContactListResponse(contactsWithProfile, Collections.emptyList(), Collections.emptyList());
	}
	
	@Test
	public void testContactListHasInvitation() throws Exception {
		Contact contact1 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_1, TENANT_ID).withSentInvitationText("You are invited!", APP_ID).buildContact();
		Contact contact2 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_2, TENANT_ID).withSentInvitationText("To be or not to be?", APP_ID).buildContact();
		Contact contact3 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_3, TENANT_ID).buildContact();
		saveContacts(contact1, contact2, contact3);
		
		List<Contact> contactsWithInvitation = Arrays.asList(contact1, contact2);
		
		String requestJson = createContactListRequest().withHasInvitation().buildRequestJson();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.CONTACT_LIST_RESPONSE);
		
		testContactListResponse(contactsWithInvitation, Collections.emptyList(), Collections.emptyList());
	}
	
	@Test
	public void testContactListIncludeBuddies() throws Exception {
		Buddy buddy1 = createBuddy(ANONYMOUS_USER_ID, USER_ID_1, TENANT_ID).buildBuddy();
		Buddy buddy2 = createBuddy(ANONYMOUS_USER_ID, USER_ID_2, TENANT_ID).buildBuddy();
		saveBuddies(ANONYMOUS_USER_ID, buddy1, buddy2);
		
		Contact contact1 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_1, TENANT_ID).withUserId(buddy1.getUserId()).buildContact();
		Contact contact2 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_2, TENANT_ID).withUserId(buddy2.getUserId()).buildContact();
		saveContacts(contact1, contact2);
				
		List<Contact> contacts = Arrays.asList(contact1, contact2);
		List<Buddy> buddies = Arrays.asList(buddy1, buddy2);
		
		String requestJson = createContactListRequest().withIncludeBuddyInfo().buildRequestJson();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.CONTACT_LIST_RESPONSE);
		
		testContactListResponse(contacts, buddies, Collections.emptyList());
	}
	
	@Test
	public void testContactListIncludeProfiles() throws Exception {
		Profile profile1 = createProfile(USER_ID_1).buildProfile();
		Profile profile2 = createProfile(USER_ID_2).buildProfile();
		saveProfiles(profile1, profile2);
		
		Contact contact1 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_1, TENANT_ID).withUserId(profile1.getUserId()).buildContact();
		Contact contact2 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_2, TENANT_ID).withUserId(profile2.getUserId()).buildContact();
		saveContacts(contact1, contact2);
				
		List<Contact> contacts = Arrays.asList(contact1, contact2);
		List<Profile> profiles = Arrays.asList(profile1, profile2);
		
		String requestJson = createContactListRequest().withIncludeProfileInfo().buildRequestJson();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.CONTACT_LIST_RESPONSE);
		
		testContactListResponse(contacts, Collections.emptyList(), profiles);
	}
	
	@Test
	public void testContactImportPhonebook_NoMerge() throws Exception {
		prepareContactsForImportAndSyncTest();
		
		String requestJson = createImportPhonebookRequest().buildRequestJson();
		sendImportPhonebookAndVerifyResults(requestJson, 2);
		verifyContacts(6);
	}

	@Test
	public void testContactImportPhonebook_MergeByNames() throws Exception {
		prepareContactsForImportAndSyncTest();
		
		// Only first name matching => not enough to merge
		String requestJson = createImportPhonebookRequest().withFirstName("testMan").buildRequestJson();
		sendImportPhonebookAndVerifyResults(requestJson, 2);
		verifyContacts(6); // two new contacts added
		testClientReceivedMessageCollector.clearReceivedMessageList();
		
		// Only last name matching => not enough to merge
		requestJson = createImportPhonebookRequest().withLastName("Munchie").buildRequestJson();
		sendImportPhonebookAndVerifyResults(requestJson, 2);
		verifyContacts(7); // one new contact added, one merged, since same with the one in previous request
		testClientReceivedMessageCollector.clearReceivedMessageList();

		// First name and last name matching => enough to merge
		requestJson = createImportPhonebookRequest().withFirstName("testman ").withLastName(" munchie").withEmails("added@a.a").withPhoneNumbers("added").buildRequestJson();
		sendImportPhonebookAndVerifyResults(requestJson, 2);
		Map<String, Contact> contacts = verifyContacts(7); // No new contacts added
		Contact merged = contacts.get(CONTACT_ID_1);
		assertArrayEquals(new String[] { "added@a.a" }, merged.getEmails());
		assertArrayEquals(new String[] { "added" }, merged.getPhoneNumbers());
	}

	@Test
	public void testContactImportPhonebook_MergeByPhone_notMatched() throws Exception {
		Profile profile1 = createProfile(USER_ID_2).withPhones("existingProfilePhone").buildProfile();
		saveProfiles(profile1);

		prepareContactsForImportAndSyncTest();
		
		// Only phone matching => not enough to merge
		String requestJson = createImportPhonebookRequest().withFirstName("testMan").withLastName("Beata").withPhoneNumbers("29292929", "new").withEmails("new").buildRequestJson();
		sendImportPhonebookAndVerifyResults(requestJson, 2);
		verifyContacts(6); // two new contacts added
		testClientReceivedMessageCollector.clearReceivedMessageList();
	}

	@Test
	public void testContactImportPhonebook_MergeByPhone_matched() throws Exception {
		Profile profile1 = createProfile(USER_ID_2).withTenants(TENANT_ID).withPhones("existingProfilePhone").buildProfile();
		saveProfiles(profile1);

		prepareContactsForImportAndSyncTest();
		
		// Phone and first name matching => enough to merge
		String requestJson = createImportPhonebookRequest().withFirstName("Bum").withLastName("Beata").withPhoneNumbers("29292929", "existingProfilePhone").withEmails("new").buildRequestJson();
		sendImportPhonebookAndVerifyResults(requestJson, 2);
		Map<String, Contact> contacts = verifyContacts(5); // Only one new contact added
		Contact merged = contacts.get(CONTACT_ID_4);
		Thread.sleep(1000);
		assertArrayEquals(new String[] { "new" }, merged.getEmails());
		assertArrayEquals(new String[] { "29292929", "111333111", "existingProfilePhone" }, merged.getPhoneNumbers());
		assertEquals("Bum", merged.getFirstName());
		assertEquals("Beata", merged.getLastName());
		assertEquals(USER_ID_2, merged.getUserId());
		assertTrue(buddyDao.getBuddies(KeyStoreTestUtil.ANONYMOUS_USER_ID, Arrays.asList(USER_ID_2)).size() == 1);
		testClientReceivedMessageCollector.clearReceivedMessageList();
	}

	@Test
	public void testContactImportPhonebook_MergeByEmail_notMatched() throws Exception {
		Profile profile1 = createProfile(USER_ID_2).withEmails("existingProfileEmail").buildProfile();
		saveProfiles(profile1);
		
		prepareContactsForImportAndSyncTest();
		
		// Only email matching => not enough to merge
		String requestJson = createImportPhonebookRequest().withFirstName("testMan").withLastName("Beata").withPhoneNumbers("new").withEmails("bebe@ba.ba", "new").buildRequestJson();
		sendImportPhonebookAndVerifyResults(requestJson, 2);
		verifyContacts(6); // two new contacts added
		testClientReceivedMessageCollector.clearReceivedMessageList();
	}

	@Test
	public void testContactImportPhonebook_MergeByEmail_matched() throws Exception {
		Profile profile1 = createProfile(USER_ID_2).withEmails("existingProfileEmail").buildProfile();
		saveProfiles(profile1);
		
		prepareContactsForImportAndSyncTest();
		
		// Phone and first name matching => enough to merge
		String requestJson = createImportPhonebookRequest().withFirstName("Bum").withLastName("Mamma").withPhoneNumbers("new").withEmails("bebe@ba.ba", "existingProfileEmail").buildRequestJson();
		sendImportPhonebookAndVerifyResults(requestJson, 2);
		Map<String, Contact> contacts = verifyContacts(5); // Only one new contact added
		Contact merged = contacts.get(CONTACT_ID_3);
		assertArrayEquals(new String[] { "test@test.te", "bebe@ba.ba", "existingProfileEmail" }, merged.getEmails());
		assertArrayEquals(new String[] { "29292929", "new" }, merged.getPhoneNumbers());
		assertEquals("Bum", merged.getFirstName());
		assertEquals("Mamma", merged.getLastName());
		assertEquals(USER_ID_2, merged.getUserId());
		testClientReceivedMessageCollector.clearReceivedMessageList();
	}

	@Test
	public void testContactImportPhonebook_MergeByEmail_profileAlreadyMatched() throws Exception {
		Profile profile1 = createProfile(USER_ID_2).withEmails("existingProfileEmail").buildProfile();
		saveProfiles(profile1);
		
		prepareContactsForImportAndSyncTest();
		
		// Phone and first name matching => enough to merge
		String requestJson = createImportPhonebookRequest().withFirstName("Bum").withLastName("Hummer").withPhoneNumbers("new").withEmails("test@test.te", "existingProfileEmail").buildRequestJson();
		sendImportPhonebookAndVerifyResults(requestJson, 2);
		Map<String, Contact> contacts = verifyContacts(5); // Just one new contact added
		Contact merged = contacts.get(CONTACT_ID_2);
		assertArrayEquals(new String[] { "test@test.te", "existingProfileEmail" }, merged.getEmails());
		assertArrayEquals(new String[] { "new" }, merged.getPhoneNumbers());
		assertEquals("Bum", merged.getFirstName());
		assertEquals("Hummer", merged.getLastName());
		assertEquals(USER_ID_1, merged.getUserId()); // Not overridden with USER_ID_2
		testClientReceivedMessageCollector.clearReceivedMessageList();
	}

	@Test
	public void testSyncContacts() throws Exception {
		Profile profile1 = createProfile(USER_ID_2).withEmails("test@test.te").buildProfile();
		Profile profile2 = createProfile(USER_ID_3).withPhones("111333111").buildProfile();
		saveProfiles(profile1, profile2);
		
		prepareContactsForImportAndSyncTest();
		
		String requestJson = jsonLoader.getPlainTextJsonFromResources("resyncRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.RESYNC_RESPONSE);
		JsonMessage<ResyncResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.RESYNC_RESPONSE);
		assertEquals(4, response.getContent().getResyncedContactCount());
		assertEquals(2, response.getContent().getNewContactMatchesCount());
		
		Map<String, Contact> contacts = verifyContacts(4);
		assertEquals(USER_ID_1, contacts.get(CONTACT_ID_2).getUserId());
		assertEquals(USER_ID_2, contacts.get(CONTACT_ID_3).getUserId());
		assertEquals(USER_ID_3, contacts.get(CONTACT_ID_4).getUserId());
	}
	
	private void prepareContactsForImportAndSyncTest() throws Exception {
		Profile profile1 = createProfile(USER_ID_1).buildProfile();
		saveProfiles(profile1);

		Contact contact1 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_1, TENANT_ID).withFirstName("testMan").withLastName("Munchie").buildContact();
		Contact contact2 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_2, TENANT_ID).withUserId(profile1.getUserId())
				.withFirstName("Beata").withLastName("Hummer").withEmails("test@test.te").buildContact();
		Contact contact3 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_3, TENANT_ID).withFirstName("Anna").withLastName("Mamma").withEmails("test@test.te", "bebe@ba.ba")
				.withPhoneNumbers("29292929").buildContact();
		Contact contact4 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_4, TENANT_ID).withFirstName("Bum").withLastName("Barabum").withPhoneNumbers("29292929", "111333111").buildContact();
		saveContacts(contact1, contact2, contact3, contact4);
	}
	
	private void sendImportPhonebookAndVerifyResults(String requestJson, int expectedImportedContactCount) throws Exception {
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.CONTACT_IMPORT_PHONEBOOK_RESPONSE);
		JsonMessage<ContactImportPhonebookResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.CONTACT_IMPORT_PHONEBOOK_RESPONSE);
		assertEquals(expectedImportedContactCount, response.getContent().getImportedContactCount());
	}

	private Map<String, Contact> verifyContacts(int expectedCount) {
		List<Contact> contacts = contactElasticDao.listAllContacts(ANONYMOUS_USER_ID);
		assertEquals(expectedCount, contacts.size());
		return contacts.stream().collect(Collectors.toMap(c -> c.getContactId(), c -> c));
	}

	private void testContactListResponse(List<Contact> contacts, List<Buddy> buddies, List<Profile> profiles) {
		JsonMessage<ContactListResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.CONTACT_LIST_RESPONSE);
		ContactListResponse content = response.getContent();
		assertNotNull(content);
		
		List<ApiContactListResult> results = content.getItems();
		assertEquals(contacts.size(), results.size());
		
		// Results are ordered by Contact default ordering: firstName, lastName, contactId
		for (int i = 0; i < contacts.size(); i++) {
			ApiContactListResult result = results.get(i);
			assertEquals(result.getContact().getContactId(), contacts.get(i).getContactId());

			if (buddies.isEmpty()) {
				assertNull(result.getBuddy());
			} else {
				assertEquals(result.getBuddy().getUserId(), buddies.get(i).getUserId());
			}

			if (profiles.isEmpty()) {
				assertNull(result.getProfile());
			} else {
				assertEquals(result.getProfile().getUserId(), profiles.get(i).getUserId());
			}
		}
	}
	
	@Test
	public void testContactInvite() throws Exception {
		Contact contact1 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_1, TENANT_ID).withEmails(EMAIL_1).buildContact();
		Contact contact2 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_2, TENANT_ID).withEmails(EMAIL_2, EMAIL_3).buildContact();
		Contact contact3 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_3, TENANT_ID).buildContact(); // without email
		saveContacts(contact1, contact2, contact3);
		
		String requestJson = createContactInviteOrResendRequest(REQ_INVITE)
				.withContactIds(CONTACT_ID_1, CONTACT_ID_2, CONTACT_ID_3)
				.withInvitationText(INVITATION_TEXT)
				.buildRequestJson();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.CONTACT_INVITE_RESPONSE);
		// wait while ElasticSearch creates buddies
		RetriedAssert.assertWithWait(() -> assertThat(
				buddyElasticDao.getAllBuddyIds(ANONYMOUS_USER_ID, null, null, null, null, null), hasSize(2)), 5 * 1000, 500);
		
		assertInvitedContact(CONTACT_ID_1, USER_ID_1, INVITATION_TEXT);
		assertInvitedContact(CONTACT_ID_2, USER_ID_2, INVITATION_TEXT);
		assertAddedBuddies(Arrays.asList(USER_ID_1, USER_ID_2));

		// assert response (not invited contact)
		JsonMessage<ContactInviteResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.CONTACT_INVITE_RESPONSE);
		ContactInviteResponse content = response.getContent();
		assertNotNull(content);
		assertThat(content.getContactIds(), arrayContainingInAnyOrder(CONTACT_ID_3));
	}
	
	@Test
	public void testContactResendInvitationWithNewText() throws Exception {
		Contact contact1 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_1, TENANT_ID).withEmails(EMAIL_1).withSentInvitationText("To be or not to be?", APP_ID).buildContact();
		Contact contact2 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_2, TENANT_ID).withEmails(EMAIL_2).buildContact(); // without invitation text
		Contact contact3 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_3, TENANT_ID).withSentInvitationText(INVITATION_TEXT, APP_ID).buildContact(); // without email
		saveContacts(contact1, contact2, contact3);
		
		String requestJson = createContactInviteOrResendRequest(REQ_RESEND)
				.withContactIds(CONTACT_ID_1, CONTACT_ID_2, CONTACT_ID_3)
				.withInvitationText(INVITATION_TEXT)
				.buildRequestJson();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.CONTACT_RESEND_INVITATION_RESPONSE);
		// wait while ElasticSearch creates buddies
		RetriedAssert.assertWithWait(() -> assertThat(
				buddyElasticDao.getAllBuddyIds(ANONYMOUS_USER_ID, null, null, null, null, null), hasSize(1)), 5 * 1000, 500);
		
		assertInvitedContact(CONTACT_ID_1, USER_ID_1, INVITATION_TEXT);
		assertAddedBuddies(Arrays.asList(USER_ID_1));
		
		// assert response (not invited contacts)
		JsonMessage<ContactResendInvitationResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.CONTACT_RESEND_INVITATION_RESPONSE);
		ContactResendInvitationResponse content = response.getContent();
		assertNotNull(content);
		assertThat(content.getContactIds(), arrayContainingInAnyOrder(CONTACT_ID_2, CONTACT_ID_3));
	}
	
	@Test
	public void testContactResendInvitationWithOldText() throws Exception {
		Contact contact1 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_1, TENANT_ID).withEmails(EMAIL_1).withSentInvitationText(INVITATION_TEXT, APP_ID).buildContact();
		saveContacts(contact1);
		
		String requestJson = createContactInviteOrResendRequest(REQ_RESEND)
				.withContactIds(CONTACT_ID_1, CONTACT_ID_2, CONTACT_ID_3)
				.buildRequestJson();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.CONTACT_RESEND_INVITATION_RESPONSE);
		// wait while ElasticSearch creates buddy
		RetriedAssert.assertWithWait(() -> assertThat(
				buddyElasticDao.getAllBuddyIds(ANONYMOUS_USER_ID, null, null, null, null, null), hasSize(1)), 5 * 1000, 500);

		assertInvitedContact(CONTACT_ID_1, USER_ID_1, INVITATION_TEXT);
		assertAddedBuddies(Arrays.asList(USER_ID_1));
		
		JsonMessage<ContactResendInvitationResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.CONTACT_RESEND_INVITATION_RESPONSE);
		ContactResendInvitationResponse content = response.getContent();
		assertNotNull(content);
		assertThat(content.getContactIds(), emptyArray());
	}
	
	private void assertInvitedContact(String contactId, String expectedUserId, String expectedText) {
		Contact contact = getContact(contactId);
		assertEquals(expectedUserId, contact.getUserId());
		assertEquals(expectedText, contact.getSentInvitationTextAndGroup(APP_ID).getLeft());
	}
	
	@Test
	public void testContactInviteNew() throws Exception {
		saveProfiles(
			createProfile(USER_ID_1).withTenants(TENANT_ID).buildProfile()
		);
		Group group = groupDao.createGroup(ANONYMOUS_USER_ID, TENANT_ID, "testgroup", "testimg", Collections.emptyMap());
		ApiInvitee apiInvitee = new ApiInvitee("John", "Doe", "00001111", EMAIL_1);
		AppConfig appConfig = AppConfigBuilder.buildDefaultAppConfigWithAdvertisement();
		appConfig.getApplication().getConfiguration().setDownloadUrl(DOWNLOAD_URL);
        when(applicationConfigurationAerospikeDao.getAppConfiguration(anyString(), anyString())).thenReturn(appConfig);

		String requestJson = createContactInviteNewRequest().withInvitee(apiInvitee).withGroupId(group.getGroupId()).buildRequestJson();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertWithWait(() -> assertNotNull(testClientReceivedMessageCollector
				.getMessageByType(FriendManagerMessageTypes.CONTACT_INVITE_NEW_RESPONSE))
				, 5 * 1000); //wait for ElasticSearch to create indexes, response from auth etc
		// wait while ElasticSearch creates buddy
		RetriedAssert.assertWithWait(
				() -> assertThat(buddyElasticDao.getAllBuddyIds(ANONYMOUS_USER_ID, null, null, null, null, null), hasSize(1)), 5 * 1000, 500);
		

		JsonMessage<ContactInviteNewResponse> response = testClientReceivedMessageCollector.getMessageByType(FriendManagerMessageTypes.CONTACT_INVITE_NEW_RESPONSE);
		ContactInviteNewResponse content = response.getContent();
		assertThat(content.getUserId(), equalTo(USER_ID_1));
		
		List<Contact> contacts = contactElasticDao.listAllContacts(ANONYMOUS_USER_ID);
		assertInvitedNewContacts(contacts, Arrays.asList(apiInvitee));
		assertAddedBuddies(contacts.stream().map(c -> c.getUserId()).collect(Collectors.toList()));
		
		group = groupDao.getGroup(ANONYMOUS_USER_ID, TENANT_ID, group.getGroupId(), false);
		assertTrue(group.getMemberUserIds().containsKey(USER_ID_1));
		
		assertTrue(this.recievedInviteMessage.toString().contains(DOWNLOAD_URL));
		assertPlayerAddedNotifications(USER_ID_1);
	}
	
	private void assertInvitedNewContacts(List<Contact> contacts, List<ApiInvitee> invitees) {
		assertThat(contacts, hasSize(invitees.size()));
		assertNewContactsProperties(collectProperties(contacts, Contact::getFirstName), collectProperties(invitees, ApiInvitee::getFirstName));
		assertNewContactsProperties(collectProperties(contacts, Contact::getLastName), collectProperties(invitees, ApiInvitee::getLastName));
		assertNewContactsProperties(collectProperties(contacts, c -> c.getPhoneNumbers()[0]), collectProperties(invitees, ApiInvitee::getPhoneNumber));
		assertNewContactsProperties(collectProperties(contacts, c -> c.getEmails()[0]), collectProperties(invitees, ApiInvitee::getEmail));
	}

	private void assertNewContactsProperties(String[] contactsProperties, String[] inviteesProperties) {
		assertThat(contactsProperties, arrayContainingInAnyOrder(inviteesProperties));
	}
	
	private <T> String[] collectProperties(List<T> list, Function<T, String> getter) {
		return list.stream().map(l -> getter.apply(l)).toArray(size -> new String[size]);
	}

	private void assertAddedBuddies(List<String> userIds) {
		List<Buddy> buddies = buddyDao.getBuddies(ANONYMOUS_USER_ID, userIds);
		assertThat(buddies, hasSize(userIds.size()));
		buddies.forEach(b -> {
			assertThat(b.getTenantIds(), arrayContainingInAnyOrder(TENANT_ID));
			assertThat(b.getRelationTypes(), contains(BuddyRelationType.BUDDY));
		});
	}
	
	@Test
	public void testRemoveInvitation() throws Exception {
		Contact contact1 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_1, TENANT_ID).withSentInvitationText(INVITATION_TEXT, APP_ID).buildContact();
		Contact contact2 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_2, TENANT_ID).withSentInvitationText(INVITATION_TEXT, APP_ID).buildContact();
		Contact contact3 = createContact(ANONYMOUS_USER_ID, CONTACT_ID_3, TENANT_ID).buildContact();
		saveContacts(contact1, contact2, contact3);
		
		String requestJson = jsonLoader.getPlainTextJsonFromResources("contactRemoveInvitation.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replaceFirst("\"<<contactIds>>\"", jsonUtil.toJson(Arrays.asList(CONTACT_ID_1, CONTACT_ID_3)));
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.CONTACT_REMOVE_INVITATION_RESPONSE);
		
		assertRemovedInvitation(CONTACT_ID_1, null);
		assertRemovedInvitation(CONTACT_ID_2, INVITATION_TEXT);
		assertRemovedInvitation(CONTACT_ID_3, null);
	}

	@Test
	public void testMoveContacts() throws Exception {
		// Prepare profiles
		saveProfiles(
			createProfile(USER_ID_1).withTenants(TENANT_ID).buildProfile(),
			createProfile(USER_ID_2).withTenants(TENANT_ID).buildProfile()
		);
		
		// Prepare contacts
		Contact contact1 = createContact(USER_ID_1, CONTACT_ID_1, TENANT_ID).buildContact();
		Contact contact2 = createContact(USER_ID_2, CONTACT_ID_2, TENANT_ID).withUserId(USER_ID_1).buildContact(); // will have reference to user1
		saveContacts(contact1, contact2);
		
		// Assure correct contact lists
		assertEquals(1, contactElasticDao.getContactList(USER_ID_1, null, false, TENANT_ID, null, null, null, null, null, 10, 0).getSize());
		assertEquals(1, contactElasticDao.getContactList(USER_ID_2, null, false, TENANT_ID, null, null, null, null, null, 10, 0).getSize());
		assertNotNull(contactDao.getContact(USER_ID_1, contact1.getContactId()));
		assertNotNull(contactDao.getContact(USER_ID_2, contact2.getContactId()));
		
		// Act
		String moveContactsRequest = getPlainTextJsonFromResources("moveRequest.json")
				.replace("<<entity>>", "Contacts")
				.replace("<<sourceUserId>>", USER_ID_1)
				.replace("<<targetUserId>>", USER_ID_2);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), moveContactsRequest);
		
		// Verify message
		assertReceivedMessagesWithWait(FriendManagerMessageTypes.MOVE_CONTACTS_RESPONSE);
		
		// Verify contacts
		assertEquals(0, contactElasticDao.getContactList(USER_ID_1, null, false, TENANT_ID, null, null, null, null, null, 10, 0).getSize());
		assertEquals(2, contactElasticDao.getContactList(USER_ID_2, null, false, TENANT_ID, null, null, null, null, null, 10, 0).getSize());
		assertNull(contactDao.getContact(USER_ID_1, contact1.getContactId()));
		assertNotNull(contactDao.getContact(USER_ID_2, contact1.getContactId()));
		
		contact2 = contactDao.getContact(USER_ID_2, contact2.getContactId());
		assertNotNull(contact2);
		assertEquals(USER_ID_2, contact2.getUserId());
	}
	
	private void assertRemovedInvitation(String contactId, String expectedText) {
		Contact contact = getContact(contactId);
		assertEquals("Invitation text for contactId [" + contactId + "]", expectedText, contact.getSentInvitationTextAndGroup(APP_ID).getLeft());
	}

	@Override
	protected JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> message) throws Exception {
		JsonMessageContent response;
		if (AuthMessageTypes.INVITE_USER_BY_EMAIL == message.getType(AuthMessageTypes.class)) {
			response = onRegisterEmail((InviteUserByEmailRequest) message.getContent());
			this.recievedInviteMessage = message;
		} else if (message.getType(AuthMessageTypes.class) != null) {
			response = null;
		} else if (UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE == message.getType(UserMessageMessageTypes.class)) { 
			response = null;
		} else {
			LOGGER.error("Mocked Service received unexpected message [{}]", message);
			throw new UnexpectedTestException("Unexpected message");
		}
		return response;
	}

	private JsonMessageContent onRegisterEmail(InviteUserByEmailRequest content) throws IOException {
		String userId;
		if (ArrayUtils.contains(content.getEmails(), EMAIL_1)) {
			userId = USER_ID_1;
		} else if (ArrayUtils.contains(content.getEmails(), EMAIL_2)) {
			userId = USER_ID_2;
		} else {
			LOGGER.error("onRegisterEmail received unexpected content [{}]", content.toString());
			throw new F4MFatalErrorException("Unexpected InviteUserByEmailRequest");
		}
		return new InviteUserByEmailResponse(userId, "randomTokenJustForValidation");
	}

	private void saveContacts(Contact... contacts) {
		Arrays.stream(contacts).forEach(c -> contactDao.createOrUpdateContact(c));
	}
	
	private Contact getContact(String contactId) {
		return contactDao.getContact(ANONYMOUS_USER_ID, contactId);
	}
	
	private void saveBuddies(String userId, Buddy... buddies) {
		Arrays.stream(buddies).forEach(b -> buddyDao.createOrUpdateBuddy(userId, TENANT_ID, b));
	}
	
	private void saveProfiles(Profile... profiles) {
		Arrays.stream(profiles).forEach(profile -> {
			String key = profilePrimaryKeyUtil.createPrimaryKey(profile.getUserId());
			String set = config.getProperty(AerospikeConfigImpl.AEROSPIKE_PROFILE_SET);
			aerospikeDao.createJson(set, key, CommonProfileAerospikeDao.BLOB_BIN_NAME, profile.getAsString());
			final List<String> emails = profile.getProfileEmails();
			if (emails != null && !emails.isEmpty()) {
				emails.forEach(e -> createProfileKey(set, profile.getUserId(), ProfileIdentifierType.EMAIL, e));
			}

			final List<String> phones = profile.getProfilePhones();
			if (phones != null && !phones.isEmpty()) {
				phones.forEach(p -> createProfileKey(set, profile.getUserId(), ProfileIdentifierType.PHONE, p));
			}

			profileDao.createOrUpdate(profile);
		});
	}

	private void createProfileKey(String set, String userId, ProfileIdentifierType type, String identifier) {
		final String recordKey = profilePrimaryKeyUtil.createPrimaryKey(type, identifier);
		final String profileKey = profilePrimaryKeyUtil.createPrimaryKey(userId);
		aerospikeDao.createString(set, recordKey, CommonProfileAerospikeDao.PROFILE_ID_BIN_NAME, profileKey);
	}

}
