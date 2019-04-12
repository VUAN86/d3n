package de.ascendro.f4m.server.multiplayer.dao;

import static de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder.create;
import static de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder.createDuel;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.APP_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.REGISTERED_USER_ID;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.elastic.ElasticsearchTestNode;
import de.ascendro.f4m.server.util.ElasticUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder;
import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.game.selection.model.multiplayer.PublicGameFilter;
import de.ascendro.f4m.service.game.selection.model.multiplayer.PublicGameFilterBuilder;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.random.RandomUtilImpl;
import de.ascendro.f4m.service.util.register.ServiceMonitoringRegister;

public class PublicGameElasticDaoImplTest {

	private static final int ELASTIC_PORT = 9202;
	
	private static final String MGI_ID_1 = "mgi_id_1";
	private static final String MGI_ID_2 = "mgi_id_2";
	private static final String MGI_ID_3 = "mgi_id_3";
	private static final String MGI_ID_4 = "mgi_id_4";
	private static final String MGI_ID_5_ULT = "mgi_id_5_ult"; //user live tournament
	private static final String USER_ID_1 = "user_id_1";
	private static final String APP_ID_1 = "app_id_1";

	@Rule
	public ElasticsearchTestNode elastic = new ElasticsearchTestNode(ELASTIC_PORT);
	private ElasticClient client;
	private PublicGameElasticDaoImpl publicGameDao;
	private GameConfigImpl config;

	@Before
	public void setUp() throws Exception {
		config = new GameConfigImpl();
		config.setProperty(ElasticConfigImpl.ELASTIC_SERVER_HOSTS, "http://localhost:" + ELASTIC_PORT);
		config.setProperty(ElasticConfigImpl.ELASTIC_HEADER_REFRESH, ElasticConfigImpl.ELASTIC_HEADER_VALUE_WAIT_FOR);
		
		client = new ElasticClient(config, new ElasticUtil(), new ServiceMonitoringRegister());
		publicGameDao = new PublicGameElasticDaoImpl(client, config, new JsonUtil(), new RandomUtilImpl());
	}
	
	@After
	public void shutdown() {
		client.close();
	}
	
	@Test
	public void testGetPublicGame() {
		CustomGameConfig customGameConfig = new CustomGameConfig("any_game_id");
		customGameConfig.setId(MGI_ID_1);
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(customGameConfig, ANONYMOUS_USER_ID));
		
