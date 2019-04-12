package de.ascendro.f4m.service.friend.dao;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.elastic.ElasticsearchTestNode;
import de.ascendro.f4m.server.profile.CommonProfileElasticDao;
import de.ascendro.f4m.server.profile.CommonProfileElasticDaoImpl;
import de.ascendro.f4m.server.util.ElasticUtil;
import de.ascendro.f4m.service.friend.config.FriendManagerConfig;
import de.ascendro.f4m.service.friend.model.Contact;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.util.register.ServiceMonitoringRegister;

public class ContactElasticDaoImplTest {

	private static final int ELASTIC_PORT = 9204;

	private static final String CONTACT_ID_1 = "1qwefoijo123-2345";
	private static final String CONTACT_ID_2 = "2qqwvbq52-2345";
	private static final String CONTACT_ID_3 = "3243g90jok-2345";
	private static final String CONTACT_ID_4 = "4qwefoijo12323i49ghionkl";

	private static final String USER_ID_1 = "uid1";
	private static final String USER_ID_2 = "uid2";
	
	private static final String APP_ID_1 = "1";
	private static final String APP_ID_2 = "2";

	private static final String TENANT_ID = "tenant1";
	
	@Rule
	public ElasticsearchTestNode elastic = new ElasticsearchTestNode(ELASTIC_PORT);
	
	private ElasticClient client;
	
	private ContactElasticDao contactDao;
	private CommonProfileElasticDao profileDao;
	
	@Before
	public void setUp() throws Exception {
		FriendManagerConfig config = new FriendManagerConfig();
		config.setProperty(ElasticConfigImpl.ELASTIC_SERVER_HOSTS, "http://localhost:" + ELASTIC_PORT);
		config.setProperty(ElasticConfigImpl.ELASTIC_HEADER_REFRESH, ElasticConfigImpl.ELASTIC_HEADER_VALUE_WAIT_FOR);
		ElasticUtil elasticUtil = new ElasticUtil();
		client = new ElasticClient(config, elasticUtil, new ServiceMonitoringRegister());
		profileDao = new CommonProfileElasticDaoImpl(client, config, elasticUtil);
		contactDao = new ContactElasticDaoImpl(client, config, elasticUtil);
	}
	
	@After
	public void shutdown() {
		client.close();
	}
	
	@Test
	public void testIndex() {
		prepareData();
		
		// Test paging
		assertContents(USER_ID_1, APP_ID_1, false, null, null, null, 10, 0, CONTACT_ID_4, CONTACT_ID_1, CONTACT_ID_3, CONTACT_ID_2);
		assertContents(USER_ID_1, APP_ID_1, false, null, null, null, 2, 1, CONTACT_ID_1, CONTACT_ID_3);
		assertContents(USER_ID_1, APP_ID_1, false, null, null, null, 10, 3, CONTACT_ID_2);

		// Test wrong owner
		assertContents(USER_ID_2, APP_ID_1, false, null, null, null, 10, 0);

		// Test app ID
		assertContents(USER_ID_1, APP_ID_1, true, null, null, null, 10, 0, CONTACT_ID_4);
		assertContents(USER_ID_1, APP_ID_2, true, null, null, null, 10, 0, CONTACT_ID_3);
		
		// Test search term
		assertContents(USER_ID_1, APP_ID_1, false, "con kol", null, null, 10, 0, CONTACT_ID_1);
		assertContents(USER_ID_1, APP_ID_1, false, "con ela kole mar", null, null, 10, 0, CONTACT_ID_1);
		assertContents(USER_ID_1, APP_ID_1, false, "molos", null, null, 10, 0, CONTACT_ID_3);
		assertContents(USER_ID_1, APP_ID_1, false, "con ela kole muchacho", null, null, 10, 0, CONTACT_ID_1); // 2 matches are enough
		assertContents(USER_ID_1, APP_ID_1, false, "con muchacho", null, null, 10, 0); // 1 match is not enough, more than 1 word specified
		
		// Test has profile
		assertContents(USER_ID_1, APP_ID_1, false, null, null, true, 10, 0, CONTACT_ID_4, CONTACT_ID_3);
		assertContents(USER_ID_1, APP_ID_1, false, null, null, false, 10, 0, CONTACT_ID_1, CONTACT_ID_2);
		
		// Test has invitation
		assertContents(USER_ID_1, APP_ID_1, false, null, true, null, 10, 0, CONTACT_ID_1);
		assertContents(USER_ID_1, APP_ID_2, false, null, true, null, 10, 0, CONTACT_ID_1, CONTACT_ID_3);
		assertContents(USER_ID_1, APP_ID_1, false, null, false, null, 10, 0, CONTACT_ID_4, CONTACT_ID_3, CONTACT_ID_2);
		assertContents(USER_ID_1, APP_ID_2, false, null, false, null, 10, 0, CONTACT_ID_4, CONTACT_ID_2);
		
		// Test all
		assertContents(USER_ID_1, APP_ID_2, false, "ozz", true, true, 10, 0, CONTACT_ID_3);
		assertContents(USER_ID_1, APP_ID_1, false, "ozzenshtein", true, true, 10, 0);
		
	}

