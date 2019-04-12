package de.ascendro.f4m.service.game.selection.integration;

import static de.ascendro.f4m.service.game.selection.builder.GetGameListRequestBuilder.createGetGameListRequest;
import static de.ascendro.f4m.service.game.selection.integration.TestDataLoader.GAME_ID_0;
import static de.ascendro.f4m.service.game.selection.integration.TestDataLoader.GAME_ID_1;
import static de.ascendro.f4m.service.game.selection.integration.TestDataLoader.GAME_ID_2;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.inject.Injector;
import com.google.inject.Stage;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticsearchTestNode;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.auth.AuthMessageTypes;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypes;
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypes;
import de.ascendro.f4m.service.game.selection.GameSelectionMessageTypes;
import de.ascendro.f4m.service.game.selection.dao.GameSelectorAerospikeDaoImpl;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.GetGameListResponse;
import de.ascendro.f4m.service.game.selection.model.game.GetGameResponse;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerGameParameters;
import de.ascendro.f4m.service.game.selection.server.MultiplayerGameInstanceManager;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class GameSelectionTest extends GameSelectionServiceTestBase {
	private static final String MGI_ID1 = "10551c1e-a718-11e6-555-76304dec7eb1";
	private static final String MGI_ID2 = "20551c1e-a718-11e6-555-76304dec7eb2";
	private static final String MGI_ID3 = "30551c1e-a718-11e6-555-76304dec7eb3";
	
	private static final String TENANT_ID1 = "tenant_id_1";
	private static final String APP_ID1 = "app_id_1";
	private static final String APP_ID2 = "app_id_2";
	private static final String APP_ID3 = "app_id_3";
	
	private static final ZonedDateTime DATE_TIME_NOW = DateTimeUtil.getCurrentDateTime();
	
	private ClientInfo clientFromApp1;
	private ClientInfo clientFromApp2;
	private ClientInfo clientFromApp3;
	private ClientInfo clientInfoNullCountryCode;
	
	
	private static final int ELASTIC_PORT = 9211;
		
	private GameSelectorAerospikeDaoImpl aerospikeDao;
	private MultiplayerGameInstanceManager mgiManager;
	private Config config;
	private TestDataLoader testDataLoader;
	private JsonUtil jsonUtil;
	
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

		final Injector injector = jettyServerRule.getServerStartup().getInjector();
		aerospikeDao = injector.getInstance(GameSelectorAerospikeDaoImpl.class);
		mgiManager = injector.getInstance(MultiplayerGameInstanceManager.class);
		config = injector.getInstance(Config.class);
		jsonUtil = injector.getInstance(JsonUtil.class);
		
		final AerospikeClientProvider aerospikeClientProvider = injector.getInstance(AerospikeClientProvider.class);
		final JsonUtil jsonUtil = injector.getInstance(JsonUtil.class);
		final JsonMessageUtil jsonMessageUtil = injector.getInstance(JsonMessageUtil.class);
		testDataLoader = new TestDataLoader(config, aerospikeClientProvider, jsonUtil, jsonMessageUtil);

		testDataLoader.updateGameTestData(aerospikeDao);
		assertServiceStartup(FriendManagerMessageTypes.SERVICE_NAME, UserMessageMessageTypes.SERVICE_NAME,
				ProfileMessageTypes.SERVICE_NAME, EventMessageTypes.SERVICE_NAME, AuthMessageTypes.SERVICE_NAME,
				GameEngineMessageTypes.SERVICE_NAME, PaymentMessageTypes.SERVICE_NAME);
		
		clientFromApp1 = ClientInfo.cloneOf(ANONYMOUS_CLIENT_INFO);
		clientFromApp1.setAppId(APP_ID1);
		clientFromApp1.setTenantId(TENANT_ID1);
		
		clientFromApp2 = ClientInfo.cloneOf(ANONYMOUS_CLIENT_INFO);
		clientFromApp2.setAppId(APP_ID2);
		clientFromApp2.setTenantId(TENANT_ID1);
		
		clientFromApp3 = ClientInfo.cloneOf(ANONYMOUS_CLIENT_INFO);
		clientFromApp3.setAppId(APP_ID3);
		clientFromApp3.setTenantId(TENANT_ID1);
		
		clientInfoNullCountryCode = ClientInfo.cloneOf(ANONYMOUS_CLIENT_INFO);
		clientInfoNullCountryCode.setAppId(APP_ID2);
		clientInfoNullCountryCode.setTenantId(TENANT_ID1);
		clientInfoNullCountryCode.setCountryCode(null);
		clientInfoNullCountryCode.setOriginCountry(null);
		
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new GameSelectionStartupUsingAerospikeMock(Stage.PRODUCTION);
	}

	@Test
	public void testGetEmptyGameList() throws Exception {
		String requestJson = createGetGameListRequest()
				.withType(GameType.DUEL.name())
				.withHandicap(1.0, 3.0)
				.buildRequestJson(ANONYMOUS_CLIENT_INFO);
		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		JsonArray gameList = responseMessage.getContent().getGames();
		assertEquals(0, gameList.size());
	}

	@Test
	public void testGetGameListByMandatorySearchAttributes() throws Exception {
		String requestJson = createGetGameListRequest()
				.buildRequestJson(clientFromApp1);
		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		JsonArray gameList = responseMessage.getContent().getGames();

		assertGameListContainsGames(gameList, "game_id_0", "game_id_1", "game_id_2");
	}

	@Test
	public void testGetGameListByPool() throws Exception {
		String requestJson = createGetGameListRequest()
				.withPool("puzzle")
				.buildRequestJson(clientFromApp2);
		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		JsonArray gameList = responseMessage.getContent().getGames();
		assertGameListContainsGames(gameList, "game_id_0", "game_id_5", "game_id_8", "game_id_9");
	}

	@Test
	public void testGetGameListByMultipleStringFields() throws Exception {
		String requestJson = createGetGameListRequest()
				.withPool("puzzle")
				.withType(GameType.DUEL.name())
				.buildRequestJson(clientFromApp2);
		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		JsonArray gameList = responseMessage.getContent().getGames();
		assertGameListContainsGames(gameList, "game_id_5", "game_id_9");
	}

	@Test
	public void testGetGameListByHandicap() throws Exception {
		String requestJson = createGetGameListRequest()
				.withHandicap(1.5, 2.5)
				.buildRequestJson(clientFromApp2);
		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		JsonArray gameList = responseMessage.getContent().getGames();
		assertGameListContainsGames(gameList, "game_id_2", "game_id_5", "game_id_8", "game_id_9");
	}

	@Test
	public void testGetGameListByNumberOfQuestions() throws Exception {
		String requestJson = createGetGameListRequest()
				.withNumberOfQuestions(15, 20)
				.withType(GameType.DUEL.name())
				.buildRequestJson(clientFromApp2);
		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		
		JsonArray gameList = responseMessage.getContent().getGames();
		assertGameListContainsGames(gameList, "game_id_1", "game_id_7");
	}

	@Test
	public void testGetGameListWithJackpot() throws Exception {
		
		String requestJson = createGetGameListRequest()
				.withType(GameType.TOURNAMENT.name())
				.buildRequestJson(clientFromApp2);
		
		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		
		JsonArray gameList = responseMessage.getContent().getGames();
		
		assertGameListContainsGames(gameList, "game_id_0", "game_id_2", "game_id_7", "game_id_8");
		
		Game game = jsonUtil.fromJson(gameList.get(0), Game.class);
		Game game1 = jsonUtil.fromJson(gameList.get(1), Game.class);
		Game game2 = jsonUtil.fromJson(gameList.get(2), Game.class);
		Game game3 = jsonUtil.fromJson(gameList.get(3), Game.class);
		
		assertEquals("game_id_0", game2.getGameId());
		assertEquals(new BigDecimal("10"), game2.getJackpot().getBalance());
		assertEquals(Currency.CREDIT, game2.getJackpot().getCurrency());

		assertEquals("game_id_7", game3.getGameId());
		assertEquals(new BigDecimal("10"), game3.getJackpot().getBalance());
		assertEquals(Currency.CREDIT, game3.getJackpot().getCurrency());
		
		assertEquals("game_id_8", game.getGameId());
		assertEquals(null, game.getJackpot());
		assertEquals("game_id_2", game1.getGameId());
		assertEquals(null, game1.getJackpot());
		
	}
	
	public void testGetGameListNullCountrycode() throws Exception {
		ClientInfo cl= clientFromApp2;
		cl.setCountryCode(null);
		cl.setOriginCountry(null);
		
		String requestJson = createGetGameListRequest()
				.withNumberOfQuestions(15, 20)
				.withType(GameType.DUEL.name())
				.buildRequestJson(clientInfoNullCountryCode);
		
		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		JsonArray gameList = responseMessage.getContent().getGames();
		assertGameListContainsGames(gameList, "game_id_1", "game_id_7");
	}
	
	@Test
	public void testGetGameListByInvitations() throws Exception {
		final String userId1 = "user_id_1", userId2 = ANONYMOUS_USER_ID, userId3 = "user_id_3";

		final CustomGameConfigBuilder customGameConfig = CustomGameConfigBuilder.create(userId1)
				.withTenant(TENANT_ID1)
				.withApp(APP_ID1);
		
		testDataLoader.inviteFriends(MGI_ID1, userId1, customGameConfig.forGame("game_id_1"), 
				Arrays.asList(new ImmutablePair<>(userId2, userId1), new ImmutablePair<>(userId3, userId2))); //u1->u2->u3
		
		testDataLoader.inviteFriends(MGI_ID2, userId2, customGameConfig.forGame("game_id_2"), 
				Arrays.asList(new ImmutablePair<>(userId3, userId2))); //>u2->u3
		
		testDataLoader.inviteFriends(MGI_ID3, userId3, customGameConfig.forGame("game_id_3"), 
				Arrays.asList(new ImmutablePair<>(userId1, userId3)));//u3->u2

		String requestJson = createGetGameListRequest()
				.withByInvitation()
				.buildRequestJson(clientFromApp1);
		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		final JsonArray gameList = responseMessage.getContent().getGames();

		assertGameListContainsGames(gameList, "game_id_1", "game_id_2");
	}

	@Test
	public void testGetGameListCreatedByFriends() throws Exception {
		String requestJson = createGetGameListRequest()
				.withByFriendCreated()
				.buildRequestJson(clientFromApp2);
		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		JsonArray gameList = responseMessage.getContent().getGames();
		assertGameListContainsGames(gameList, "game_id_1", "game_id_2", "game_id_7");
	}

	@Test
	public void testGetGameListClientNullCountryCode() throws Exception {
		String requestJson = createGetGameListRequest()
				.withByFriendCreated()
				.buildRequestJson(clientInfoNullCountryCode);
		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		JsonArray gameList = responseMessage.getContent().getGames();
		assertGameListContainsGames(gameList, "game_id_1", "game_id_2", "game_id_7");
	}
	
	
	@Test
	public void testGetGameListPlayedByFriends() throws Exception {
		String requestJson = createGetGameListRequest()
				.withByFriendPlayed()
				.buildRequestJson(clientFromApp2);
		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		JsonArray gameList = responseMessage.getContent().getGames();
		assertGameListContainsGames(gameList, "game_id_1", "game_id_2", "game_id_3");
		assertNull(getGameById(gameList, "game_id_1").getAssignedPools());
		assertNotNull(getGameById(gameList, "game_id_2").getAssignedPools());
	}

	private Game getGameById(JsonArray games, String id) {
		List<Game> list = StreamSupport.stream(games.spliterator(), false)
			.map(e -> jsonUtil.fromJson(e, Game.class))
			.filter(g -> id.equals(g.getGameId()))
			.collect(Collectors.toList());
		assertThat(list, hasSize(1));
		return list.get(0);
	}

	@Test
	public void testGetGameListBySpecialPrize() throws Exception {
		String requestJson = createGetGameListRequest()
				.withHasSpecialPrize(true)
				.buildRequestJson(clientFromApp2);
		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		JsonArray gameList = responseMessage.getContent().getGames();
		assertGameListContainsGames(gameList, "game_id_0", "game_id_6", "game_id_7");
		assertEquals(TestDataLoader.TOURNAMENT_PLAYER_READINESS, jsonUtil.fromJson(gameList.get(0), Game.class).getPlayerGameReadiness());
		assertEquals(TestDataLoader.TOURNAMENT_EMAIL_NOTIFICATION, jsonUtil.fromJson(gameList.get(0), Game.class).getEmailNotification());
		assertEquals(TestDataLoader.DUEL_PLAYER_READINESS, jsonUtil.fromJson(gameList.get(2), Game.class).getPlayerGameReadiness());
		assertEquals(TestDataLoader.DUEL_EMAIL_NOTIFICATION, jsonUtil.fromJson(gameList.get(2), Game.class).getEmailNotification());
	}

	@Test
	public void testGetGameListByQuiz24SpecialGame() throws Exception {
		String requestJson = createGetGameListRequest()
				.withType(GameType.QUIZ24.name())
				.withIsSpecialGame(true)
				.buildRequestJson(clientFromApp3);
		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		JsonArray gameList = responseMessage.getContent().getGames();
		assertGameListContainsGames(gameList, "game_id_2");
		assertEquals(true, jsonUtil.fromJson(gameList.get(0), Game.class).isSpecial());
		assertEquals("banner-123", jsonUtil.fromJson(gameList.get(0), Game.class).getBannerMediaId());
		assertEquals(1.3, jsonUtil.fromJson(gameList.get(0), Game.class).getWeight().doubleValue(), 0.0);
	}

	@Test
	public void testGetGameListByQuiz24WithRealGames() throws Exception {
		ClientInfo customClientInfo = ClientInfo.cloneOf(ANONYMOUS_CLIENT_INFO);
		customClientInfo.setTenantId("3");
		customClientInfo.setAppId("6");
		String requestJson = createGetGameListRequest()
				.withType(GameType.QUIZ24.name())
				.withIsSpecialGame(false)
				.buildRequestJson(customClientInfo);
		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		JsonArray gameList = responseMessage.getContent().getGames();
		assertEquals(12, gameList.size());
		
		testClientReceivedMessageCollector.clearReceivedMessageList();
		
		String requestJson1 = createGetGameListRequest()
				.withType(GameType.QUIZ24.name())
				.withIsSpecialGame(null)
				.buildRequestJson(customClientInfo);
		JsonMessage<GetGameListResponse> responseMessage1 = sendGetGameListAndAssertResponse(requestJson1);
		JsonArray gameList1 = responseMessage1.getContent().getGames();
		assertEquals(13, gameList1.size());
		
		testClientReceivedMessageCollector.clearReceivedMessageList();
		
		String requestJson2 = createGetGameListRequest()
				.withType(GameType.QUIZ24.name())
				.withIsSpecialGame(true)
				.buildRequestJson(customClientInfo);
		JsonMessage<GetGameListResponse> responseMessage2 = sendGetGameListAndAssertResponse(requestJson2);
		JsonArray gameList2 = responseMessage2.getContent().getGames();
		assertEquals(1, gameList2.size());		
		assertEquals("23", jsonUtil.fromJson(gameList2.get(0), Game.class).getGameId());
	}

	@Test
	public void testGetGameListByQuiz24NonSpecialGame() throws Exception {
		String requestJson = createGetGameListRequest()
				.withType(GameType.QUIZ24.name())
				.withIsSpecialGame(false)
				.buildRequestJson(clientFromApp3);
		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		JsonArray gameList = responseMessage.getContent().getGames();
		assertGameListContainsGames(gameList, "game_id_3");
		assertEquals(false, jsonUtil.fromJson(gameList.get(0), Game.class).isSpecial());
		assertEquals(null, jsonUtil.fromJson(gameList.get(0), Game.class).getBannerMediaId());
		assertEquals(0.0, jsonUtil.fromJson(gameList.get(0), Game.class).getWeight().doubleValue(), 0.0);
	}

	@Test
	public void testGetGameListAllQuiz24() throws Exception {
		String requestJson = createGetGameListRequest()
				.withType(GameType.QUIZ24.name())
				.buildRequestJson(clientFromApp3);
		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		JsonArray gameList = responseMessage.getContent().getGames();
		assertGameListContainsGames(gameList, "game_id_2", "game_id_3");
		assertEquals(true, jsonUtil.fromJson(gameList.get(0), Game.class).isSpecial());
		assertEquals(false, jsonUtil.fromJson(gameList.get(1), Game.class).isSpecial());
	}
	
	@Test
	public void testGetGameListByFullText() throws Exception {
		String requestJson = createGetGameListRequest()
				.withFullText("cB")
				.buildRequestJson(clientFromApp2);
		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		JsonArray gameList = responseMessage.getContent().getGames();
		assertGameListContainsGames(gameList, "game_id_3", "game_id_5", "game_id_6", "game_id_7", "game_id_8", "game_id_9");
	}

	/**
	 * 	 check if game is removed if not from  playable region.
	 * */
	@Test
	public void testGetGameListWithPlayingRegions() throws Exception {
		
		ClientInfo clientCantPlayGame = ClientInfo.cloneOf(ANONYMOUS_CLIENT_INFO);
		clientCantPlayGame.setAppId(APP_ID2);
		clientCantPlayGame.setTenantId(TENANT_ID1);
		clientCantPlayGame.setCountryCode(ISOCountry.LT);
		
		String requestJson = createGetGameListRequest()
				.withNumberOfQuestions(15, 20)
				.withType(GameType.DUEL.name())
				.buildRequestJson(clientCantPlayGame);

		JsonMessage<GetGameListResponse> responseMessage = sendGetGameListAndAssertResponse(requestJson);
		JsonArray gameList = responseMessage.getContent().getGames();
		assertGameListContainsGames(gameList, "game_id_1");
	}

	
	private JsonMessage<GetGameListResponse> sendGetGameListAndAssertResponse(String requestJson) throws Exception {
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		assertReceivedMessagesWithWait(GameSelectionMessageTypes.GET_GAME_LIST_RESPONSE);
		final JsonMessage<GetGameListResponse> gameListResponseMessage = testClientReceivedMessageCollector
				.getMessageByType(GameSelectionMessageTypes.GET_GAME_LIST_RESPONSE);
		assertMessageContentType(gameListResponseMessage, GetGameListResponse.class);

		return gameListResponseMessage;
	}

	private void assertGameListContainsGames(JsonArray games, String...expectedGameIds) {
		assertEquals(expectedGameIds.length, games.size());
		
		final Set<String> actualGameIds = StreamSupport.stream(games.spliterator(), false)
			.map(e -> jsonUtil.fromJson(e, Game.class).getGameId())
			.collect(Collectors.toSet());
		
		assertThat(actualGameIds, hasItems(expectedGameIds));
	}

	@Test
	public void testGetGame() throws Exception {
		final String gameRequestJson = getPlainTextJsonFromResources("getGameRequest.json", ANONYMOUS_CLIENT_INFO)
				.replaceFirst("<<gameId>>", GAME_ID_1);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), gameRequestJson);

		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> responseMessage = testClientReceivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(responseMessage, GetGameResponse.class);

		Game game = ((GetGameResponse) responseMessage.getContent()).getGame();
		assertEquals(GAME_ID_1, game.getGameId());
		assertNull(game.getMultiplayerGameInstanceId());
		assertEquals(TestDataLoader.DUEL_PLAYER_READINESS, game.getPlayerGameReadiness());
		assertEquals(TestDataLoader.DUEL_EMAIL_NOTIFICATION, game.getEmailNotification());
		assertNull(game.getAssignedPools());
		assertFalse(game.isSpecial());
		assertTrue(game.isMultiplePurchaseAllowed());
		assertEquals(1, game.getEntryFeeBatchSize().intValue());
	}

	@Test
	public void testGetGameSpecial() throws Exception {
		final String gameRequestJson = getPlainTextJsonFromResources("getGameRequest.json", ANONYMOUS_CLIENT_INFO)
				.replaceFirst("<<gameId>>", GAME_ID_2);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), gameRequestJson);

		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> responseMessage = testClientReceivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(responseMessage, GetGameResponse.class);

		Game game = ((GetGameResponse) responseMessage.getContent()).getGame();
		assertEquals(GAME_ID_2, game.getGameId());
		assertNull(game.getMultiplayerGameInstanceId());
		assertTrue(game.isSpecial());
		assertFalse(game.isMultiplePurchaseAllowed());
		assertEquals(4, game.getEntryFeeBatchSize().intValue());
	}
	
	@Test
	public void testGetGameWithMGI() throws Exception {
		final String mgiId = prepareTournament(GAME_ID_0);
		final String gameRequestJson = getPlainTextJsonFromResources("getGameRequest.json", ANONYMOUS_CLIENT_INFO)
				.replaceFirst("<<gameId>>", GAME_ID_0);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), gameRequestJson);

		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> responseMessage = testClientReceivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(responseMessage, GetGameResponse.class);

		Game game = ((GetGameResponse) responseMessage.getContent()).getGame();
		assertThat(game.getGameId(), equalTo(GAME_ID_0));
		assertThat(game.getMultiplayerGameInstanceId(), equalTo(mgiId));
		assertThat(jsonUtil.toJson(game.getLiveTournamentDateTime()), equalTo(jsonUtil.toJson(DATE_TIME_NOW)));
		assertEquals(1, game.getEntryFeeBatchSize().intValue());
		assertEquals(true, game.isMultiplePurchaseAllowed());
		assertNotNull(game.getAssignedPools());
	}

	@Test
	public void testGetGameWithJackpot() throws Exception {
		final String mgiId = prepareTournament(GAME_ID_0);
		final String gameRequestJson = getPlainTextJsonFromResources("getGameRequest.json", ANONYMOUS_CLIENT_INFO)
				.replaceFirst("<<gameId>>", GAME_ID_0);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), gameRequestJson);

		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> responseMessage = testClientReceivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(responseMessage, GetGameResponse.class);

		Game game = ((GetGameResponse) responseMessage.getContent()).getGame();
		assertThat(game.getGameId(), equalTo(GAME_ID_0));
		assertThat(game.getMultiplayerGameInstanceId(), equalTo(mgiId));
		assertThat(game.getJackpot().getBalance(), equalTo(new BigDecimal("10")));
		
	}
	
	
	@Test
	public void testGetNotExistingGame() throws Exception {
		final String gameRequestJson = getPlainTextJsonFromResources("getGameRequest.json", ANONYMOUS_CLIENT_INFO)
				.replaceFirst("<<gameId>>", "not_existing_game_id");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), gameRequestJson);

		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> responseMessage = testClientReceivedMessageCollector
				.getReceivedMessageList().get(0);
		assertMessageContentType(responseMessage, GetGameResponse.class);

		Game game = ((GetGameResponse) responseMessage.getContent()).getGame();
		assertNull(game.getGameId());
	}
	
	private String prepareTournament(String gameId) {
		MultiplayerGameParameters params = new MultiplayerGameParameters(gameId);
		params.setPlayDateTime(DATE_TIME_NOW);
		ClientInfo clientInfo = new ClientInfo();
		clientInfo.setAppId(APP_ID1);
		clientInfo.setTenantId(TENANT_ID1);
		String mgiId = mgiManager.createMultiplayerGameInstance(params, clientInfo, null, null, false).getId();
		mgiManager.mapTournament(gameId, mgiId);
		return mgiId;
	}

}