		Invitation publicGame = publicGameDao.getPublicGame(APP_ID, MGI_ID_1);
		assertThat(publicGame, notNullValue());
		assertThat(publicGame.getMultiplayerGameInstanceId(), equalTo(MGI_ID_1));
		assertThat(publicGame.getInviter().getUserId(), equalTo(ANONYMOUS_USER_ID));
	}

	@Test
	public void testSearchPublicGames() {
		prepareData();
		PublicGameFilterBuilder filterBuilder;
		
		filterBuilder = PublicGameFilterBuilder.create(APP_ID);
		assertPublicGames(filterBuilder.build(), MGI_ID_1, MGI_ID_2, MGI_ID_3, MGI_ID_4, MGI_ID_5_ULT);
		
		filterBuilder = PublicGameFilterBuilder.create(APP_ID_1);
		assertPublicGames(filterBuilder.build(), MGI_ID_2, MGI_ID_3);
		
		filterBuilder = PublicGameFilterBuilder.create("another_app_id");
		assertPublicGames(filterBuilder.build());
		
		filterBuilder = PublicGameFilterBuilder.create(APP_ID).withGameType(GameType.TOURNAMENT);
		assertPublicGames(filterBuilder.build(), MGI_ID_3, MGI_ID_4);
		
		filterBuilder = PublicGameFilterBuilder.create(APP_ID).withSearchKeywords("first");
		assertPublicGames(filterBuilder.build(), MGI_ID_1, MGI_ID_3);
		
		filterBuilder = PublicGameFilterBuilder.create(APP_ID).withSearchKeywords("tournament normal");
		assertPublicGames(filterBuilder.build(), MGI_ID_3, MGI_ID_4);

		filterBuilder = PublicGameFilterBuilder.create(APP_ID).withPlayingRegions("de", "fr", "en");
		assertPublicGames(filterBuilder.build(), MGI_ID_1);

		filterBuilder = PublicGameFilterBuilder.create(APP_ID).withPlayingRegions("de");
		assertPublicGames(filterBuilder.build(), MGI_ID_1, MGI_ID_3);

		filterBuilder = PublicGameFilterBuilder.create(APP_ID).withPoolIds("history", "sport");
		assertPublicGames(filterBuilder.build(), MGI_ID_1, MGI_ID_3);

		filterBuilder = PublicGameFilterBuilder.create(APP_ID).withNumberOfQuestions(7);
		assertPublicGames(filterBuilder.build(), MGI_ID_3);

		filterBuilder = PublicGameFilterBuilder.create(APP_ID).withEntryFee(new BigDecimal("7.00"), new BigDecimal("8.00"));
		assertPublicGames(filterBuilder.build(), MGI_ID_2);

		filterBuilder = PublicGameFilterBuilder.create(APP_ID).withEntryFee(new BigDecimal("0.00"), new BigDecimal("0"));
		assertPublicGames(filterBuilder.build(), MGI_ID_3, MGI_ID_4, MGI_ID_5_ULT);

		filterBuilder = PublicGameFilterBuilder.create(APP_ID).withEntryFeeCurrency(Currency.MONEY);
		assertPublicGames(filterBuilder.build(), MGI_ID_1, MGI_ID_2);

		filterBuilder = PublicGameFilterBuilder.create(APP_ID);
		assertPublicGames(filterBuilder.build(), MGI_ID_1, MGI_ID_2, MGI_ID_3, MGI_ID_4, MGI_ID_5_ULT);

		filterBuilder = PublicGameFilterBuilder.create(APP_ID).withPlayingRegions("lv");
		assertPublicGames(filterBuilder.build());

		deleteData();
	}
	
	@Test
	public void testGetNumberOfPublicGames() {
		long numberOfGames = 4;
		for (int i = 0; i < numberOfGames; i++) {
			CustomGameConfig duel = createDuel(ANONYMOUS_USER_ID).build(String.valueOf(i));
			publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(duel, ANONYMOUS_USER_ID));
		}
		
		assertThat(publicGameDao.getNumberOfPublicGames(new PublicGameFilter(APP_ID)), equalTo(numberOfGames));
	}
	
	@Test
	public void testGetNextPublicGame() {
		ZonedDateTime now = DateTimeUtil.getCurrentDateTime();
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(createDuel(ANONYMOUS_USER_ID).withExpiryDateTime(now.plusSeconds(30)).build(MGI_ID_1), ANONYMOUS_USER_ID));
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(createDuel(ANONYMOUS_USER_ID).withExpiryDateTime(now.plusSeconds(10)).build(MGI_ID_2), ANONYMOUS_USER_ID));
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(createDuel(ANONYMOUS_USER_ID).withExpiryDateTime(now.plusSeconds(20)).build(MGI_ID_3), ANONYMOUS_USER_ID));
		
		assertThat(publicGameDao.getNextPublicGame(new PublicGameFilter(APP_ID)).getMultiplayerGameInstanceId(), equalTo(MGI_ID_2));
	}

	@Test
	public void testGetNextPublicGameCount() {
		ZonedDateTime now = DateTimeUtil.getCurrentDateTime();
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(createDuel(ANONYMOUS_USER_ID).withExpiryDateTime(now.plusSeconds(30)).build(MGI_ID_1), ANONYMOUS_USER_ID));
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(createDuel(REGISTERED_USER_ID).withExpiryDateTime(now.plusSeconds(20)).build(MGI_ID_3), ANONYMOUS_USER_ID));
		PublicGameFilter publicGameFilter = new PublicGameFilter(APP_ID);
		publicGameFilter.setNotByUserId(ANONYMOUS_USER_ID);
		assertThat(publicGameDao.getNumberOfPublicGames(publicGameFilter), equalTo(1L));
	}

	@Test
	public void testRandomSelection() {
		int limit = 13;
		int countOfAllMgiIds = limit * 3;
		int middle = limit / 2;
		
		// prepare public games with MGI IDs "0".."<countOfAllMgiIds - 1>"
		for (int i = 0; i < countOfAllMgiIds; i++) {
			CustomGameConfig publicGame = createDuel(ANONYMOUS_USER_ID)
					.withExpiryDateTime(DateTimeUtil.getCurrentDateTime().plusSeconds(60 + 10*(i+1)))
					.build(String.valueOf(i));
			publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(publicGame, ANONYMOUS_USER_ID));
		}
		
		// validate
		PublicGameFilter filter = new PublicGameFilter(APP_ID);
		List<Invitation> publicGames = publicGameDao.searchPublicGames(filter, limit);
		assertThat(publicGames.size(), is(limit));
		
		List<String> actualMgiIds = publicGames.stream().map(g -> g.getMultiplayerGameInstanceId()).collect(Collectors.toList());
		String[] expectedMgiIds = IntStream.range(0, limit)
				.mapToObj(i -> {
					if (i <= middle) {
						// 50% of old public games
						return String.valueOf(i);
					} else {
						// 50% of new public games
						return String.valueOf(countOfAllMgiIds - (i - middle));
					}
				})
				.toArray(String[]::new);
		assertThat(actualMgiIds, containsInAnyOrder(expectedMgiIds));
	}
	
	@Test
	public void testRemoveExpiredPublicGames() throws Exception {
		ZonedDateTime currentDateTime = DateTimeUtil.getCurrentDateTime();
		int countOfGames = 29;
		int countOfExpiredGames = countOfGames / 2;
		config.setProperty(GameConfigImpl.EXPIRED_PUBLIC_GAME_LIST_SIZE, 3);
		
		// prepare expired games
		for (int i = 0; i < countOfExpiredGames; i++) {
			CustomGameConfig publicGame = createDuel(ANONYMOUS_USER_ID)
					.withExpiryDateTime(currentDateTime.minusMinutes(i + 1))
					.build("expired_" + i);
			publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(publicGame, ANONYMOUS_USER_ID));
		}
		
		// prepare active games
		String[] activeGames = new String[countOfGames - countOfExpiredGames];
		for (int i = 0; i < countOfGames - countOfExpiredGames; i++) {
			String mgiId = "active_" + i;
			CustomGameConfig publicGame = createDuel(ANONYMOUS_USER_ID)
					.withExpiryDateTime(currentDateTime.plusMinutes(i + 1))
					.build(mgiId);
			publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(publicGame, ANONYMOUS_USER_ID));
			activeGames[i] = mgiId;
		}
		
		assertPublicGames(new PublicGameFilter(APP_ID), activeGames);
	}
	
	@Test
	public void testMoveUserOfPublicGame() throws Exception {
		// prepare: public games where ANONYMOUS_USER_ID is ...
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(createDuel(ANONYMOUS_USER_ID).build(MGI_ID_1), ANONYMOUS_USER_ID)); // creator and inviter
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(createDuel(USER_ID_1).build(MGI_ID_2), ANONYMOUS_USER_ID)); // inviter
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(createDuel(ANONYMOUS_USER_ID).build(MGI_ID_3), USER_ID_1)); // creator
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(createDuel(USER_ID_1).build(MGI_ID_4), USER_ID_1)); // non

		// validate
		assertPublicGameCreatorAndInviter(MGI_ID_1, ANONYMOUS_USER_ID, ANONYMOUS_USER_ID);
		assertPublicGameCreatorAndInviter(MGI_ID_2, USER_ID_1, ANONYMOUS_USER_ID);
		assertPublicGameCreatorAndInviter(MGI_ID_3, ANONYMOUS_USER_ID, USER_ID_1);
		assertPublicGameCreatorAndInviter(MGI_ID_4, USER_ID_1, USER_ID_1);

		// test: change ANONYMOUS_USER_ID to REGISTERED_USER_ID
		publicGameDao.changeUserOfPublicGame(ANONYMOUS_USER_ID, REGISTERED_USER_ID);

		// validate
		assertPublicGameCreatorAndInviter(MGI_ID_1, REGISTERED_USER_ID, REGISTERED_USER_ID);
		assertPublicGameCreatorAndInviter(MGI_ID_2, USER_ID_1, REGISTERED_USER_ID);
		assertPublicGameCreatorAndInviter(MGI_ID_3, REGISTERED_USER_ID, USER_ID_1);
		assertPublicGameCreatorAndInviter(MGI_ID_4, USER_ID_1, USER_ID_1);
	}
	
	@Test
	public void testRemoveLiveTournamentsWithExpiredPlayDateTime() throws Exception {
		// test if fails on first call on empty elastic
		publicGameDao.removeLiveTournamentsWithExpiredPlayDateTime();
		
		ZonedDateTime now = DateTimeUtil.getCurrentDateTime();
		CustomGameConfigBuilder mgi = create(ANONYMOUS_USER_ID).withGameType(GameType.LIVE_TOURNAMENT);
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(mgi.withPlayDateTime(now.plusHours(1)).build(MGI_ID_1), ANONYMOUS_USER_ID)); // live tournament in 1 hour
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(mgi.withPlayDateTime(now.minusHours(1)).build(MGI_ID_2), ANONYMOUS_USER_ID)); // live tournament 1 hour ago
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(mgi.withGameType(GameType.DUEL).withPlayDateTime(null).build(MGI_ID_3), ANONYMOUS_USER_ID)); // duel without play date
		
		// verify
		assertPublicGames(new PublicGameFilter(APP_ID), MGI_ID_1, MGI_ID_2, MGI_ID_3);
		
		CustomGameConfigBuilder ult = create(ANONYMOUS_USER_ID).withGameType(GameType.USER_LIVE_TOURNAMENT);
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(ult.withPlayDateTime(now.minusHours(1)).build(MGI_ID_5_ULT), ANONYMOUS_USER_ID)); // live tournament 1 hour ago
		assertPublicGames(new PublicGameFilter(APP_ID), MGI_ID_1, MGI_ID_2, MGI_ID_3, MGI_ID_5_ULT);

		// test
		publicGameDao.removeLiveTournamentsWithExpiredPlayDateTime();
		
		// validate
		assertPublicGames(new PublicGameFilter(APP_ID), MGI_ID_1, MGI_ID_3);
	}

	private void assertPublicGames(PublicGameFilter publicGameFilter, String... mgiIds) {
		List<Invitation> publicGames = publicGameDao.searchPublicGames(publicGameFilter, 100);
		List<String> actualMgiIds = publicGames.stream().map(g -> g.getMultiplayerGameInstanceId())
				.collect(Collectors.toList());
		assertThat(actualMgiIds, containsInAnyOrder(mgiIds));
	}

	private void assertPublicGameCreatorAndInviter(String mgiId, String creatorId, String inviterId) {
		Invitation publicGame = publicGameDao.getPublicGame(APP_ID, mgiId);
		assertThat(publicGame.getCreator().getUserId(), equalTo(creatorId));
		assertThat(publicGame.getInviter().getUserId(), equalTo(inviterId));
	}

	private void prepareData() {
		CustomGameConfig duelCustomConfig1 = createDuel(ANONYMOUS_USER_ID)
				.withGameTitle("First Duel")
				.withGameDescription("Duel description")
				.withPlayingRegions("en", "de", "fr")
				.withPools("history", "sport")
				.withNumberOfQuestions(3)
				.withEntryFee(new BigDecimal("5.50"), Currency.MONEY)
				.build(MGI_ID_1);
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(duelCustomConfig1, ANONYMOUS_USER_ID));
		
		CustomGameConfig duelCustomConfig2 = createDuel(ANONYMOUS_USER_ID)
				.withGameTitle("Second duel")
				.withPlayingRegions("en")
				.withPools("sport")
				.withEntryFee(new BigDecimal("8.00"), Currency.MONEY)
				.build(MGI_ID_2);
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID, APP_ID_1), new Invitation(duelCustomConfig2, ANONYMOUS_USER_ID));
		
		CustomGameConfig duelCustomConfig3 = create(ANONYMOUS_USER_ID)
				.withGameTitle("1st normal T")
				.withGameDescription("First tournament description")
				.withGameType(GameType.TOURNAMENT)
				.withPlayingRegions("en", "de")
				.withPools("history", "sport")
				.withNumberOfQuestions(7)
				.build(MGI_ID_3);
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID, APP_ID_1), new Invitation(duelCustomConfig3, ANONYMOUS_USER_ID));
		
		CustomGameConfig duelCustomConfig4 = create(ANONYMOUS_USER_ID)
				.withGameTitle("Second normal tournament")
				.withGameType(GameType.TOURNAMENT)
				.withEntryFee(new BigDecimal("0.00"), Currency.CREDIT)
				.build(MGI_ID_4);
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(duelCustomConfig4, ANONYMOUS_USER_ID));

		CustomGameConfig userLiveTournamentConfig = create(ANONYMOUS_USER_ID)
				.withGameTitle("A live tournament by user")
				.withGameType(GameType.USER_LIVE_TOURNAMENT)
				.build(MGI_ID_5_ULT);
		publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(userLiveTournamentConfig, ANONYMOUS_USER_ID));
	}
	
	private void deleteData() {
		publicGameDao.delete(MGI_ID_1, true, false);
		publicGameDao.delete(MGI_ID_2, true, false);
		publicGameDao.delete(MGI_ID_3, true, false);
		publicGameDao.delete(MGI_ID_4, true, false);
	}

}
