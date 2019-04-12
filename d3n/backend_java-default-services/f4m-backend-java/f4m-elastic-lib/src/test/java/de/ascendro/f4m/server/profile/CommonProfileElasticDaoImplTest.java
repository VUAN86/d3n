package de.ascendro.f4m.server.profile;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.elastic.ElasticsearchTestNode;
import de.ascendro.f4m.server.util.ElasticUtil;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.util.register.ServiceMonitoringRegister;

public class CommonProfileElasticDaoImplTest {

	private static final int ELASTIC_PORT = 9201;

	private static final String USER_ID_1 = "uid1";
	private static final String USER_ID_2 = "uid2";
	private static final String USER_ID_3 = "uid3";
	private static final String USER_ID_4 = "uid4";
	private static final String USER_ID_5 = "uid5";
	private static final String USER_ID_6 = "uid6";
	private static final String USER_ID_7 = "uid7";

	private static final String APP_ID_1 = "1";
	private static final String APP_ID_2 = "2";

	@Rule
	public ElasticsearchTestNode elastic = new ElasticsearchTestNode(ELASTIC_PORT);

	private ElasticClient client;
	
	private CommonProfileElasticDao profileDao;

	@Before
	public void setUp() throws Exception {
		ElasticConfigImpl config = new ElasticConfigImpl();
		config.setProperty(ElasticConfigImpl.ELASTIC_SERVER_HOSTS, "http://localhost:" + ELASTIC_PORT);
		config.setProperty(ElasticConfigImpl.ELASTIC_HEADER_REFRESH, ElasticConfigImpl.ELASTIC_HEADER_VALUE_WAIT_FOR);
		ElasticUtil elasticUtil = new ElasticUtil();
		client = new ElasticClient(config, elasticUtil, new ServiceMonitoringRegister());
		profileDao = new CommonProfileElasticDaoImpl(client, config, elasticUtil);
	}
	
	@After
	public void shutdown() {
		client.close();
	}

	@Test
	public void testIndex() {
		prepareData();
		
		// Test paging
		assertContents(APP_ID_2, null, null, null, 10, 0, USER_ID_1, USER_ID_3, USER_ID_6, USER_ID_7, USER_ID_2, USER_ID_4);
		assertContents(APP_ID_2, null, null, null, 2, 1, USER_ID_3, USER_ID_6);
		assertContents(APP_ID_2, null, null, null, 10, 5, USER_ID_4);

		// Test other app ID
		assertContents(APP_ID_1, null, null, null, 10, 0, USER_ID_1);
		assertContents(null, null, null, null, 10, 0, USER_ID_1, USER_ID_5, USER_ID_3, USER_ID_6, USER_ID_7, USER_ID_2, USER_ID_4);
		
		// Test search term
		assertContents(APP_ID_2, "con kol", null, null, 10, 0, USER_ID_1);
		assertContents(APP_ID_2, "con ela kole mar", null, null, 10, 0, USER_ID_1);
		assertContents(APP_ID_2, "molos", null, null, 10, 0, USER_ID_3, USER_ID_6, USER_ID_7);
		assertContents(APP_ID_2, "con ela kole muchacho", null, null, 10, 0, USER_ID_1); // 2 matches are enough
		assertContents(APP_ID_2, "con muchacho", null, null, 10, 0); // 1 match is not enough, more than 1 word specified
		assertContents(APP_ID_2, "nIk", null, null, 10, 0, USER_ID_1, USER_ID_3, USER_ID_6, USER_ID_7, USER_ID_2);
		
		// Test included user ids
		assertContents(APP_ID_2, null, new String[] { USER_ID_3, USER_ID_1 }, null, 10, 0, USER_ID_1, USER_ID_3);
		assertContents(APP_ID_2, null, new String[] { USER_ID_2 }, null, 10, 0, USER_ID_2);
		assertContents(APP_ID_2, null, new String[] { USER_ID_5 }, null, 10, 0);
		
		// Test excluded user ids
		assertContents(APP_ID_2, null, null, new String[] { USER_ID_3, USER_ID_1 }, 10, 0, USER_ID_6, USER_ID_7, USER_ID_2, USER_ID_4);
		assertContents(APP_ID_2, null, null, new String[] { USER_ID_2, USER_ID_6, USER_ID_7 }, 10, 0, USER_ID_1, USER_ID_3, USER_ID_4);
		assertContents(APP_ID_2, null, null, new String[] { USER_ID_5 }, 10, 0, USER_ID_1, USER_ID_3, USER_ID_6, USER_ID_7, USER_ID_2, USER_ID_4);
		
		// Test all
		assertContents(APP_ID_2, "ozzy", new String[] { USER_ID_3 }, new String[] { USER_ID_2 }, 10, 0, USER_ID_3);
		assertContents(APP_ID_2, "ozzenshtein", new String[] { USER_ID_3 }, new String[] { USER_ID_2 }, 10, 0);
		
		// Test sorting
		assertContents(APP_ID_2, null, null, null, PlayerListOrderType.NONE, 10, 0, USER_ID_1, USER_ID_3, USER_ID_6, USER_ID_7, USER_ID_2, USER_ID_4);
		assertContents(APP_ID_2, null, null, null, PlayerListOrderType.HANDICAP, 10, 0, USER_ID_4, USER_ID_1, USER_ID_2, USER_ID_3, USER_ID_6, USER_ID_7);
		
	}

