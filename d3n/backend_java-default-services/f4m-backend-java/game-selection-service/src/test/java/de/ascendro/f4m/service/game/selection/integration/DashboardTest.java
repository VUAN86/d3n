package de.ascendro.f4m.service.game.selection.integration;

import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.APP_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;
import com.google.inject.Stage;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.dashboard.dao.DashboardDao;
import de.ascendro.f4m.server.dashboard.dao.DashboardDaoImpl;
import de.ascendro.f4m.server.elastic.ElasticsearchTestNode;
import de.ascendro.f4m.server.game.GameAerospikeDao;
import de.ascendro.f4m.server.game.util.GameEnginePrimaryKeyUtil;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDaoImpl;
import de.ascendro.f4m.server.multiplayer.dao.PublicGameElasticDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.game.selection.GameSelectionMessageTypes;
import de.ascendro.f4m.service.game.selection.builder.GetDashboardRequestBuilder;
import de.ascendro.f4m.service.game.selection.builder.UpdatePlayedGameRequestBuilder;
import de.ascendro.f4m.service.game.selection.dao.GameSelectorAerospikeDao;
import de.ascendro.f4m.service.game.selection.dao.GameSelectorAerospikeDaoImpl;
import de.ascendro.f4m.service.game.selection.dao.GameSelectorPrimaryKeyUtil;
import de.ascendro.f4m.service.game.selection.model.dashboard.ApiDuel;
import de.ascendro.f4m.service.game.selection.model.dashboard.ApiQuiz;
import de.ascendro.f4m.service.game.selection.model.dashboard.ApiTournament;
import de.ascendro.f4m.service.game.selection.model.dashboard.ApiUserTournament;
import de.ascendro.f4m.service.game.selection.model.dashboard.GetDashboardResponse;
import de.ascendro.f4m.service.game.selection.model.dashboard.NextInvitation;
import de.ascendro.f4m.service.game.selection.model.dashboard.PlayedDuelInfo;
import de.ascendro.f4m.service.game.selection.model.dashboard.PlayedGameInfo;
import de.ascendro.f4m.service.game.selection.model.dashboard.PlayedTournamentInfo;
import de.ascendro.f4m.service.game.selection.model.dashboard.SpecialGameInfo;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.GameTypeConfiguration;
import de.ascendro.f4m.service.game.selection.model.game.MultiplayerGameTypeConfigurationData;
import de.ascendro.f4m.service.game.selection.model.game.SingleplayerGameTypeConfigurationData;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder;
import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerGameParameters;
import de.ascendro.f4m.service.game.selection.server.DashboardManager;
import de.ascendro.f4m.service.game.selection.server.MultiplayerGameInstanceManager;
import de.ascendro.f4m.service.game.usergameaccess.UserGameAccessDaoImpl;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.profile.model.api.ApiProfileBasicInfo;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class DashboardTest extends GameSelectionServiceTestBase {
	
	private static final double SPECIAL_GAME_SORT_WEIGHT = 1.0;

	private static final Currency SPECIAL_GAME_ENTRY_FEE_CURRENCY = Currency.MONEY;

	private static final String SPECIAL_GAME_ENTRY_FEE = BigDecimal.ONE.toString();

	private static final boolean SPECIAL_GAME_TO_BUY_ONCE = false;

	private static final int SPECIAL_GAME_BATCH_SIZE = 3;

	private static final Logger LOGGER = LoggerFactory.getLogger(DashboardTest.class);
	
	private static final String GAME_ID_1 = "game_id_1";
	private static final String GAME_ID_2 = "game_id_2";
	private static final String GAME_ID_3 = "game_id_3";
	private static final String GAME_ID_4 = "game_id_4";
	private static final String GAME_ID_5 = "game_id_5";
	private static final String MGI_ID_1 = "mgi_id_1";
	private static final String MGI_ID_2 = "mgi_id_2";
	private static final String GAME_INSTANCE_ID_1 = "gi_id_1";
	private static final String GAME_INSTANCE_ID_2 = "gi_id_2";
	private static final String USER_ID_1 = "user_id_1";
	private static final String USER_ID_2 = "user_id_2";
	private static final String USER_2_FIRST = "Leonardo";
	private static final String USER_2_LAST = "Bonacci";
	private static final String USER_2_NICK = "Bona";
	private static final String USER_2_COUNTRY = "Italy";
	private static final String USER_2_CITY = "Pisa";
	
	private static final int ELASTIC_PORT = 9210;
	
	private GameSelectorAerospikeDao gameSelectorAerospikeDao;
	private GameSelectorPrimaryKeyUtil gameSelectorPrimaryKeyUtil;
	private GameEnginePrimaryKeyUtil gameEnginePrimaryKeyUtil;
	private JsonUtil jsonUtil;
	private DashboardManager dashboardManager;
	private MultiplayerGameInstanceManager mgiManager;
	private CommonMultiplayerGameInstanceDao mgiDao;
	private DashboardDao dashboardDao;
	private ProfilePrimaryKeyUtil profileKeyUtil;
	private PublicGameElasticDao publicGameDao;
	private ZonedDateTime now = DateTimeUtil.getCurrentDateTime().truncatedTo(ChronoUnit.SECONDS);

	@BeforeClass
	public static void setUpClass() {
		F4MServiceWithMockIntegrationTestBase.setUpClass();
		System.setProperty(ElasticConfigImpl.ELASTIC_SERVER_HOSTS, "http://localhost:" + ELASTIC_PORT);
		System.setProperty(ElasticConfigImpl.ELASTIC_HEADER_REFRESH, ElasticConfigImpl.ELASTIC_HEADER_VALUE_WAIT_FOR);
	}

	@Rule
	public ElasticsearchTestNode elastic = new ElasticsearchTestNode(ELASTIC_PORT);

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		
		Injector injector = jettyServerRule.getServerStartup().getInjector();
		
		gameSelectorAerospikeDao = injector.getInstance(GameSelectorAerospikeDaoImpl.class);
		gameSelectorPrimaryKeyUtil = injector.getInstance(GameSelectorPrimaryKeyUtil.class);
		gameEnginePrimaryKeyUtil = injector.getInstance(GameEnginePrimaryKeyUtil.class);
		jsonUtil = injector.getInstance(JsonUtil.class);
		dashboardManager = injector.getInstance(DashboardManager.class);
		mgiManager = injector.getInstance(MultiplayerGameInstanceManager.class);
		mgiDao = injector.getInstance(CommonMultiplayerGameInstanceDaoImpl.class);
		dashboardDao = injector.getInstance(DashboardDaoImpl.class);
		profileKeyUtil = injector.getInstance(ProfilePrimaryKeyUtil.class);
		publicGameDao = injector.getInstance(PublicGameElasticDao.class);
	}
	
	@Override
	protected ServiceStartup getServiceStartup() {
		return new GameSelectionStartupUsingAerospikeMock(Stage.PRODUCTION);
	}
	
	@Override
	protected JsonMessageContent onReceivedMessage(RequestContext requestContext) throws Exception {
		JsonMessage<? extends JsonMessageContent> message = requestContext.getMessage();
		JsonMessageContent response = null;
		LOGGER.debug("Mocked Service of DashboardTest received request {}", message.getContent());
		
		if (UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE == message.getType(UserMessageMessageTypes.class)) {
			// ignore
		} else {
			return super.onReceivedMessage(requestContext);
		}
		return response;
	}

	@Test
	public void testDashboardQuizData() throws Exception {
		prepareTestData(GameType.QUIZ24, false);
		
		String requestJson = GetDashboardRequestBuilder.create().withQuiz(true).build();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.GET_DASHBOARD_RESPONSE);
		
		JsonMessage<GetDashboardResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.GET_DASHBOARD_RESPONSE);
		GetDashboardResponse content = response.getContent();
		
		ApiQuiz quiz = content.getQuiz();
		assertThat(quiz.getNumberOfGamesAvailable(), equalTo(3)); // GAME_ID_1, GAME_ID_2 and GAME_ID_3
		assertThat(quiz.getLastPlayedGame().getGameId(), equalTo(GAME_ID_1));
		
		assertThat(quiz.getMostPlayedGame().getGameInfo().getGameId(), equalTo(GAME_ID_2));
		assertThat(quiz.getMostPlayedGame().getNumberOfPlayers(), equalTo(3L)); // ANONYMOUS_USER_ID once, USER_ID_1 twice
		
		assertThat(quiz.getNumberOfSpecialGamesAvailable(), equalTo(0));
		assertNull(quiz.getLatestSpecialGame());
	}

	@Test
	public void testDashboardQuizDataWithSpecialGames() throws Exception {
		prepareTestData(GameType.QUIZ24, true);

		String requestJson = GetDashboardRequestBuilder.create().withQuiz(true).build();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.GET_DASHBOARD_RESPONSE);

		JsonMessage<GetDashboardResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.GET_DASHBOARD_RESPONSE);
		GetDashboardResponse content = response.getContent();

		ApiQuiz quiz = content.getQuiz();
		assertThat(quiz.getNumberOfGamesAvailable(), equalTo(3)); // GAME_ID_1, GAME_ID_2 and GAME_ID_3
		assertThat(quiz.getLastPlayedGame().getGameId(), equalTo(GAME_ID_1));

		assertThat(quiz.getMostPlayedGame().getGameInfo().getGameId(), equalTo(GAME_ID_2));
		assertThat(quiz.getMostPlayedGame().getNumberOfPlayers(), equalTo(3L)); // ANONYMOUS_USER_ID once, USER_ID_1 twice
		
		assertThat(quiz.getNumberOfSpecialGamesAvailable(), equalTo(2));
		
		SpecialGameInfo latestSpecialGame = quiz.getLatestSpecialGame();
		assertThat(latestSpecialGame.getGameId(), equalTo(GAME_ID_5));
		assertThat(latestSpecialGame.getEntryFeeBatchSize(), equalTo(SPECIAL_GAME_BATCH_SIZE));
		assertThat(latestSpecialGame.getEntryFeeAmount(), equalTo(BigDecimal.ONE));
		assertThat(latestSpecialGame.getEntryFeeCurrency(), equalTo(SPECIAL_GAME_ENTRY_FEE_CURRENCY.name()));
	}

	@Test
	public void testDashboardQuizDataWithSpecialGamesInvisible() throws Exception {
		prepareTestData(GameType.QUIZ24, true);
		playedSpecialGameUntilInvisible(GAME_ID_5, KeyStoreTestUtil.ANONYMOUS_USER_ID);

		String requestJson = GetDashboardRequestBuilder.create().withQuiz(true).build();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.GET_DASHBOARD_RESPONSE);

		JsonMessage<GetDashboardResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.GET_DASHBOARD_RESPONSE);
		GetDashboardResponse content = response.getContent();

		ApiQuiz quiz = content.getQuiz();
		assertThat(quiz.getNumberOfGamesAvailable(), equalTo(3)); // GAME_ID_1, GAME_ID_2 and GAME_ID_3
		assertThat(quiz.getLastPlayedGame().getGameId(), equalTo(GAME_ID_1));

		assertThat(quiz.getMostPlayedGame().getGameInfo().getGameId(), equalTo(GAME_ID_2));
		assertThat(quiz.getMostPlayedGame().getNumberOfPlayers(), equalTo(3L)); // ANONYMOUS_USER_ID once, USER_ID_1 twice
		
		assertThat(quiz.getNumberOfSpecialGamesAvailable(), equalTo(2));
		
		SpecialGameInfo latestSpecialGame = quiz.getLatestSpecialGame();
		assertThat(latestSpecialGame.getGameId(), equalTo(GAME_ID_4));
		assertThat(latestSpecialGame.getEntryFeeBatchSize(), equalTo(SPECIAL_GAME_BATCH_SIZE));
		assertThat(latestSpecialGame.getEntryFeeAmount(), equalTo(BigDecimal.ONE));
		assertThat(latestSpecialGame.getEntryFeeCurrency(), equalTo(SPECIAL_GAME_ENTRY_FEE_CURRENCY.name()));
	}
	
	@SuppressWarnings("unchecked")
	private void playedSpecialGameUntilInvisible(String gameId, String userId) {
		AerospikeDaoImpl<GameSelectorPrimaryKeyUtil> userGameAccessDao = (AerospikeDaoImpl<GameSelectorPrimaryKeyUtil>) gameSelectorAerospikeDao;
		String aerospikeUserGameAccessSet = config.getProperty(GameConfigImpl.AEROSPIKE_USER_GAME_ACCESS_SET);
		String primaryKey = gameEnginePrimaryKeyUtil.createUserGameAccessId(gameId, userId);
		userGameAccessDao.add(aerospikeUserGameAccessSet, primaryKey, UserGameAccessDaoImpl.USER_GAME_ACCESS_BIN_NAME, 0);
		
	}

	@Test
	public void testDashboardDuelData() throws Exception {
		prepareProfileUser2();
		prepareTestData(GameType.DUEL, false);
		
		String requestJson = GetDashboardRequestBuilder.create().withDuel(true).build();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.GET_DASHBOARD_RESPONSE);
		
		JsonMessage<GetDashboardResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.GET_DASHBOARD_RESPONSE);
		GetDashboardResponse content = response.getContent();
		
		ApiDuel duelDashboard = content.getDuel();
		// validate last duel
		PlayedGameInfo lastDuel = duelDashboard.getLastDuel();
		assertThat(lastDuel.getGameId(), equalTo(GAME_ID_2));
		assertThat(lastDuel.getDuelInfo().getOpponentUserId(), equalTo(USER_ID_2));
		ApiProfileBasicInfo opponentInfo = lastDuel.getDuelInfo().getOpponentInfo();
		assertThat(opponentInfo.getFirstName(), Matchers.nullValue());
		assertThat(opponentInfo.getLastName(), Matchers.nullValue());
		assertThat(opponentInfo.getNickname(), equalTo(USER_2_NICK));
		assertThat(opponentInfo.getCountry(), equalTo(USER_2_COUNTRY));
		assertThat(opponentInfo.getCity(), equalTo(USER_2_CITY));


		// validate next duel invitation
		NextInvitation nextDuel = duelDashboard.getNextInvitation();
		assertThat(nextDuel.getGameId(), equalTo(GAME_ID_1));
		assertThat(nextDuel.getInviterId(), equalTo(USER_ID_2));

		assertThat(nextDuel.getStartDateTime(), equalTo(now.plusMinutes(2)));
		assertNull(nextDuel.getPlayDateTime());

		// validate next public game
		NextInvitation nextPublicGame = duelDashboard.getNextPublicGame();
		assertThat(nextPublicGame.getGameId(), equalTo(GAME_ID_1));
		assertThat(nextPublicGame.getInviterId(), equalTo(USER_ID_1));
		
		// validate number of invitations
		assertThat(duelDashboard.getNumberOfInvitations(), equalTo(3L));
		
		// validate number of public games
		assertThat(duelDashboard.getNumberOfPublicGames(), equalTo(1L));
	}
	
	@Test
	public void testDashboardUserTournamentData() throws Exception {
		GetDashboardResponse content = testDashboardTournament(GameType.USER_TOURNAMENT,
				GetDashboardResponse::getUserTournament, GetDashboardRequestBuilder.create().withUserTournament(true));
		ApiUserTournament userTournament = content.getUserTournament();
		PlayedGameInfo lastTournament = userTournament.getLastTournament();
		assertThat(lastTournament.getGameId(), equalTo(GAME_ID_2));
		assertThat(lastTournament.getTournamentInfo().getGameInstanceId(), equalTo(GAME_INSTANCE_ID_2));
		assertThat(lastTournament.getTournamentInfo().getMgiId(), equalTo(MGI_ID_2));
	}
	
	@Test
	public void testDashboardUserLiveTournamentData() throws Exception {
		GetDashboardResponse content = testDashboardTournament(GameType.USER_LIVE_TOURNAMENT,
				GetDashboardResponse::getUserLiveTournament, GetDashboardRequestBuilder.create().withUserLiveTournament(true));
		ApiUserTournament userTournament = content.getUserLiveTournament();
		PlayedGameInfo lastTournament = userTournament.getLastTournament();
		assertThat(lastTournament.getGameId(), equalTo(GAME_ID_2));
		assertThat(lastTournament.getTournamentInfo().getGameInstanceId(), equalTo(GAME_INSTANCE_ID_2));
		assertThat(lastTournament.getTournamentInfo().getMgiId(), equalTo(MGI_ID_2));
	}
	
	@Test
	public void testDashboardTournamentData() throws Exception {
		testDashboardTournament(GameType.TOURNAMENT, GetDashboardResponse::getTournament,
				GetDashboardRequestBuilder.create().withTournament(true));
	}

	@Test
	public void testDashboardLiveTournamentData() throws Exception {
		testDashboardTournament(GameType.LIVE_TOURNAMENT, GetDashboardResponse::getLiveTournament,
				GetDashboardRequestBuilder.create().withLiveTournament(true));
	}

	private GetDashboardResponse testDashboardTournament(GameType gameType,
			Function<GetDashboardResponse, ApiTournament> tournamentGetter, GetDashboardRequestBuilder builder) throws Exception {
		prepareTestData(gameType, false);
		
		String requestJson = builder.build();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.GET_DASHBOARD_RESPONSE);
		
		JsonMessage<GetDashboardResponse> response = testClientReceivedMessageCollector.getMessageByType(GameSelectionMessageTypes.GET_DASHBOARD_RESPONSE);
		GetDashboardResponse content = response.getContent();
		
		ApiTournament tournament = tournamentGetter.apply(content);
		NextInvitation nextInvitation = tournament.getNextInvitation();
		assertThat(nextInvitation.getGameId(), equalTo(GAME_ID_1));
		assertThat(nextInvitation.getInviterId(), equalTo(USER_ID_1));

		assertThat(tournament.getNextInvitation().getExpiryDateTime(), equalTo(now.plusMinutes(5)));
		assertThat(tournament.getNextInvitation().getStartDateTime(), equalTo(now.plusMinutes(2)));
		assertThat(tournament.getNextInvitation().getPlayDateTime(), equalTo(now.plusMinutes(3)));
		final NextInvitation nextTournament = tournament.getNextTournament();
		if (gameType == GameType.USER_TOURNAMENT || gameType == GameType.USER_LIVE_TOURNAMENT) {
			assertNull(nextTournament.getExpiryDateTime());
			assertNull(nextTournament.getStartDateTime());
			assertNull(nextTournament.getPlayDateTime());
			assertNull(nextTournament.getGameId());
			assertNull(nextTournament.getInviterId());

			assertThat(tournament.getNumberOfTournaments(), equalTo(3L));
		} else {
			assertThat(nextTournament.getExpiryDateTime(), equalTo(now.plusMinutes(3)));
			assertThat(nextTournament.getStartDateTime(), equalTo(now));
			assertThat(nextTournament.getPlayDateTime(), equalTo(now.plusMinutes(1)));
			assertThat(nextTournament.getGameId(), equalTo(GAME_ID_2));
			assertThat(nextTournament.getInviterId(), equalTo(USER_ID_1));

			assertThat(tournament.getNumberOfTournaments(), equalTo(2L));
		}
		assertThat(tournament.getNumberOfInvitations(), equalTo(1L));
		return content;
	}

	private void prepareProfileUser2() {
		Profile profile = new Profile();
		profile.setUserId(USER_ID_2);
		profile.setShowFullName(false);
		ProfileUser person = new ProfileUser();
		person.setFirstName(USER_2_FIRST);
		person.setLastName(USER_2_LAST);
		person.setNickname(USER_2_NICK);
		profile.setPersonWrapper(person);
		ProfileAddress address = new ProfileAddress();
		address.setCountry(USER_2_COUNTRY);
		address.setCity(USER_2_CITY);
		profile.setAddress(address);
		
		String key = profileKeyUtil.createPrimaryKey(USER_ID_2);
		String set = config.getProperty(AerospikeConfigImpl.AEROSPIKE_PROFILE_SET);
		((AerospikeDao) gameSelectorAerospikeDao).createJson(set, key, CommonProfileAerospikeDao.BLOB_BIN_NAME, profile.getAsString());
	}

	private void prepareTestData(GameType gameType, boolean withSpecialGames) {
		List<String> gamesList = Arrays.asList(GAME_ID_1, GAME_ID_2, GAME_ID_3);
		List<String> specialGames = Arrays.asList(GAME_ID_4, GAME_ID_5);
		if(withSpecialGames) {
			prepareGameList(gameType, gamesList, specialGames);
		} else {
			prepareGameList(gameType, gamesList, null);
		}

		switch (gameType) {
		case QUIZ24:
			preparePlayedQuizes();
			break;
		case DUEL:
			preparePlayedDuels();
			prepareNextDuels();
			break;
		case USER_TOURNAMENT:
		case LIVE_TOURNAMENT:
		case USER_LIVE_TOURNAMENT:
		case TOURNAMENT:
			preparePlayedTournaments(gameType);
			prepareNextTournaments(gameType);
			break;
		default:
			throw new IllegalArgumentException(String.format("Not supported game type [%s]", gameType));
		}
	}

	@SuppressWarnings("unchecked")
	private void prepareGameList(GameType gameType, List<String> gameIds, List<String> specialGameIds) {
		AerospikeDaoImpl<GameSelectorPrimaryKeyUtil> gameDao = (AerospikeDaoImpl<GameSelectorPrimaryKeyUtil>) gameSelectorAerospikeDao;
		String aerospikeGameSet = config.getProperty(AerospikeConfigImpl.AEROSPIKE_GAME_SET);

		List<Game> gameList = new ArrayList<>();

		gameIds.forEach(id -> {
            Game game = addGame(gameType, id, false);
			gameList.add(game);

			String primaryKey = gameSelectorPrimaryKeyUtil.createPrimaryKey(id);
			gameDao.createJson(aerospikeGameSet, primaryKey, GameAerospikeDao.BLOB_BIN_NAME, jsonUtil.toJson(game));
		});

		if(specialGameIds != null) {
			specialGameIds.forEach(id -> {
                Game game = addGame(gameType, id, true);
				gameList.add(game);

				String primaryKey = gameSelectorPrimaryKeyUtil.createPrimaryKey(id);
				gameDao.createJson(aerospikeGameSet, primaryKey, GameAerospikeDao.BLOB_BIN_NAME, jsonUtil.toJson(game));
			});
		}

		String searchKey = gameSelectorPrimaryKeyUtil.createSearchKey(TENANT_ID, APP_ID, Game.SEARCH_FIELD_TYPE, gameType.name());
		gameDao.createJson(aerospikeGameSet, searchKey, GameAerospikeDao.BLOB_BIN_NAME, jsonUtil.toJson(gameList));
	}

    private Game addGame(GameType gameType, String id, boolean isSpecial) {
        Game game = new Game();
        game.setGameId(id);
        game.setApplications(new String[]{APP_ID});
        game.setType(gameType);
        game.setTimeToAcceptInvites(30); // 30 minutes

        if (GameType.QUIZ24 == gameType) {
            if (isSpecial) {
                game.setEntryFeeAmount(new BigDecimal(SPECIAL_GAME_ENTRY_FEE));
                game.setEntryFeeCurrency(SPECIAL_GAME_ENTRY_FEE_CURRENCY);
            	game.setEntryFeeBatchSize(SPECIAL_GAME_BATCH_SIZE);
            	game.setMultiplePurchaseAllowed(SPECIAL_GAME_TO_BUY_ONCE);
            }
            game.setTypeConfiguration(new GameTypeConfiguration(null, null, new SingleplayerGameTypeConfigurationData(isSpecial, SPECIAL_GAME_SORT_WEIGHT, null)));
        } else {
    		MultiplayerGameTypeConfigurationData duelTournament = new MultiplayerGameTypeConfigurationData();
    		duelTournament.setMinimumPlayerNeeded(2);
        	game.setTypeConfiguration(new GameTypeConfiguration(null, duelTournament, null));
        }
        return game;
    }

    private void preparePlayedQuizes() {
		dashboardManager.updatePlayedGame(TENANT_ID, ANONYMOUS_USER_ID, new PlayedGameInfo(GAME_ID_2, GameType.QUIZ24, "Game2", false));
		dashboardManager.updatePlayedGame(TENANT_ID, ANONYMOUS_USER_ID, new PlayedGameInfo(GAME_ID_1, GameType.QUIZ24, "Game1", false));
		
		dashboardManager.updatePlayedGame(TENANT_ID, USER_ID_1, new PlayedGameInfo(GAME_ID_2, GameType.QUIZ24, "Game2", false));
		dashboardManager.updatePlayedGame(TENANT_ID, USER_ID_1, new PlayedGameInfo(GAME_ID_2, GameType.QUIZ24, "Game2", false));
	}
	
	private void preparePlayedDuels() {
		PlayedGameInfo game1vsUser1 = new PlayedGameInfo(GAME_ID_1, GameType.DUEL, "Game1", new PlayedDuelInfo(KeyStoreTestUtil.ANONYMOUS_USER_ID, USER_ID_1), false);
		dashboardManager.updatePlayedGame(TENANT_ID, ANONYMOUS_USER_ID, game1vsUser1);
		PlayedGameInfo game2vsUser2 = new PlayedGameInfo(GAME_ID_2, GameType.DUEL, "Game2", new PlayedDuelInfo(KeyStoreTestUtil.ANONYMOUS_USER_ID, USER_ID_2), false);
		dashboardManager.updatePlayedGame(TENANT_ID, ANONYMOUS_USER_ID, game2vsUser2);
	}
	
	private void preparePlayedTournaments(GameType gameType) {
		PlayedTournamentInfo tournamentInfo1 = new PlayedTournamentInfo(MGI_ID_1, GAME_INSTANCE_ID_1);
		tournamentInfo1.setPlacement(1);
		PlayedGameInfo game1 = new PlayedGameInfo(GAME_ID_1, gameType, "Game1", tournamentInfo1);
		dashboardManager.updatePlayedGame(TENANT_ID, ANONYMOUS_USER_ID, game1);
		
		PlayedTournamentInfo tournamentInfo2 = new PlayedTournamentInfo(MGI_ID_2, GAME_INSTANCE_ID_2);
		tournamentInfo2.setPlacement(13);
		PlayedGameInfo game2 = new PlayedGameInfo(GAME_ID_2, gameType, "Game2", tournamentInfo2);
		dashboardManager.updatePlayedGame(TENANT_ID, ANONYMOUS_USER_ID, game2);
	}
	
	private void prepareNextDuels() {
		inviteUserToGame(GAME_ID_1, GameType.DUEL, USER_ID_1, ANONYMOUS_USER_ID, now.plusMinutes(10), now.plusMinutes(5)); // expires in 10 minutes, starts in 5
		inviteUserToGame(GAME_ID_1, GameType.DUEL, USER_ID_2, ANONYMOUS_USER_ID, now.plusMinutes(5), now.plusMinutes(2)); // expires in 5 minutes, starts in 2
		inviteUserToGame(GAME_ID_2, GameType.DUEL, USER_ID_1, ANONYMOUS_USER_ID, now.plusMinutes(15), now.plusMinutes(10));  // expires in 15 minutes, starts in 10
		
		createPublicGame(GAME_ID_1, USER_ID_1, now.plusMinutes(5)); // expires in 5 minutes
		createPublicGame(GAME_ID_2, ANONYMOUS_USER_ID, now.plusMinutes(10)); // expires in 10 minutes
	}
	
	private void prepareNextTournaments(GameType gameType) {
		inviteUserToGame(GAME_ID_1, gameType, USER_ID_1, ANONYMOUS_USER_ID, now.plusMinutes(5), now.plusMinutes(2),
				now.plusMinutes(3)); // expires in 5 minutes, starts in 2 minutes, plays in 3 minutes
		inviteUserToGame(GAME_ID_2, gameType, USER_ID_1, USER_ID_2, now.plusMinutes(3), now,
				now.plusMinutes(1)); // expires in 3 minutes, starts now, plays in 1 minute
		// add e game where self invited, to check that self invitations are indeed excluded
		inviteUserToGame(GAME_ID_3, gameType, ANONYMOUS_USER_ID, ANONYMOUS_USER_ID, now.plusMinutes(4), now.plusMinutes(2),
				now.plusMinutes(3)); // expires in 4 minutes, starts in 3 minutes, plays in 4 minutes
	}

	private void inviteUserToGame(String gameId, GameType gameType, String inviterId, String inviteeId,
			ZonedDateTime expiryDateTime, ZonedDateTime startDateTime ) {
		inviteUserToGame(gameId, gameType, inviterId, inviteeId, expiryDateTime, startDateTime, null);
	}

	private void inviteUserToGame(String gameId, GameType gameType, String inviterId, String inviteeId,
			ZonedDateTime expiryDateTime, ZonedDateTime startDateTime, ZonedDateTime playDateTime) {
		CustomGameConfig gameConfig = CustomGameConfigBuilder
				.create(TENANT_ID, APP_ID, gameId, inviterId)
				.withGameType(gameType)
				.withStartDateTime(startDateTime)
				.withPlayDateTime(playDateTime)
				.withExpiryDateTime(expiryDateTime)
				.build();
		writeGameConfigAndInvitation(gameType, inviterId, inviteeId, gameConfig);
	}

	private void writeGameConfigAndInvitation(GameType gameType, String inviterId, String inviteeId, CustomGameConfig gameConfig) {
		String mgiId = mgiDao.create(inviterId, gameConfig);
		mgiDao.registerForGame(mgiId, new ClientInfo("t1", "a1", inviterId, "ip", null), "any_game_instance_id");
		mgiManager.addUser(inviteeId, mgiId, inviterId);
		mgiDao.activateInvitations(mgiId);
		if (gameType.isTournament()) {
			publicGameDao.createOrUpdate(Arrays.asList(APP_ID), new Invitation(gameConfig, inviterId));
		}
	}

	private void createPublicGame(String gameId, String creatorId, ZonedDateTime expiryDateTime) {
		MultiplayerGameParameters publicGameParams = CustomGameConfigBuilder
				.create(TENANT_ID, APP_ID, gameId, creatorId)
				.withGameType(GameType.DUEL)
				.withExpiryDateTime(expiryDateTime)
				.buildMultiplayerGameParameters();
		ClientInfo clientInfo = new ClientInfo(TENANT_ID, APP_ID, creatorId, "IP", null);
		String mgiId = mgiManager.createPublicGame(publicGameParams, clientInfo, null, null);
		mgiDao.registerForGame(mgiId, clientInfo, "any_game_instance_id");
	}
	
	@Test
	public void testUpdatePlayedQuiz() throws Exception {
		String requestJson = UpdatePlayedGameRequestBuilder.create(TENANT_ID)
				.withUserId(ANONYMOUS_USER_ID)
				.withPlayedGameInfo(GAME_ID_1, GameType.QUIZ24)
				.buildRequestJson();
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		
		RetriedAssert.assertWithWait(() -> assertNotNull(dashboardDao.getLastPlayedGame(TENANT_ID, ANONYMOUS_USER_ID, GameType.QUIZ24).getGameId()));
		PlayedGameInfo lastPlayedGame = dashboardDao.getLastPlayedGame(TENANT_ID, ANONYMOUS_USER_ID, GameType.QUIZ24);
		assertThat(lastPlayedGame.getGameId(), equalTo(GAME_ID_1));
		assertThat(lastPlayedGame.getType(), equalTo(GameType.QUIZ24));
	}
	
}
