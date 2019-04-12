package de.ascendro.f4m.server.winning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.server.winning.util.UserWinningPrimaryKeyUtil;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.json.model.OrderBy;
import de.ascendro.f4m.service.json.model.OrderBy.Direction;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.winning.WinningMessageTypes;
import de.ascendro.f4m.service.winning.model.UserWinning;
import de.ascendro.f4m.service.winning.model.UserWinningType;

public class CommonUserWinningAerospikeDaoImplTest extends RealAerospikeTestBase {

	private static final String WINNING_SET = WinningMessageTypes.SERVICE_NAME;
	private static final String BLOB_BIN_NAME = CommonUserWinningAerospikeDao.BLOB_BIN_NAME;
	private static final String WINNINGS_BIN_NAME = CommonUserWinningAerospikeDao.WINNINGS_BIN_NAME;

	private static final String USER_ID = "user_id_1";
	private static final String APP_ID = "app_id_1";
	private static final String USER_WINNING_ID = "user_winning_id_1";

	private final UserWinningPrimaryKeyUtil userWinningPrimaryKeyUtil = new UserWinningPrimaryKeyUtil(config);
	private final JsonUtil jsonUtil = new JsonUtil();

	private CommonUserWinningAerospikeDao commonWinningAerospikeDao;

	@Override
	@Before
	public void setUp() {
		super.setUp();
		clearSet(WINNING_SET);
	}

	@Override
	@After
	public void tearDown() {
		try {
			clearSet(WINNING_SET);
		} finally {
			super.tearDown();
		}
	}

	protected void clearSet(String set) {
		final String namespace = config.getProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE);
		if (aerospikeClientProvider != null && aerospikeClientProvider instanceof AerospikeClientProvider) {
			super.clearSet(namespace, set);
		}
	}

	@Override
	protected void setUpAerospike() {
		commonWinningAerospikeDao = new CommonUserWinningAerospikeDaoImpl(config, userWinningPrimaryKeyUtil, jsonUtil,
				aerospikeClientProvider);
	}

	@Test
	public void testSaveUserWinning() {
		// Prepare
		final UserWinning userWinning = prepareUserWinning();

		// Test
		commonWinningAerospikeDao.saveUserWinning(APP_ID, USER_ID, userWinning);

		// Validate user winning
		final String userWinningKey = userWinningPrimaryKeyUtil.createPrimaryKey(APP_ID, userWinning.getUserWinningId());
		final String userWinningString = commonWinningAerospikeDao.readJson(WINNING_SET, userWinningKey, BLOB_BIN_NAME);
		assertNotNull(userWinningString);
		assertEquals(userWinning.getAsString(), userWinningString);

		// Validate user winning map
		final String userWinningsKey = userWinningPrimaryKeyUtil.createUserWinningsKey(APP_ID, USER_ID);
		final Map<String, String> userWinningsMap = commonWinningAerospikeDao.getAllMap(WINNING_SET, userWinningsKey,
				WINNINGS_BIN_NAME);
		assertNotNull(userWinningsMap);
		assertEquals(1, userWinningsMap.size());
		assertTrue(userWinningsMap.containsKey(userWinning.getUserWinningId()));
	}

	@Test
	public void testGetUserWinning() {
		// Prepare
		final UserWinning userWinning = prepareUserWinning();
		userWinning.setUserWinningId(USER_WINNING_ID);
		
		final String userWinningKey = userWinningPrimaryKeyUtil.createPrimaryKey(APP_ID, USER_WINNING_ID);
		commonWinningAerospikeDao.createJson(WINNING_SET, userWinningKey, BLOB_BIN_NAME, userWinning.getAsString());

		// Test
		final UserWinning userWinningFromDB = commonWinningAerospikeDao.getUserWinning(APP_ID, USER_WINNING_ID);

		// Validate
		assertNotNull(userWinningFromDB);
		assertEquals(userWinning.getAsString(), userWinningFromDB.getAsString());
	}
	
	@Test
	public void testGetUserWinningsWithOrderBy() {
		// Prepare user winnings
		final List<UserWinning> testUserWinnings = prepareUserWinnings();

		// Prepare ordering
		final OrderBy orderByType = new OrderBy(UserWinning.PROPERTY_TYPE, Direction.asc);
		final OrderBy orderByDate = new OrderBy(UserWinning.PROPERTY_OBTAIN_DATE, Direction.desc);
		final List<OrderBy> orderBy = Arrays.asList(orderByType, orderByDate);

		// Test
		final List<JsonObject> userWinnings = commonWinningAerospikeDao.getUserWinnings(APP_ID, USER_ID, testUserWinnings.size(), 0, orderBy);

		// Validate
		assertEquals(testUserWinnings.get(1).getAsString(), new UserWinning(userWinnings.get(0)).getAsString());
		assertEquals(testUserWinnings.get(0).getAsString(), new UserWinning(userWinnings.get(1)).getAsString());
		assertEquals(testUserWinnings.get(2).getAsString(), new UserWinning(userWinnings.get(2)).getAsString());
	}
	
	@Test
	public void testGetUserWinningsWithOffset() {
		final int offset = 1;
		
		// Prepare user winnings
		final List<UserWinning> testUserWinnings = prepareUserWinnings();

		// Test
		final List<JsonObject> userWinnings = commonWinningAerospikeDao.getUserWinnings(APP_ID, USER_ID, testUserWinnings.size(), offset, null);

		// Validate
		assertEquals(testUserWinnings.size() - offset, userWinnings.size());
	}
	
	@Test
	public void testGetUserWinningsWithLimit() {
		final int limit = 2;

		// Prepare user winnings
		prepareUserWinnings();

		// Test
		final List<JsonObject> userWinnings = commonWinningAerospikeDao.getUserWinnings(APP_ID, USER_ID, limit, 0, null);

		// Validate
		assertEquals(limit, userWinnings.size());
	}

	private UserWinning prepareUserWinning() {
		return new UserWinning("title", UserWinningType.TOMBOLA, BigDecimal.valueOf(2.2), Currency.MONEY, "details", "imageId", "gameInstance1");
	}
	
	private List<UserWinning> prepareUserWinnings() {
		final UserWinning userWinning1 = prepareUserWinning();
		userWinning1.setObtainDate(ZonedDateTime.of(2000, 1, 5, 12, 0, 0, 0, ZoneOffset.UTC));
		commonWinningAerospikeDao.saveUserWinning(APP_ID, USER_ID, userWinning1);

		final UserWinning userWinning2 = prepareUserWinning();
		userWinning2.setType(UserWinningType.DUEL);
		userWinning2.setObtainDate(ZonedDateTime.of(2000, 1, 3, 12, 0, 0, 0, ZoneOffset.UTC));
		commonWinningAerospikeDao.saveUserWinning(APP_ID, USER_ID, userWinning2);

		final UserWinning userWinning3 = prepareUserWinning();
		userWinning3.setObtainDate(ZonedDateTime.of(2000, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC));
		commonWinningAerospikeDao.saveUserWinning(APP_ID, USER_ID, userWinning3);
		
		return Arrays.asList(userWinning1, userWinning2, userWinning3);
	}

}