	@Test
	public void testRank() {
		prepareData();
		
		Pair<Long, Long> result = profileDao.getRank(APP_ID_2, "uid8", 0.0);
		assertEquals(7l, result.getLeft().longValue());
		assertEquals(7l, result.getRight().longValue()); // The one searching is not among results, so it will increase the total size
		
		result = profileDao.getRank(APP_ID_1, "uid0", 0.0);
		assertEquals(2l, result.getLeft().longValue());
		assertEquals(2l, result.getRight().longValue()); // The one searching is not among results, so it will increase the total size

		result = profileDao.getRank(APP_ID_2, USER_ID_2, 2.0);
		assertEquals(3l, result.getLeft().longValue());
		assertEquals(6l, result.getRight().longValue());

		result = profileDao.getRank(APP_ID_2, USER_ID_4, 1.1);
		assertEquals(3l, result.getLeft().longValue());
		assertEquals(6l, result.getRight().longValue());

		result = profileDao.getRank(APP_ID_2, "uid9", 1.1);
		assertEquals(4l, result.getLeft().longValue());
		assertEquals(6l, result.getRight().longValue());

		result = profileDao.getRank(APP_ID_2, "uid0", 1.1);
		assertEquals(3l, result.getLeft().longValue());
		assertEquals(6l, result.getRight().longValue());
		
		result = profileDao.getRank(APP_ID_2, USER_ID_7, 0.0);
		assertEquals(6l, result.getLeft().longValue());
		assertEquals(6l, result.getRight().longValue());
		
		result = profileDao.getRank(APP_ID_2, USER_ID_6, null); // All will be greater, since asking for null handicap
		assertEquals(6l, result.getLeft().longValue());
		assertEquals(6l, result.getRight().longValue());

		result = profileDao.getRank(APP_ID_2, USER_ID_6, 0.0);
		assertEquals(5l, result.getLeft().longValue());
		assertEquals(6l, result.getRight().longValue());
	}
	
	private void assertContents(String appId, String searchTerm, String[] includedUserIds, String[] excludedUserIds, int limit, long offset, String... userIds) {
		assertContents(appId, searchTerm, includedUserIds, excludedUserIds, PlayerListOrderType.NONE, limit, offset, userIds);
	}
	
	private void assertContents(String appId, String searchTerm, String[] includedUserIds, String[] excludedUserIds, 
			PlayerListOrderType orderType, int limit, long offset, String... userIds) {
		assertThat(profileDao.searchProfiles(appId, searchTerm, includedUserIds, excludedUserIds, orderType, limit, offset).getItems()
				.stream().map(p -> p.getUserId()).collect(Collectors.toList()), 
				userIds == null || userIds.length == 0 ? hasSize(0) : contains(userIds));
	}

	private void prepareData() {
		prepareProfile(USER_ID_1, true, "Con ela", "kole mar", "Nikki", 2.5, APP_ID_1, APP_ID_2); // 2 
		prepareProfile(USER_ID_2, true, "Peter", "None meee", "Nikki", 1.1, APP_ID_2);            // 3
		prepareProfile(USER_ID_3, true, "Ozzy1", "Molos", "Nikki", null, APP_ID_2);               // 4
		prepareProfile(USER_ID_4, false, "Anatoly", "Hole", "Superman", 53.1, APP_ID_2);          // 1
		prepareProfile(USER_ID_5, false, "Noman", "Noman", "Noman", 0.5); 
		prepareProfile(USER_ID_6, true, "Ozzy2", "Molos", "Nikki", null, APP_ID_2);               // 5
		prepareProfile(USER_ID_7, true, "Ozzy3", "Molos", "Nikki", null, APP_ID_2);               // 6
		prepareProfile("xxx", false, "", "", "", 0.5);
	}

	private void prepareProfile(String userId, boolean showFullName, String firstName, String lastName, String nickname, Double handicap, String... appIds) {
		Profile profile = new Profile();
		profile.setUserId(userId);
		if (handicap != null) {
			profile.setHandicap(handicap);
		}
		profile.setShowFullName(showFullName);
		
		ProfileUser person = new ProfileUser();
		person.setFirstName(firstName);
		person.setLastName(lastName);
		person.setNickname(nickname);
		profile.setPersonWrapper(person);

		ProfileAddress address = new ProfileAddress();
		address.setCity("xxx");
		address.setCountry("xxx");
		profile.setAddress(address);
		
		Arrays.stream(appIds).forEach(appId -> profile.addApplication(appId));
		
		profileDao.createOrUpdate(profile);
	}
	

}