	@Test
	public void testListAllContacts() {
		prepareData();
		
		// Test batching
		client.batchSize = 1;
		assertContents(contactDao.listAllContacts(USER_ID_1), CONTACT_ID_1, CONTACT_ID_2, CONTACT_ID_3, CONTACT_ID_4);
		client.batchSize = 2;
		assertContents(contactDao.listAllContacts(USER_ID_1), CONTACT_ID_1, CONTACT_ID_2, CONTACT_ID_3, CONTACT_ID_4);
		client.batchSize = 4;
		assertContents(contactDao.listAllContacts(USER_ID_1), CONTACT_ID_1, CONTACT_ID_2, CONTACT_ID_3, CONTACT_ID_4);
	}

	private void assertContents(String ownerId, String appId, boolean hasApp, String searchTerm, Boolean hasInvitation, Boolean hasProfile, 
			int limit, long offset, String... contactIds) {
		assertContents(contactDao.getContactList(ownerId, appId, hasApp, TENANT_ID, searchTerm, hasInvitation, hasProfile, null, null, limit, offset).getItems(), contactIds);
	}
	
	private void assertContents(List<Contact> result, String... contactIds) {
		assertThat(result.stream().map(c -> c.getContactId()).collect(Collectors.toList()), 
				contactIds == null || contactIds.length == 0 ? hasSize(0) : contains(contactIds));
	}
	
	private void prepareData() {
		prepareContact(USER_ID_1, CONTACT_ID_1, "Con ela", "kole mar", "Please join", null, null, APP_ID_1, APP_ID_2);
		prepareContact(USER_ID_1, CONTACT_ID_2, "Peter", "None meee", null, null, null);
		prepareContact(USER_ID_1, CONTACT_ID_3, "Ozzy", "Molos", "Cmon", null, USER_ID_1, APP_ID_2);
		prepareContact(USER_ID_1, CONTACT_ID_4, "Anatoly", "Hole", null, null, USER_ID_2, APP_ID_1);
	}

	private void prepareContact(String ownerId, String id, String firstName, String lastName, String invitationText, 
			String groupId, String userId, String... appIds) {
		Contact contact = new Contact();
		contact.setOwnerId(ownerId);
		contact.setContactId(id);
		contact.setEmails("some@email.lv", "oth@oo.com");
		contact.setFirstName(firstName);
		contact.setLastName(lastName);
		contact.setPhoneNumbers("291589125");
		Arrays.stream(appIds).forEach(appId -> contact.setSentInvitationTextAndGroup(appId, invitationText, groupId));
		contact.setUserId(userId);
		contact.setTenantIds(TENANT_ID);
		contactDao.createOrUpdate(contact);
		
		if (userId != null && ArrayUtils.isNotEmpty(appIds)) {
			Profile profile = new Profile();
			profile.setUserId(userId);
			Arrays.stream(appIds).forEach(appId -> profile.addApplication(appId));
			profileDao.createOrUpdate(profile);
		}
	}
	
}
