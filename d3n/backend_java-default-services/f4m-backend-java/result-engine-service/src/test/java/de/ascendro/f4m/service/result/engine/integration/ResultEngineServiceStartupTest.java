package de.ascendro.f4m.service.result.engine.integration;

import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.REGISTERED_CLIENT_INFO;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.TENANT_ID;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonElement;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;

import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticsearchTestNode;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.game.util.GameEnginePrimaryKeyUtil;
import de.ascendro.f4m.server.history.dao.CommonGameHistoryDao;
import de.ascendro.f4m.server.history.dao.CommonGameHistoryDaoImpl;
import de.ascendro.f4m.server.history.dao.GameHistoryPrimaryKeyUtil;
import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.result.ResultType;
import de.ascendro.f4m.server.result.Results;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.exception.ExceptionCodes;
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypes;
import de.ascendro.f4m.service.game.engine.model.GameHistory;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder;
import de.ascendro.f4m.service.game.selection.model.multiplayer.InvitationGame;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.collector.ReceivedMessageCollector;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.BindableAbstractModule;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.ThrowingFunction;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.json.model.user.UserPermission;
import de.ascendro.f4m.service.json.model.user.UserRole;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.TransactionLog;
import de.ascendro.f4m.service.payment.model.TransactionStatus;
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotRequest.PayoutItem;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceRequest;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceResponse;
import de.ascendro.f4m.service.payment.model.internal.GetJackpotResponse;
import de.ascendro.f4m.service.payment.model.internal.LoadOrWithdrawWithoutCoverageRequest;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.api.ApiProfileBasicInfo;
import de.ascendro.f4m.service.profile.model.update.UpdateProfileRequest;
import de.ascendro.f4m.service.profile.model.update.UpdateProfileResponse;
import de.ascendro.f4m.service.result.engine.config.ResultEngineConfig;
import de.ascendro.f4m.service.result.engine.dao.CompletedGameHistoryAerospikeDao;
import de.ascendro.f4m.service.result.engine.dao.ResultEngineAerospikeDao;
import de.ascendro.f4m.service.result.engine.di.ResultEngineDefaultMessageMapper;
import de.ascendro.f4m.service.result.engine.exception.ResultEngineExceptionCodes;
import de.ascendro.f4m.service.result.engine.model.ApiProfileWithResults;
import de.ascendro.f4m.service.result.engine.model.CompletedGameHistoryInfo;
import de.ascendro.f4m.service.result.engine.model.GameHistoryUpdateKind;
import de.ascendro.f4m.service.result.engine.model.calculate.CalculateResultsRequest;
import de.ascendro.f4m.service.result.engine.model.calculate.CalculateResultsResponse;
import de.ascendro.f4m.service.result.engine.model.get.CompletedGameListResponse;
import de.ascendro.f4m.service.result.engine.model.get.GetMultiplayerResultsResponse;
import de.ascendro.f4m.service.result.engine.model.get.GetResultsResponse;
import de.ascendro.f4m.service.result.engine.model.multiplayerGameResult.MultiplayerResultsBuddiesListResponse;
import de.ascendro.f4m.service.result.engine.model.multiplayerGameResult.MultiplayerResultsGlobalListResponse;
import de.ascendro.f4m.service.result.engine.model.multiplayerGameResult.MultiplayerResultsStatus;
import de.ascendro.f4m.service.result.engine.model.notification.GameEndNotification;
import de.ascendro.f4m.service.result.engine.model.schema.ResultEngineMessageSchemaMapper;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageRequest;
import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherAssignRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherAssignResponse;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherReleaseRequest;
import de.ascendro.f4m.service.winning.model.UserWinning;
import de.ascendro.f4m.service.winning.model.UserWinningType;

public class ResultEngineServiceStartupTest extends F4MServiceWithMockIntegrationTestBase {

	private static final int ELASTIC_PORT = 9214;
	public static final Integer DUEL_PLAYER_READINESS = 30;
	public static final Boolean DUEL_EMAIL_NOTIFICATION = false;

	private static final String GAME_INSTANCE_ID1 = "gi1";
	private static final String GAME_INSTANCE_ID2 = "gi2";
	private static final String GAME_ID1 = "1";
	private static final String MGI_ID = "mpgi1";

	private static final String TEST_USER_ID = "test_user";
	private static final String DEFAULT_SPECIAL_PRIZE_CORRECT_ANSWERS_PERCENT = "60";
	private static final String SPECIAL_PRIZE_CORRECT_ANSWERS_PERCENT_FOR_VOUCHER_RELEASE = "99";
	public static final double PAYDENT_BONUS_POINTS = 5.56;

	private final Config config = new ResultEngineConfig();
	private final GameHistoryPrimaryKeyUtil gameHistoryPrimaryKeyUtil = new GameHistoryPrimaryKeyUtil(config);

	@BeforeClass
	public static void setUpClass() {
		F4MServiceWithMockIntegrationTestBase.setUpClass();
		System.setProperty(ElasticConfigImpl.ELASTIC_SERVER_HOSTS, "http://localhost:" + ELASTIC_PORT);
		System.setProperty(ElasticConfigImpl.ELASTIC_HEADER_REFRESH, ElasticConfigImpl.ELASTIC_HEADER_VALUE_WAIT_FOR);
	}

	@Rule
	public ElasticsearchTestNode elastic = new ElasticsearchTestNode(ELASTIC_PORT);

	@Override
	protected void configureInjectionModuleMock(BindableAbstractModule module) {
		module.bindToModule(JsonMessageTypeMap.class).to(ResultEngineDefaultMessageMapper.class);
		module.bindToModule(JsonMessageSchemaMap.class).to(ResultEngineMessageSchemaMapper.class);
	}

	@Override
	protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandlerMock() {
		return ctx -> onReceivedMessage(ctx);
	}

	private ReceivedMessageCollector mockServiceReceivedMessageCollector = new ReceivedMessageCollector();

	private TransactionLogAerospikeDao transactionLogAerospikeDao;
	
	private CommonUserWinningAerospikeDao userWinningDao;
	
	private ResultEngineAerospikeDao resultEngineDao;

	private CompletedGameHistoryAerospikeDao completedGameHistoryAerospikeDao;
	private CommonGameHistoryDaoImpl commonGameHistoryDao;

	private GameEnginePrimaryKeyUtil gameEnginePrimaryKeyUtil;
	private CommonMultiplayerGameInstanceDao multiplayerGameInstanceDao;
	private CommonGameInstanceAerospikeDao gameInstanceDao;

	private CommonProfileAerospikeDao commonProfileAerospikeDao;

	private JsonMessageUtil jsonMessageUtil;
	private JsonUtil jsonUtil;

	@Override
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		super.setUp();
		
		Injector injector = jettyServerRule.getServerStartup().getInjector();
		resultEngineDao = injector.getInstance(ResultEngineAerospikeDao.class);
		completedGameHistoryAerospikeDao = injector.getInstance(CompletedGameHistoryAerospikeDao.class);
		commonGameHistoryDao = (CommonGameHistoryDaoImpl) injector.getInstance(CommonGameHistoryDao.class);
		multiplayerGameInstanceDao = injector.getInstance(CommonMultiplayerGameInstanceDao.class);
		gameInstanceDao = injector.getInstance(CommonGameInstanceAerospikeDao.class);

		Config config = new F4MConfigImpl();
		config.setProperty(F4MConfigImpl.SERVICE_NAME, GameEngineMessageTypes.SERVICE_NAME);
		gameEnginePrimaryKeyUtil = new GameEnginePrimaryKeyUtil(config);

		jsonMessageUtil = clientInjector.getInstance(JsonMessageUtil.class);
		jsonUtil = clientInjector.getInstance(JsonUtil.class);

		assertServiceStartup(ProfileMessageTypes.SERVICE_NAME, UserMessageMessageTypes.SERVICE_NAME,
				VoucherMessageTypes.SERVICE_NAME, PaymentMessageTypes.SERVICE_NAME);
		
		mockServiceReceivedMessageCollector.setSessionWrapper(mock(SessionWrapper.class));
		prepareMgiConfig(MGI_ID, TEST_USER_ID);
	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(ResultEngineDefaultMessageMapper.class, ResultEngineMessageSchemaMapper.class);
	}

	private int transactionLogUpdates;
	
	private Map<String, List<UserWinning>> userWinningsSaved = new HashMap<>(2);
	
	@Override
	protected ServiceStartup getServiceStartup() {
		return new ResultEngineServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE) {
			@Override
		    protected AbstractModule getModule() {
		        return new ResultEngineMockModule() {
		            @Override
		            protected void configure() {
						transactionLogAerospikeDao = mock(TransactionLogAerospikeDao.class);
						bind(TransactionLogAerospikeDao.class).toInstance(transactionLogAerospikeDao);
						when(transactionLogAerospikeDao.createTransactionLog(any(TransactionLog.class))).thenReturn("tid");
						doAnswer(invocation -> {
                            transactionLogUpdates++;
                            return null;
                        }).when(transactionLogAerospikeDao).updateTransactionLog("tid", "transId", TransactionStatus.COMPLETED);

						userWinningDao = mock(CommonUserWinningAerospikeDao.class);
						bind(CommonUserWinningAerospikeDao.class).toInstance(userWinningDao);
						doAnswer(invocation -> {
                            String userId = invocation.getArgument(1);
                            List<UserWinning> list = userWinningsSaved.get(userId);
                            if (list == null) {
                                list = new ArrayList<>(2);
                                userWinningsSaved.put(userId, list);
                            }
                            list.add(invocation.getArgument(2));
                            return null;
                        }).when(userWinningDao).saveUserWinning(anyString(), anyString(), any(UserWinning.class));

						commonProfileAerospikeDao = mock(CommonProfileAerospikeDao.class);
						bind(CommonProfileAerospikeDao.class).toInstance(commonProfileAerospikeDao);
						Profile profile =  new Profile();
						profile.addTenant(TENANT_ID);
                        profile.setArray(Profile.ROLES_PROPERTY, ANONYMOUS_CLIENT_INFO.getRoles());
						when(commonProfileAerospikeDao.getProfile(any())).thenReturn(profile);
						when(commonProfileAerospikeDao.getProfileBasicInfo(TEST_USER_ID)).thenReturn(new ApiProfileBasicInfo(TEST_USER_ID));
						when(commonProfileAerospikeDao.getProfiles(any())).thenAnswer(invocation -> {
							List<String> userIds = invocation.getArgument(0);
							return userIds.stream().map(userId -> {
								Profile p =  new Profile();
								p.addTenant(TENANT_ID);
		                        p.setArray(Profile.ROLES_PROPERTY, ANONYMOUS_CLIENT_INFO.getRoles());
		                        p.setUserId(userId);
								return p;
							}).collect(Collectors.toList());
						});
						bindAdditional(this);
		            }
		        };
		    }
			@Override
			protected void bindAdditional(ResultEngineMockModule module) {
				module.bindToModule(JsonMessageSchemaMap.class).toInstance(getResultEngineMessageSchemaMapperMock());
			}
		};
	}

	private ResultEngineMessageSchemaMapper getResultEngineMessageSchemaMapperMock() {
		return new ResultEngineMessageSchemaMapper() {
			private static final long serialVersionUID = 7475113988053116392L;

			@Override
			protected Map<String, Set<String>> loadRolePermissions() {
				final Map<String, Set<String>> rolePermissions = new HashMap<>();

				final Set<String> tenantReadPermission = new HashSet<>(Arrays.asList(UserPermission.TENANT_DATA_READ.name()));
				rolePermissions.put(UserRole.REGISTERED.name(), tenantReadPermission);

				return rolePermissions;
			}
		};
	}
	
	private JsonMessageContent onReceivedMessage(RequestContext context) throws Exception {
		JsonMessage<? extends JsonMessageContent> originalMessageDecoded = context.getMessage();
		mockServiceReceivedMessageCollector.onProcess(context);
		if (originalMessageDecoded.getType(ProfileMessageTypes.class) == ProfileMessageTypes.UPDATE_PROFILE) {
			return new UpdateProfileResponse(new Profile().getJsonObject());
		} else if (originalMessageDecoded.getType(VoucherMessageTypes.class) == VoucherMessageTypes.USER_VOUCHER_ASSIGN ) {
			UserVoucherAssignRequest request = (UserVoucherAssignRequest) originalMessageDecoded.getContent();
			return new UserVoucherAssignResponse(request.getVoucherId());
		} else if (originalMessageDecoded.getType(VoucherMessageTypes.class) == VoucherMessageTypes.USER_VOUCHER_RELEASE ) {
			return new EmptyJsonMessageContent();
		} else if (originalMessageDecoded.getType(PaymentMessageTypes.class) == PaymentMessageTypes.TRANSFER_BETWEEN_ACCOUNTS) {
			return new TransactionId("transId");
		} else if (originalMessageDecoded.getType(PaymentMessageTypes.class) == PaymentMessageTypes.LOAD_OR_WITHDRAW_WITHOUT_COVERAGE) {
			return new TransactionId("transId");
		} else if (PaymentMessageTypes.GET_JACKPOT == originalMessageDecoded.getType(PaymentMessageTypes.class)) {
			GetJackpotResponse jack = new GetJackpotResponse();
			jack.setBalance(new BigDecimal("10"));
			jack.setCurrency(Currency.CREDIT);
			return jack;
		} else if (originalMessageDecoded.getType(PaymentMessageTypes.class) == PaymentMessageTypes.GET_ACCOUNT_BALANCE) {
			GetAccountBalanceRequest request = (GetAccountBalanceRequest) originalMessageDecoded.getContent();
			GetAccountBalanceResponse response = new GetAccountBalanceResponse();
			response.setAmount(new BigDecimal("5.56"));
			response.setCurrency(request.getCurrency());
			return response;
		} else {
			return null;
		}
	}
	
	@Test
	public void testCalculateResults_GameNotCompleted() throws Exception {
		String requestJson = getCalculateResultRequest(false)
				.replaceAll("\"status\": \"COMPLETED\",", ""); // remove game completed status
		final JsonMessage<? extends JsonMessageContent> response = sendRequestAndWaitForResults(requestJson);
		assertEquals(ExceptionCodes.ERR_VALIDATION_FAILED, response.getError().getCode());
	}
	
	@Test
	public void testCalculateResults_GameCancelled() throws Exception {
		String mpgiId = prepareMultiplayerGameInstance(getCalculateResultRequest(false), TEST_USER_ID);
		final String requestCancelledWithAnswersJson = getCalculateResultRequest(false)
				.replaceAll("mpgi1", mpgiId)
				.replaceAll("\"status\": \"COMPLETED\"", "\"status\": \"CANCELLED\""); //set game cancelled
		assertMessageContentType(sendRequestAndWaitForResults(requestCancelledWithAnswersJson),
				CalculateResultsResponse.class);
		
		final String requestCancelledWithMissingAnswersJson =requestCancelledWithAnswersJson.replaceAll("\"numberOfQuestions\": 3", "\"numberOfQuestions\": 4");
		final JsonMessage<? extends JsonMessageContent> response = sendRequestAndWaitForResults(requestCancelledWithMissingAnswersJson);
		assertEquals(ExceptionCodes.ERR_VALIDATION_FAILED, response.getError().getCode());
	}

	@Test
	public void testCalculateAndGetResults() throws Exception {
		// No results should result in error
		JsonMessage<? extends JsonMessageContent> response = sendRequestAndWaitForResults(getGetResultRequest());
		assertEquals(ExceptionCodes.ERR_ENTRY_NOT_FOUND, response.getError().getCode());

		// First calculation should be successful
		mockServiceReceivedMessageCollector.clearReceivedMessageList();
		String mpgiId = prepareMultiplayerGameInstance(getCalculateResultRequest(false), TEST_USER_ID);
		String requestJson = getCalculateResultRequest(false).replaceAll("mpgi1", mpgiId);
		response = sendRequestAndWaitForResults(requestJson);
		assertMessageContentType(response, CalculateResultsResponse.class);

		response = sendRequestAndWaitForResults(getGetResultRequest());
		assertMessageContentType(response, GetResultsResponse.class);
		GetResultsResponse getResultsResponse = (GetResultsResponse) response.getContent();
		Results results = new Results(getResultsResponse.getResults());
		assertEquals(2, results.getCorrectAnswerCount());
		assertEquals(3, results.getAnswerResults().size());
		assertEquals("1", results.getSpecialPrizeVoucherId());
		assertEquals(200 + PAYDENT_BONUS_POINTS, results.getResultItems().get(ResultType.TOTAL_BONUS_POINTS).getAmount(), 0);
		assertNotNull(results.getExpiryDateTime());

		assertEquals(4, mockServiceReceivedMessageCollector.getReceivedMessageList().size());
		assertLoadOrWithdrawRequest();
		assertVoucherAssignRequest();
		assertHandicapUpdate();
		assertBonusPointsRequest();
		
		// Second calculation should fail => may not overwrite results
		response = sendRequestAndWaitForResults(requestJson);
		assertEquals(ExceptionCodes.ERR_ENTRY_ALREADY_EXISTS, response.getError().getCode());
		
		// Force recalculate should succeed
		requestJson = getCalculateResultRequest(true) // add winning component requiring interaction
				.replaceAll("mpgi1", mpgiId)
				.replaceAll("\"gameInstance\"", "\"forceRecalculate\":true,\"gameInstance\""); // add forceRecalculate
		response = sendRequestAndWaitForResults(requestJson);
		assertMessageContentType(response, CalculateResultsResponse.class);

		// Now get results should fail since user has not responded to interaction
		response = sendRequestAndWaitForResults(getGetResultRequest());
		assertEquals(ResultEngineExceptionCodes.ERR_WINNING_COMPONENT_PURCHASE_PENDING, response.getError().getCode());
		
		// Respond to user interaction
		response = sendRequestAndWaitForResults(getRespondToPayForWinningRequest());
		assertMessageContentType(response, EmptyJsonMessageContent.class);
		
		// Now get results should succeed
		response = sendRequestAndWaitForResults(getGetResultRequest());
		assertMessageContentType(response, GetResultsResponse.class);
		getResultsResponse = (GetResultsResponse) response.getContent();
		results = new Results(getResultsResponse.getResults());
		assertEquals(2, results.getCorrectAnswerCount());
		assertEquals(3, results.getAnswerResults().size());

		// Total bonus points should be set in results
		assertEquals(PAYDENT_BONUS_POINTS, results.getResultItems().get(ResultType.TOTAL_BONUS_POINTS).getAmount(), 0);

		ZonedDateTime dateTimeFrom = DateTimeUtil.parseISODateTimeString("2016-01-11T00:00:00Z");
		ZonedDateTime dateTimeTo = DateTimeUtil.parseISODateTimeString("2016-01-11T23:59:59Z");
        // Game history info should be available
        List<CompletedGameHistoryInfo> completedGameHistoryInfoList =
				completedGameHistoryAerospikeDao.getCompletedGamesList(results.getUserId(), results.getTenantId(),
						GameType.DUEL, 0, 10, dateTimeFrom, dateTimeTo).getCompletedGameList();
        assertEquals(1, completedGameHistoryInfoList.size());
        CompletedGameHistoryInfo completedGameHistoryInfo = completedGameHistoryInfoList.get(0);

        Long totalTimeInSeconds = DateTimeUtil.getSecondsBetween(
        		DateTimeUtil.parseISODateTimeString("2016-01-10T00:00:00Z"),
				DateTimeUtil.parseISODateTimeString("2016-01-11T01:01:01Z"));

		CompletedGameHistoryInfo expectedGameHistoryInfo = new CompletedGameHistoryInfo();
		expectedGameHistoryInfo.setUserId(TEST_USER_ID);
		expectedGameHistoryInfo.setTenantId(TENANT_ID);
		expectedGameHistoryInfo.setGame(new InvitationGame(prepareGame(GameType.DUEL)));
		expectedGameHistoryInfo.setGameInstanceId(GAME_INSTANCE_ID1);
		expectedGameHistoryInfo.setEndDateTime(DateTimeUtil.parseISODateTimeString("2016-01-11T01:01:01Z"));
		expectedGameHistoryInfo.setNumberOfQuestions(3);
		expectedGameHistoryInfo.setNumberOfCorrectQuestions(2);
		expectedGameHistoryInfo.setTotalTime(totalTimeInSeconds);
		expectedGameHistoryInfo.setMultiplayerGameFinished(false);
		expectedGameHistoryInfo.setEmailNotification(DUEL_EMAIL_NOTIFICATION);
		expectedGameHistoryInfo.setPlayerGameReadiness(DUEL_PLAYER_READINESS);

		assertGameHistoryInfo(expectedGameHistoryInfo, completedGameHistoryInfo);
	}

	/**
	 * if user comes from country where is no gambling, no winning component should be shown
	 * */
	@Test
	public void testCalculateResultsNoGamblingCountry() throws Exception {
		mockServiceReceivedMessageCollector.clearReceivedMessageList();
		ClientInfo clientInfo=ANONYMOUS_CLIENT_INFO;
		clientInfo.setCountryCode(ISOCountry.valueOf("SA"));
		clientInfo.setUserId(TEST_USER_ID);
		
		String requestJson = getCalculateResultRequest(false, clientInfo)
				.replace("\"<<playerReadinessDuel>>\"", DUEL_PLAYER_READINESS+"");

		//copied from prepareMultiplayerGameInstance
		String userId=TEST_USER_ID;
		CustomGameConfig customGameConfig = CustomGameConfigBuilder.create("tenantId", "appId", "gameId", userId)
				.withPlayingRegions("region")
				.withPools("poolIds")
				.withNumberOfQuestions(3)
				.withMaxNumberOfParticipants(2)
				.withEntryFee(BigDecimal.valueOf(5.5), Currency.MONEY)
				.withExpiryDateTime(DateTimeUtil.getCurrentDateTime().plusSeconds(5))
				.withStartDateTime(DateTimeUtil.getCurrentDateTime().minusSeconds(10))
				.withEndDateTime(DateTimeUtil.getCurrentDateTime().plusSeconds(10))
				.build();
		String mpgiId = multiplayerGameInstanceDao.create(userId, customGameConfig);
		GameInstance gameInstance = prepareGameInstance(requestJson, mpgiId);
		multiplayerGameInstanceDao.registerForGame(mpgiId, clientInfo, gameInstance.getId());
		
		requestJson = getCalculateResultRequest(true, clientInfo) // add winning component requiring interaction
				.replace("\"<<playerReadinessDuel>>\"", DUEL_PLAYER_READINESS+"")
				.replaceAll("mpgi1", mpgiId);
		JsonMessage<? extends JsonMessageContent> response = sendRequestAndWaitForResults(requestJson);
		assertMessageContentType(response, CalculateResultsResponse.class);

		// Now get results should fail since user has not responded to interaction
		response = sendRequestAndWaitForResults(getGetResultRequest());
		GetResultsResponse resp= (GetResultsResponse) response.getContent();
		JsonElement result= resp.getResults().get("eligibleToWinnings");
		assertEquals(false, result.getAsBoolean() );	
	}
	
	
	
	private void assertVoucherAssignRequest() {
		UserVoucherAssignRequest addVoucherRequest = mockServiceReceivedMessageCollector
				.<UserVoucherAssignRequest>getMessageByType(VoucherMessageTypes.USER_VOUCHER_ASSIGN)
				.getContent();
		assertEquals(TEST_USER_ID, addVoucherRequest.getUserId());
		assertEquals("1", addVoucherRequest.getVoucherId());
	}

	private void assertLoadOrWithdrawRequest() {
		LoadOrWithdrawWithoutCoverageRequest transferRequest = mockServiceReceivedMessageCollector
				.<LoadOrWithdrawWithoutCoverageRequest>getMessageByType(PaymentMessageTypes.LOAD_OR_WITHDRAW_WITHOUT_COVERAGE)
				.getContent();
		assertEquals(BigDecimal.valueOf(200), transferRequest.getAmount());
		assertEquals(Currency.BONUS, transferRequest.getCurrency());
	}

	@Test
	public void testCalculateAndGetWithVoucheReleaseResults() throws Exception {
		String mpgiId = prepareMultiplayerGameInstance(getCalculateResultRequest(false), TEST_USER_ID);
		String calculateResultsRequest = getCalculateResultRequest(SPECIAL_PRIZE_CORRECT_ANSWERS_PERCENT_FOR_VOUCHER_RELEASE, false, null)
				.replaceAll("mpgi1", mpgiId);
		
		// No results should result in error
		JsonMessage<? extends JsonMessageContent> response = sendRequestAndWaitForResults(getGetResultRequest());
		assertEquals(ExceptionCodes.ERR_ENTRY_NOT_FOUND, response.getError().getCode());

		// First calculation should be successful
		mockServiceReceivedMessageCollector.clearReceivedMessageList();
		response = sendRequestAndWaitForResults(calculateResultsRequest);
		assertMessageContentType(response, CalculateResultsResponse.class);

		response = sendRequestAndWaitForResults(getGetResultRequest());
		assertMessageContentType(response, GetResultsResponse.class);
		GetResultsResponse getResultsResponse = (GetResultsResponse) response.getContent();
		Results results = new Results(getResultsResponse.getResults());
		assertEquals(2, results.getCorrectAnswerCount());
		assertEquals(3, results.getAnswerResults().size());

		// Voucher should be assigned to profile
		assertEquals(4, mockServiceReceivedMessageCollector.getReceivedMessageList().size());
		assertLoadOrWithdrawRequest();
		assertVoucherReleaseRequest();
		assertHandicapUpdate();
		assertBonusPointsRequest();
	}

	private void assertVoucherReleaseRequest() {
		UserVoucherReleaseRequest releaseVouherRequest = mockServiceReceivedMessageCollector
				.<UserVoucherReleaseRequest>getMessageByType(VoucherMessageTypes.USER_VOUCHER_RELEASE)
				.getContent();
		assertEquals("1", releaseVouherRequest.getVoucherId());
	}

	private void assertBonusPointsRequest() {
		// Total bonus points should have been requested
		GetAccountBalanceRequest balanceRequest = mockServiceReceivedMessageCollector
				.<GetAccountBalanceRequest>getMessageByType(PaymentMessageTypes.GET_ACCOUNT_BALANCE).getContent();
		assertEquals(TEST_USER_ID, balanceRequest.getProfileId());
	}

	private void assertHandicapUpdate() {
		// Profile should be updated with new handicap
		UpdateProfileRequest updateProfileRequest = mockServiceReceivedMessageCollector
				.<UpdateProfileRequest>getMessageByType(ProfileMessageTypes.UPDATE_PROFILE)
				.getContent();
		Profile profile = new Profile(updateProfileRequest.getProfile());
		assertEquals(6.4, profile.getHandicap(), 0);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCalculateAndGetMultiplayerResults() throws Exception {
		String mpgiId = prepareMultiplayerGameInstance(getCalculateResultRequest(false), TEST_USER_ID);
		registerUsers(mpgiId, TEST_USER_ID, Pair.of("test_user2", getCalculateResultRequest(false).replaceAll("\"gi1\"", "\"gi2\"")));

		// Calculate results
		String calculateResultsRequest = getCalculateResultRequest(false).replaceAll("mpgi1", mpgiId);
		JsonMessage<? extends JsonMessageContent> response = sendRequestAndWaitForResults(calculateResultsRequest);
		assertMessageContentType(response, CalculateResultsResponse.class);

		// No results should result in error
		String mpResultRequest = getGetMultiplayerResultRequest();
		response = sendRequestAndWaitForResults(mpResultRequest);
		assertEquals(ResultEngineExceptionCodes.ERR_RESULTS_NOT_RELEASED, response.getError().getCode());
		
		// First calculation should be successful
		mockServiceReceivedMessageCollector.clearReceivedMessageList();
		String requestJson = getCalculateMultiplayerResultRequest().replaceAll("mpgi1", mpgiId);
		response = sendRequestAndWaitForResults(requestJson);
		assertMessageContentType(response, EmptyJsonMessageContent.class);
		
		// Game-end notifications have to be sent to all users
		assertGameEndNotifications(mpgiId, GameType.DUEL, new String[] { GAME_INSTANCE_ID1, GAME_INSTANCE_ID2 },
				new String[] {TEST_USER_ID, "test_user2" });

		// Check the results
		response = sendRequestAndWaitForResults(mpResultRequest);
		GetMultiplayerResultsResponse getMultiplayerResultsResponse = (GetMultiplayerResultsResponse) response.getContent();
		assertNotNull(getMultiplayerResultsResponse.getPlayerResults());
		assertNotNull(getMultiplayerResultsResponse.getDuelOpponentResults());
		assertEquals(1, getMultiplayerResultsResponse.getPlayerResults().getResultItems().get(ResultType.PLACE).getAmount(), 0);
		assertFalse(getMultiplayerResultsResponse.getDuelOpponentResults().isGameFinished());
		
		// Second calculation should fail => may not overwrite results
		response = sendRequestAndWaitForResults(requestJson);
		assertEquals(ExceptionCodes.ERR_ENTRY_ALREADY_EXISTS, response.getError().getCode());
		
		// Force recalculate should succeed
		requestJson = requestJson.replaceFirst("\"content\": \\{", "\"content\": \\{\"forceRecalculate\":true,"); // add forceRecalculate
		response = sendRequestAndWaitForResults(requestJson);
		assertMessageContentType(response, EmptyJsonMessageContent.class);

		// Now get results should work
		response = sendRequestAndWaitForResults(mpResultRequest);
		assertMessageContentType(response, GetMultiplayerResultsResponse.class);
		getMultiplayerResultsResponse = (GetMultiplayerResultsResponse) response.getContent();
		assertNotNull(getMultiplayerResultsResponse.getPlayerResults());
		assertNotNull(getMultiplayerResultsResponse.getDuelOpponentResults());

		ZonedDateTime dateTimeFrom = DateTimeUtil.parseISODateTimeString("2016-01-11T00:00:00Z");
		ZonedDateTime dateTimeTo = DateTimeUtil.parseISODateTimeString("2016-01-11T23:59:59Z");
		// Game history info should be available
		List<CompletedGameHistoryInfo> completedGameHistoryInfoList = completedGameHistoryAerospikeDao.
				getCompletedGamesList(TEST_USER_ID, TENANT_ID, GameType.DUEL, 0, 10,
						dateTimeFrom, dateTimeTo).getCompletedGameList();
		assertEquals(1, completedGameHistoryInfoList.size());
		CompletedGameHistoryInfo completedGameHistoryInfo = completedGameHistoryInfoList.get(0);

		Long totalTimeInSeconds = DateTimeUtil.getSecondsBetween(
				DateTimeUtil.parseISODateTimeString("2016-01-10T00:00:00Z"),
				DateTimeUtil.parseISODateTimeString("2016-01-11T01:01:01Z"));

		CompletedGameHistoryInfo expectedGameHistoryInfo = new CompletedGameHistoryInfo();
		expectedGameHistoryInfo.setUserId(TEST_USER_ID);
		expectedGameHistoryInfo.setTenantId(TENANT_ID);
		expectedGameHistoryInfo.setGameInstanceId(GAME_INSTANCE_ID1);
		expectedGameHistoryInfo.setGame(new InvitationGame(prepareGame(GameType.DUEL)));
		expectedGameHistoryInfo.setEndDateTime(DateTimeUtil.parseISODateTimeString("2016-01-11T01:01:01Z"));
		expectedGameHistoryInfo.setNumberOfQuestions(3);
		expectedGameHistoryInfo.setNumberOfCorrectQuestions(2);
		expectedGameHistoryInfo.setTotalTime(totalTimeInSeconds);
		expectedGameHistoryInfo.setMultiplayerGameFinished(true);
		expectedGameHistoryInfo.setPlacement(1);
		expectedGameHistoryInfo.addOpponent(new ApiProfileBasicInfo(TEST_USER_ID));
		expectedGameHistoryInfo.setEmailNotification(DUEL_EMAIL_NOTIFICATION);
		expectedGameHistoryInfo.setPlayerGameReadiness(DUEL_PLAYER_READINESS);

		assertGameHistoryInfo(expectedGameHistoryInfo, completedGameHistoryInfo);
	}

	private void assertGameEndNotifications(String mpgiId, GameType gameType, String[] gameInstanceIds, String[] recipients) {
		assertEquals(2, mockServiceReceivedMessageCollector.getReceivedMessageList().size());

		final List<SendWebsocketMessageRequest> notificationRequests = mockServiceReceivedMessageCollector.getReceivedMessageList()
			.stream()
			.map(m -> assertMessageContentType(m, SendWebsocketMessageRequest.class))
			.collect(Collectors.toList());

		assertThat(notificationRequests.stream().map(r -> r.getUserId()).collect(Collectors.toSet()),
				containsInAnyOrder(recipients));
		assertThat(notificationRequests.stream().map(r -> r.getType()).collect(Collectors.toSet()),
				contains(WebsocketMessageType.GAME_END));
		
		final List<GameEndNotification> notifications = notificationRequests.stream()
			.map(r -> jsonUtil.fromJson(r.getPayload(), GameEndNotification.class))
			.collect(Collectors.toList());
		final GameEndNotification[] expectedNotifications = new GameEndNotification[gameInstanceIds.length];
		IntStream.range(0, gameInstanceIds.length)
			.forEach(i -> expectedNotifications[i] = new GameEndNotification(gameType, mpgiId, gameInstanceIds[i]));
		assertThat(notifications, containsInAnyOrder(expectedNotifications));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCalculateTournamentMultiplayerResults() throws Exception {
		String mpgiId = prepareMultiplayerGameInstance(getCalculateTournamentResultRequest(1), "test_user1");
		registerUsers(mpgiId, "test_user1", Pair.of("test_user2", getCalculateTournamentResultRequest(2)));

		// Calculate results
		JsonMessage<? extends JsonMessageContent> response = sendRequestAndWaitForResults(getCalculateTournamentResultRequest(1).replaceAll("mpgi1", mpgiId));
		assertMessageContentType(response, CalculateResultsResponse.class);
		
		response = sendRequestAndWaitForResults(getCalculateTournamentResultRequest(2).replaceAll("mpgi1", mpgiId));
		assertMessageContentType(response, CalculateResultsResponse.class);

		//temporary results before total multiplayer results
		MultiplayerResultsGlobalListResponse globalList = verifyMultiplayerResultsGlobalList(true);
		assertEquals(MultiplayerResultsStatus.PRELIMINARY, globalList.getStatus());
		MultiplayerResultsGlobalListResponse globalListWithinRange = verifyMultiplayerResultsGlobalList(false);
		assertEquals(MultiplayerResultsStatus.PRELIMINARY, globalListWithinRange.getStatus());

		//temporary results for buddies
		response = sendRequestAndWaitForResults(getMultiplayerResultsBuddiesListRequest(false));
		MultiplayerResultsBuddiesListResponse buddiesList = (MultiplayerResultsBuddiesListResponse) response.getContent();
		assertEquals(0, buddiesList.getItems().size());
		assertEquals(MultiplayerResultsStatus.PRELIMINARY, buddiesList.getStatus());

		// Calculate game results
		mockServiceReceivedMessageCollector.clearReceivedMessageList();
		String requestJson = getCalculateMultiplayerResultRequest().replaceAll("mpgi1", mpgiId);
		response = sendRequestAndWaitForResults(requestJson);
		assertMessageContentType(response, EmptyJsonMessageContent.class);

		response = sendRequestAndWaitForResults(getGetMultiplayerResultRequest());
		GetMultiplayerResultsResponse getMultiplayerResultsResponse = (GetMultiplayerResultsResponse) response.getContent();
		assertEquals(2, getMultiplayerResultsResponse.getPlayerResults().getResultItems().get(ResultType.PLACE).getAmount(), 0);
		assertEquals(2, getMultiplayerResultsResponse.getGlobalPlace().intValue());
		assertEquals(0, getMultiplayerResultsResponse.getGlobalPlaceAmongBuddies().intValue());
		assertEquals(0, getMultiplayerResultsResponse.getPlaceAmongBuddies().intValue());
		
		
		RetriedAssert.assertWithWait(() -> assertEquals(3, mockServiceReceivedMessageCollector.getReceivedMessageList().size()));
		List<String> receivedMessages = new ArrayList<>();
		mockServiceReceivedMessageCollector.getReceivedMessageList().forEach(m -> {
			if (m.getContent() instanceof SendWebsocketMessageRequest) {
				receivedMessages.add("send_" + ((SendWebsocketMessageRequest) m.getContent()).getUserId());
			} else {
				CloseJackpotRequest transferRequest = (CloseJackpotRequest) m.getContent();
				for (PayoutItem payout : transferRequest.getPayouts()) {
					receivedMessages.add("transfer_from_" + transferRequest.getTenantId() + "_to_" + payout.getProfileId()
								+ "_amount_" + payout.getAmount());
				}
			}
		});
		assertThat(receivedMessages, containsInAnyOrder("send_test_user1", "send_test_user2", "transfer_from_" + TENANT_ID + "_to_test_user1_amount_1.90", 
				"transfer_from_" + TENANT_ID + "_to_test_user2_amount_7.60"));
		assertEquals(2, transactionLogUpdates); // jackpot payout and bonus transfer
		
		assertEquals(2, userWinningsSaved.size());
		assertWinning(userWinningsSaved.get("test_user2").get(0), "300", Currency.BONUS);
		assertWinning(userWinningsSaved.get("test_user1").get(0), "200", Currency.BONUS);
		assertWinning(userWinningsSaved.get("test_user2").get(1), "7.60", Currency.MONEY);
		assertWinning(userWinningsSaved.get("test_user1").get(1), "1.90", Currency.MONEY);
		
		// List multiplayer results
		globalListWithinRange = verifyMultiplayerResultsGlobalList(false);
		assertEquals(MultiplayerResultsStatus.FINAL, globalListWithinRange.getStatus());
		globalListWithinRange = verifyMultiplayerResultsGlobalList(true);
		assertEquals(MultiplayerResultsStatus.FINAL, globalListWithinRange.getStatus());

		response = sendRequestAndWaitForResults(getMultiplayerResultsBuddiesListRequest(false));
		buddiesList = (MultiplayerResultsBuddiesListResponse) response.getContent();
		assertEquals(0, buddiesList.getItems().size());
		assertEquals(MultiplayerResultsStatus.FINAL, buddiesList.getStatus());
	}

	private MultiplayerResultsGlobalListResponse verifyMultiplayerResultsGlobalList(boolean amongAllHandicapRanges) throws Exception, IOException {
		JsonMessage<? extends JsonMessageContent> response;
		response = sendRequestAndWaitForResults(getMultiplayerResultsGlobalListRequest(amongAllHandicapRanges));
		assertNull(response.getError());
		MultiplayerResultsGlobalListResponse globalList = (MultiplayerResultsGlobalListResponse) response.getContent();
		assertEquals(2, globalList.getItems().size());
		ApiProfileWithResults item0 = globalList.getItems().get(0);
		assertEquals("test_user2", item0.getUserId());
		assertEquals(3, item0.getCorrectAnswers());
		assertEquals(9.7 + 10, item0.getGamePointsWithBonus(), 0.01); // two questions answered quick enough to get additional 10 points (5 + 5)
		assertEquals(Integer.valueOf(0), item0.getHandicapRangeId());
		assertEquals(Integer.valueOf(0), item0.getHandicapFrom());
		assertEquals(Integer.valueOf(100), item0.getHandicapTo());
		assertEquals(Integer.valueOf(1), item0.getPlace());
		ApiProfileWithResults item1 = globalList.getItems().get(1);
		assertEquals("test_user1", item1.getUserId());
		assertEquals(2, globalList.getItems().get(1).getCorrectAnswers());
		assertEquals(6.5 + 5, globalList.getItems().get(1).getGamePointsWithBonus(), 0.01); // one quick answer -> +5 points
		assertEquals(Integer.valueOf(2), globalList.getItems().get(1).getPlace());
		return globalList;
	}
	
	@Test
	public void testStoreWinning() throws Exception {
		String mpgiId = prepareMultiplayerGameInstance(getCalculateTournamentResultRequest(1), "test_user1");
		
		// Calculate results
		JsonMessage<? extends JsonMessageContent> response = sendRequestAndWaitForResults(getCalculateTournamentResultRequest(1).replaceAll("mpgi1", mpgiId));
		assertMessageContentType(response, CalculateResultsResponse.class);

		Results results = resultEngineDao.getResults(GAME_INSTANCE_ID1);
		assertNull(results.getUserWinningComponentId());

		response = sendRequestAndWaitForResults(getStoreUserWinningComponentRequest());
		assertMessageContentType(response, EmptyJsonMessageContent.class);
		
		results = resultEngineDao.getResults(GAME_INSTANCE_ID1);
		assertEquals("uwc1", results.getUserWinningComponentId());
	}

	@Test
	public void testCompletedGameList() throws Exception {
		// prepare test data
        // insert some game history records
        CompletedGameHistoryInfo savedHistoryItem1 = insertGameHistoryRecord(GAME_INSTANCE_ID1, GAME_ID1, GameType.DUEL,
                DateTimeUtil.parseISODateTimeString("2016-01-11T01:01:01Z"), DUEL_PLAYER_READINESS, DUEL_EMAIL_NOTIFICATION);
		CompletedGameHistoryInfo savedHistoryItem2 = insertGameHistoryRecord(GAME_INSTANCE_ID2, "g2", GameType.DUEL,
                DateTimeUtil.parseISODateTimeString("2016-01-11T01:01:03Z"), DUEL_PLAYER_READINESS, DUEL_EMAIL_NOTIFICATION);
		CompletedGameHistoryInfo savedHistoryItem3 = insertGameHistoryRecord("gi3", "g3", GameType.DUEL,
				DateTimeUtil.parseISODateTimeString("2016-01-11T01:01:02Z"), DUEL_PLAYER_READINESS, DUEL_EMAIL_NOTIFICATION);

		CompletedGameListResponse gameListByStatusResponse = getCompletedGameListResponse(
				"completedGameList.json", 10, 0, Arrays.asList(GameType.DUEL),
				"2016-01-10T00:00:00Z", "2016-01-14T23:59:59Z", 3);

		assertGameHistoryInfo(savedHistoryItem2,
				gameListByStatusResponse.getItems().get(0));
		assertGameHistoryInfo(savedHistoryItem3,
				gameListByStatusResponse.getItems().get(1));
		assertGameHistoryInfo(savedHistoryItem1,
				gameListByStatusResponse.getItems().get(2));
	}

	@Test
	public void testCompletedGameListJackpot() throws Exception {
		// prepare test data
        // insert some game history records
		insertGameHistoryRecordMultiplayer(GAME_INSTANCE_ID1, GAME_ID1,
				GameType.TOURNAMENT, DateTimeUtil.parseISODateTimeString("2016-01-11T01:01:01Z"), "111", false);
		CompletedGameHistoryInfo savedHistoryItem2 = insertGameHistoryRecordMultiplayer(GAME_INSTANCE_ID2, "g2",
				GameType.TOURNAMENT, DateTimeUtil.parseISODateTimeString("2016-01-11T01:01:03Z"), "222", false);
		insertGameHistoryRecordMultiplayer("gi3", "g3",
				GameType.TOURNAMENT, DateTimeUtil.parseISODateTimeString("2016-01-11T01:01:02Z"), "333", false);

		CompletedGameListResponse gameListByStatusResponse = getCompletedGameListResponse(
				"completedGameList.json", 10, 0, Arrays.asList(GameType.TOURNAMENT),
				"2016-01-10T00:00:00Z", "2016-01-14T23:59:59Z", 3);

		assertGameHistoryInfo(savedHistoryItem2,
				gameListByStatusResponse.getItems().get(0));
		
		assertEquals(new BigDecimal("10"), gameListByStatusResponse.getItems().get(0).getJackpot().getBalance());
		assertEquals(Currency.CREDIT, gameListByStatusResponse.getItems().get(0).getJackpot().getCurrency());
	}
	
	@Test
	public void testCompletedGameListForUserJackpot() throws Exception {
		// prepare test data
		// insert some game history records
		CompletedGameHistoryInfo savedHistoryItem1 = insertGameHistoryRecordMultiplayer(GAME_INSTANCE_ID1, GAME_ID1,
				GameType.TOURNAMENT, DateTimeUtil.parseISODateTimeString("2016-01-11T01:01:01Z"), "111", false);
		CompletedGameHistoryInfo savedHistoryItem2 = insertGameHistoryRecordMultiplayer(GAME_INSTANCE_ID2, "g2",
				GameType.TOURNAMENT, DateTimeUtil.parseISODateTimeString("2016-01-11T01:01:03Z"), "222", false);
		CompletedGameHistoryInfo savedHistoryItem3 = insertGameHistoryRecordMultiplayer("gi3", "g3",
				GameType.TOURNAMENT, DateTimeUtil.parseISODateTimeString("2016-01-11T01:01:02Z"), "333", false);

		CompletedGameListResponse gameListByStatusResponse = getCompletedGameListForUserResponse(
				"completedGameListForUser.json", ANONYMOUS_USER_ID, 10, 0, Arrays.asList(GameType.TOURNAMENT),
				"2016-01-10T00:00:00Z", "2016-01-14T23:59:59Z", 3);

		assertGameHistoryInfo(savedHistoryItem2, gameListByStatusResponse.getItems().get(0));
		assertGameHistoryInfo(savedHistoryItem3, gameListByStatusResponse.getItems().get(1));
		assertGameHistoryInfo(savedHistoryItem1, gameListByStatusResponse.getItems().get(2));

		assertEquals(new BigDecimal("10"), gameListByStatusResponse.getItems().get(0).getJackpot().getBalance());
		assertEquals(Currency.CREDIT, gameListByStatusResponse.getItems().get(0).getJackpot().getCurrency());
	}
	
	@Test
	public void testCompletedGameListByMultipleGameTypes() throws Exception {
		// prepare test data
        CompletedGameHistoryInfo savedHistoryItem1 = insertGameHistoryRecord(GAME_INSTANCE_ID1, GAME_ID1, GameType.DUEL,
                DateTimeUtil.parseISODateTimeString("2016-01-11T01:01:01Z"));
		CompletedGameHistoryInfo savedHistoryItem2 = insertGameHistoryRecord(GAME_INSTANCE_ID2, "g2", GameType.QUIZ24,
                DateTimeUtil.parseISODateTimeString("2016-01-11T01:01:03Z"));

		CompletedGameListResponse gameListByStatusResponse = getCompletedGameListResponse(
				"completedGameList.json", 10, 0, Arrays.asList(GameType.DUEL, GameType.QUIZ24),
				"2016-01-10T00:00:00Z", "2016-01-14T23:59:59Z", 2);

		assertGameHistoryInfo(savedHistoryItem1,
				gameListByStatusResponse.getItems().get(0));
		assertGameHistoryInfo(savedHistoryItem2,
				gameListByStatusResponse.getItems().get(1));
	}

	@Test
	public void testCompletedGameListPagination() throws Exception {
		List<CompletedGameHistoryInfo> validData = prepareCompletedGameListTestDataWithPagination();

		insertAdditionalGameHistoryRecords();

		CompletedGameListResponse gameListByStatusResponse = getCompletedGameListResponse(
				"completedGameList.json", 5, 4, Arrays.asList(GameType.QUIZ24),
				"2016-01-09T12:00:00Z", "2016-01-20T11:00:00Z", 5);

		assertCompletedGameListWithPaginationResponse(validData, gameListByStatusResponse, 4, 5, 14);
	}


	@Test
	public void testCompletedGameListNullEndDatesPagination() throws Exception {
		prepareCompletedGameListTestDataWithPagination();

		insertAdditionalGameHistoryRecords();

		insertAdditionalGameHistoryRecordsWithNullEndDateTimes();

		CompletedGameListResponse gameListByStatusResponse = getCompletedGameListResponse(
				"completedGameList.json", 5, 0, Arrays.asList(GameType.QUIZ24),
				"2016-01-09T12:00:00Z", "2016-01-20T11:00:00Z", 5);
		// the 2 records with null endDateTime should be the first ones in the results :
		assertNull(gameListByStatusResponse.getItems().get(0).getEndDateTime());
		assertNull(gameListByStatusResponse.getItems().get(1).getEndDateTime());
	}

	@Test
	public void testCompletedGameListWithTimestamp() throws Exception {
		List<CompletedGameHistoryInfo> validData = prepareCompletedGameListTestDataWithPagination();

		CompletedGameListResponse gameListByStatusResponse = getCompletedGameListResponse(
				"completedGameList.json", 10, 0, Arrays.asList(GameType.QUIZ24),
				"2016-01-13T12:00:00Z", "2016-01-17T11:00:00Z", 5);

		assertCompletedGameListWithPaginationResponse(validData, gameListByStatusResponse, 0, 10, 5);
	}

	@Test
	public void testCompletedGameListForUser() throws Exception {
		// prepare test data
		// insert some game history records
		CompletedGameHistoryInfo savedHistoryItem1 = insertGameHistoryRecord(GAME_INSTANCE_ID1, GAME_ID1, GameType.DUEL,
				DateTimeUtil.parseISODateTimeString("2016-01-11T01:01:01Z"), DUEL_PLAYER_READINESS, DUEL_EMAIL_NOTIFICATION);
		CompletedGameHistoryInfo savedHistoryItem2 = insertGameHistoryRecord(GAME_INSTANCE_ID2, "g2", GameType.DUEL,
				DateTimeUtil.parseISODateTimeString("2016-01-11T01:01:03Z"), DUEL_PLAYER_READINESS, DUEL_EMAIL_NOTIFICATION);
		CompletedGameHistoryInfo savedHistoryItem3 = insertGameHistoryRecord("gi3", "g3", GameType.DUEL,
				DateTimeUtil.parseISODateTimeString("2016-01-11T01:01:02Z"), DUEL_PLAYER_READINESS, DUEL_EMAIL_NOTIFICATION);

		CompletedGameListResponse gameListByStatusResponse = getCompletedGameListForUserResponse(
				"completedGameListForUser.json", ANONYMOUS_USER_ID, 10, 0, Arrays.asList(GameType.DUEL),
				"2016-01-10T00:00:00Z", "2016-01-14T23:59:59Z", 3);

		assertGameHistoryInfo(savedHistoryItem2,
				gameListByStatusResponse.getItems().get(0));
		assertGameHistoryInfo(savedHistoryItem3,
				gameListByStatusResponse.getItems().get(1));
		assertGameHistoryInfo(savedHistoryItem1,
				gameListByStatusResponse.getItems().get(2));
	}

	@Test
	public void testCompletedGameListForUserPagination() throws Exception {
		List<CompletedGameHistoryInfo> validData = prepareCompletedGameListTestDataWithPagination();

		insertAdditionalGameHistoryRecords();

		CompletedGameListResponse gameListByStatusResponse = getCompletedGameListForUserResponse(
				"completedGameListForUser.json", ANONYMOUS_USER_ID, 5, 4, Arrays.asList(GameType.QUIZ24),
				"2016-01-09T12:00:00Z", "2016-01-20T11:00:00Z", 5);

		assertCompletedGameListWithPaginationResponse(validData, gameListByStatusResponse, 4, 5, 14);
	}

	@Test
	public void testCompletedGameListForUserWithTimestamp() throws Exception {
		List<CompletedGameHistoryInfo> validData = prepareCompletedGameListTestDataWithPagination();

		CompletedGameListResponse gameListByStatusResponse = getCompletedGameListForUserResponse(
				"completedGameListForUser.json", ANONYMOUS_USER_ID, 10, 0, Arrays.asList(GameType.QUIZ24),
				"2016-01-13T12:00:00Z", "2016-01-17T11:00:00Z", 5);

		assertCompletedGameListWithPaginationResponse(validData, gameListByStatusResponse, 0, 10, 5);
	}

	@Test
	public void testMoveResults() throws Exception {
		ZonedDateTime date = DateTimeUtil.parseISODateTimeString("2016-01-17T01:01:02Z");
		
		// Prepare game history
		insertGameHistory(ANONYMOUS_USER_ID, GAME_INSTANCE_ID1);
		
		// Prepare results
		Results results = new Results();
		results.setUserId(ANONYMOUS_USER_ID);
		results.setGameInstanceId(GAME_INSTANCE_ID1);
		results.setTenantId(TENANT_ID);
		resultEngineDao.saveResults(GAME_INSTANCE_ID1, results, true);
		
		// Prepare completed game history
		insertGameHistoryRecord(GAME_INSTANCE_ID1, GAME_ID1, GameType.QUIZ24, date);
		assertEquals(1, completedGameHistoryAerospikeDao.getNumberOfCompletedGamesForDate(ANONYMOUS_USER_ID,
				TENANT_ID, GameType.QUIZ24, date.toLocalDate()));
		assertEquals(0, completedGameHistoryAerospikeDao.getNumberOfCompletedGamesForDate(TEST_USER_ID,
				TENANT_ID, GameType.QUIZ24, date.toLocalDate()));
		
		// Perform move
		sendRequestAndWaitForResults(getMoveResultsRequest().replaceAll("<<sourceUserId>>", ANONYMOUS_USER_ID)
				.replaceAll("<<targetUserId>>", TEST_USER_ID));
		
		// Verify results
		results = resultEngineDao.getResults(GAME_INSTANCE_ID1);
		assertEquals(TEST_USER_ID, results.getUserId());
		
		// Verify completed game history
		assertEquals(0, completedGameHistoryAerospikeDao.getNumberOfCompletedGamesForDate(ANONYMOUS_USER_ID,
				TENANT_ID, GameType.QUIZ24, date.toLocalDate()));
		assertEquals(1, completedGameHistoryAerospikeDao.getNumberOfCompletedGamesForDate(TEST_USER_ID,
				TENANT_ID, GameType.QUIZ24, date.toLocalDate()));
	}

	@Test
	public void testMoveResultsViaEvent() throws Exception {
		ZonedDateTime date = DateTimeUtil.parseISODateTimeString("2016-01-17T01:01:02Z");

		// Prepare game history
		insertGameHistory(ANONYMOUS_USER_ID, GAME_INSTANCE_ID1);

		// Prepare results
		Results results = new Results();
		results.setUserId(ANONYMOUS_USER_ID);
		results.setGameInstanceId(GAME_INSTANCE_ID1);
		results.setTenantId(TENANT_ID);
		resultEngineDao.saveResults(GAME_INSTANCE_ID1, results, true);

		// Prepare completed game history
		insertGameHistoryRecord(GAME_INSTANCE_ID1, GAME_ID1, GameType.QUIZ24, date);
		assertEquals(1, completedGameHistoryAerospikeDao.getNumberOfCompletedGamesForDate(ANONYMOUS_USER_ID,
				TENANT_ID, GameType.QUIZ24, date.toLocalDate()));
		assertEquals(0, completedGameHistoryAerospikeDao.getNumberOfCompletedGamesForDate(TEST_USER_ID,
				TENANT_ID, GameType.QUIZ24, date.toLocalDate()));

		// Perform move
		String eventJson = getMoveResultsEvent().replaceFirst("<<sourceUserId>>", ANONYMOUS_USER_ID)
				.replaceFirst("<<targetUserId>>", TEST_USER_ID);
		sendAsynMessageAsMockClient(jsonMessageUtil.fromJson(eventJson));

		// Verify results
		RetriedAssert.assertWithWait(() -> assertEquals(TEST_USER_ID, resultEngineDao.getResults(GAME_INSTANCE_ID1).getUserId()));

		// Verify completed game history
		RetriedAssert.assertWithWait(() -> assertEquals(0, completedGameHistoryAerospikeDao.getNumberOfCompletedGamesForDate(ANONYMOUS_USER_ID,
				TENANT_ID, GameType.QUIZ24, date.toLocalDate())));
		RetriedAssert.assertWithWait(() -> assertEquals(1, completedGameHistoryAerospikeDao.getNumberOfCompletedGamesForDate(TEST_USER_ID,
				TENANT_ID, GameType.QUIZ24, date.toLocalDate())));
	}
	
	private void assertCompletedGameListWithPaginationResponse(List<CompletedGameHistoryInfo> validData,
			CompletedGameListResponse response, long offset, int limit, long total) {
		assertGameHistoryInfo(validData.get(0), response.getItems().get(0));
		assertGameHistoryInfo(validData.get(1), response.getItems().get(1));
		assertGameHistoryInfo(validData.get(2), response.getItems().get(2));
		assertGameHistoryInfo(validData.get(3), response.getItems().get(3));
		assertGameHistoryInfo(validData.get(4), response.getItems().get(4));

		assertEquals(offset, response.getOffset());
		assertEquals(limit, response.getLimit());
		assertEquals(total, response.getTotal());
	}

	private List<CompletedGameHistoryInfo> prepareCompletedGameListTestDataWithPagination() {
		List<CompletedGameHistoryInfo> validData = new ArrayList<>();
		// prepare test data
		// insert some game history records
		validData.add(0, insertGameHistoryRecord(GAME_INSTANCE_ID1, GAME_ID1,
				GameType.QUIZ24, DateTimeUtil.parseISODateTimeString("2016-01-17T01:01:02Z")));
		validData.add(1, insertGameHistoryRecord(GAME_INSTANCE_ID2, "g2",
				GameType.QUIZ24, DateTimeUtil.parseISODateTimeString("2016-01-17T01:01:01Z")));
		validData.add(2, insertGameHistoryRecord("gi3", "g3",
				GameType.QUIZ24, DateTimeUtil.parseISODateTimeString("2016-01-15T01:01:02Z")));
		validData.add(3, insertGameHistoryRecord("gi4", "g4",
				GameType.QUIZ24, DateTimeUtil.parseISODateTimeString("2016-01-13T13:01:05Z")));
		validData.add(4, insertGameHistoryRecord("gi5", "g5",
				GameType.QUIZ24, DateTimeUtil.parseISODateTimeString("2016-01-13T13:01:04Z")));

		insertAdditionalGameHistoryRecords();

		return validData;
	}

	private void insertAdditionalGameHistoryRecords() {
		insertGameHistoryRecord("addgi1", "addg1", GameType.QUIZ24,
				DateTimeUtil.parseISODateTimeString("2016-01-18T01:01:02Z"));
		insertGameHistoryRecord("addgi2", "addg2", GameType.QUIZ24,
				DateTimeUtil.parseISODateTimeString("2016-01-18T01:01:01Z"));
		insertGameHistoryRecord("addgi3", "addg3", GameType.QUIZ24,
				DateTimeUtil.parseISODateTimeString("2016-01-17T21:01:03Z"));
		insertGameHistoryRecord("addgi4", "addg3", GameType.QUIZ24,
				DateTimeUtil.parseISODateTimeString("2016-01-13T01:01:03Z"));
		insertGameHistoryRecord("addgi5", "addg4", GameType.QUIZ24,
				DateTimeUtil.parseISODateTimeString("2016-01-13T01:01:02Z"));
		insertGameHistoryRecord("addgi6", "addg5", GameType.QUIZ24,
				DateTimeUtil.parseISODateTimeString("2016-01-12T01:01:03Z"));
		insertGameHistoryRecord("addgi7", "addg6", GameType.QUIZ24,
				DateTimeUtil.parseISODateTimeString("2016-01-10T01:01:02Z"));
		insertGameHistoryRecord("addgi8", "addg7", GameType.DUEL,
				DateTimeUtil.parseISODateTimeString("2016-01-15T01:01:03Z"));
		insertGameHistoryRecord("addgi9", "addg8",
				GameType.QUIZ24, DateTimeUtil.parseISODateTimeString("2016-01-09T11:01:05Z"));
		insertGameHistoryRecord("addgi10", "addg9",
				GameType.QUIZ24, DateTimeUtil.parseISODateTimeString("2016-01-09T13:01:05Z"));
		insertGameHistoryRecord("addgi11", "addg10",
				GameType.QUIZ24, DateTimeUtil.parseISODateTimeString("2016-01-20T01:01:04Z"));
		insertGameHistoryRecord("addgi12", "addg11",
				GameType.QUIZ24, DateTimeUtil.parseISODateTimeString("2016-01-20T21:01:04Z"));
	}

	private void insertAdditionalGameHistoryRecordsWithNullEndDateTimes() {
		insertGameHistoryRecordWithNullEndDateTime("withNull1", "addgame1",
				GameType.QUIZ24, DateTimeUtil.parseISODateTimeString("2016-01-19T01:01:02Z"));

		insertGameHistoryRecordWithNullEndDateTime("withNull2", "addgame2",
				GameType.QUIZ24, DateTimeUtil.parseISODateTimeString("2016-01-19T01:01:02Z"));

		insertGameHistoryRecord("withoutNull1", "addgame3", GameType.QUIZ24,
				DateTimeUtil.parseISODateTimeString("2016-01-19T01:01:02Z"));

		insertGameHistoryRecordWithNullEndDateTime("withNull3", "addgame4",
				GameType.QUIZ24, DateTimeUtil.parseISODateTimeString("2016-01-19T01:01:02Z"));
	}

	private void insertGameHistory(String userId, String gameInstanceId) {
		final String userGameHistoryKey = gameHistoryPrimaryKeyUtil.createPrimaryKey(userId);
		String set = config.getProperty(GameConfigImpl.AEROSPIKE_GAME_HISTORY_SET);
		GameHistory newHistoryEntry = new GameHistory(GameStatus.COMPLETED);
		newHistoryEntry.setGameInstanceId(gameInstanceId);
		commonGameHistoryDao.<String, String> createOrUpdateMapValueByKey(set, userGameHistoryKey, CommonGameHistoryDaoImpl.HISTORY_BIN_NAME, gameInstanceId,
				(v, wp) -> newHistoryEntry.getAsString());
	}
	
	private CompletedGameHistoryInfo insertGameHistoryRecord(String gameInstanceId, String gameId, GameType gameType,
			ZonedDateTime endDateTime) {
        CompletedGameHistoryInfo gameHistoryItem = createGameHistoryInfoItem(gameInstanceId, gameId, endDateTime, gameType, null, null);
        LocalDate endDate = endDateTime.toLocalDate();
        completedGameHistoryAerospikeDao.saveResultsForHistory(ANONYMOUS_USER_ID, TENANT_ID, gameType, endDate, gameHistoryItem, GameHistoryUpdateKind.INITIAL);
		return gameHistoryItem;
    }
	
	private CompletedGameHistoryInfo insertGameHistoryRecord(String gameInstanceId, String gameId, GameType gameType,
			ZonedDateTime endDateTime, Integer playerReadiness, Boolean emailNotification) {
        CompletedGameHistoryInfo gameHistoryItem = createGameHistoryInfoItem(gameInstanceId, gameId, endDateTime, gameType, playerReadiness, emailNotification);
        LocalDate endDate = endDateTime.toLocalDate();
        completedGameHistoryAerospikeDao.saveResultsForHistory(ANONYMOUS_USER_ID, TENANT_ID, gameType, endDate, gameHistoryItem, GameHistoryUpdateKind.INITIAL);
		return gameHistoryItem;
    }

	private CompletedGameHistoryInfo insertGameHistoryRecordWithNullEndDateTime(String gameInstanceId, String gameId, GameType gameType,
															 ZonedDateTime endDateTime) {
		CompletedGameHistoryInfo gameHistoryItem = createGameHistoryInfoItem(gameInstanceId, gameId, null, gameType, null, null);
		LocalDate endDate = endDateTime.toLocalDate();
		completedGameHistoryAerospikeDao.saveResultsForHistory(ANONYMOUS_USER_ID, TENANT_ID, gameType, endDate, gameHistoryItem, GameHistoryUpdateKind.INITIAL);
		return gameHistoryItem;
	}

	private CompletedGameHistoryInfo insertGameHistoryRecordMultiplayer(String gameInstanceId, String gameId, GameType gameType,
			ZonedDateTime endDateTime, String multiplayerGameInstanceId, boolean isFree) {
        CompletedGameHistoryInfo gameHistoryItem = createGameHistoryInfoItem(gameInstanceId, gameId, endDateTime, gameType, new Integer(1), false);
        if (multiplayerGameInstanceId != null){
        	gameHistoryItem.setMultiplayerGameInstanceId(multiplayerGameInstanceId);
        	gameHistoryItem.setGameFree(isFree);
        }
        LocalDate endDate = endDateTime.toLocalDate();
        completedGameHistoryAerospikeDao.saveResultsForHistory(ANONYMOUS_USER_ID, TENANT_ID, gameType, endDate, gameHistoryItem, GameHistoryUpdateKind.INITIAL);
		return gameHistoryItem;
    }
	
	
    private CompletedGameHistoryInfo createGameHistoryInfoItem(String gameInstanceId, String gameId, ZonedDateTime endDateTime, GameType gameType, Integer playerReadiness, Boolean emailNotification) {
        CompletedGameHistoryInfo completedGameHistoryInfo = new CompletedGameHistoryInfo();
        completedGameHistoryInfo.setUserId(TEST_USER_ID);
        completedGameHistoryInfo.setTenantId(TENANT_ID);
        completedGameHistoryInfo.setGameInstanceId(gameInstanceId);
        completedGameHistoryInfo.setEndDateTime(endDateTime);
        completedGameHistoryInfo.setGame(new InvitationGame(prepareGame(gameType)));
        completedGameHistoryInfo.setNumberOfQuestions(5);
        completedGameHistoryInfo.setNumberOfCorrectQuestions(3);
        completedGameHistoryInfo.setTotalTime(300);
        completedGameHistoryInfo.setMultiplayerGameFinished(true);
        completedGameHistoryInfo.setPlacement(1);
        completedGameHistoryInfo.setPrizeWonAmount(new BigDecimal(100));
		completedGameHistoryInfo.setPrizeWonCurrency(Currency.BONUS);
		completedGameHistoryInfo.addOpponent(new ApiProfileBasicInfo(gameInstanceId + "opponentId"));
		completedGameHistoryInfo.setPlayerGameReadiness(playerReadiness);
		completedGameHistoryInfo.setEmailNotification(emailNotification);

		return completedGameHistoryInfo;
    }

    private void assertGameHistoryInfo(CompletedGameHistoryInfo expectedCompletedGameHistoryInfo, CompletedGameHistoryInfo completedGameHistoryInfo) {
		assertEquals(expectedCompletedGameHistoryInfo.getUserId(), completedGameHistoryInfo.getUserId());
		assertEquals(expectedCompletedGameHistoryInfo.getTenantId(), completedGameHistoryInfo.getTenantId());
		assertEquals(expectedCompletedGameHistoryInfo.getGame().getType(), completedGameHistoryInfo.getGame().getType());
		assertEquals(expectedCompletedGameHistoryInfo.getGameInstanceId(), completedGameHistoryInfo.getGameInstanceId());
		assertEquals(expectedCompletedGameHistoryInfo.getGame().getId(), completedGameHistoryInfo.getGame().getId());
		assertEquals(expectedCompletedGameHistoryInfo.getEndDateTime(), completedGameHistoryInfo.getEndDateTime());
		assertEquals(expectedCompletedGameHistoryInfo.getNumberOfQuestions(), completedGameHistoryInfo.getNumberOfQuestions());
		assertEquals(expectedCompletedGameHistoryInfo.getNumberOfCorrectQuestions(), completedGameHistoryInfo.getNumberOfCorrectQuestions());
		assertEquals(expectedCompletedGameHistoryInfo.getTotalTime(), completedGameHistoryInfo.getTotalTime());
		assertEquals(expectedCompletedGameHistoryInfo.isMultiplayerGameFinished(), completedGameHistoryInfo.isMultiplayerGameFinished());
		assertEquals(expectedCompletedGameHistoryInfo.getPlacement(), completedGameHistoryInfo.getPlacement());
		assertEquals(expectedCompletedGameHistoryInfo.getPrizeWonAmount(), completedGameHistoryInfo.getPrizeWonAmount());
		assertEquals(expectedCompletedGameHistoryInfo.getPrizeWonCurrency(), completedGameHistoryInfo.getPrizeWonCurrency());
		assertEquals(expectedCompletedGameHistoryInfo.getEmailNotification(), completedGameHistoryInfo.getEmailNotification());
		assertEquals(expectedCompletedGameHistoryInfo.getPlayerGameReadiness(), completedGameHistoryInfo.getPlayerGameReadiness());
		
		List<String> expectedOpponentIds = expectedCompletedGameHistoryInfo.getOpponents().stream()
			.map(ApiProfileBasicInfo::getUserId)
			.collect(Collectors.toList());
		String[] actualOpponentIds = completedGameHistoryInfo.getOpponents().stream()
				.map(ApiProfileBasicInfo::getUserId)
				.toArray(String[]::new);
		assertThat(expectedOpponentIds, containsInAnyOrder(actualOpponentIds));
    }

    private CompletedGameListResponse getCompletedGameListResponse(String jsonRequestFileName, int limit, int offset,
			List<GameType> gameTypes, String dateFrom, String dateTo, int expectedItemCount) throws Exception {
		final String gameListByStatusJsonRequest = getPlainTextJsonFromResources(jsonRequestFileName, KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replaceFirst("\"<<limit>>\"", String.valueOf(limit))
				.replaceFirst("\"<<offset>>\"", String.valueOf(offset))
				.replaceFirst("\"<<gameTypes>>\"", jsonUtil.toJson(gameTypes))
				.replaceFirst("<<dateFrom>>", dateFrom)
				.replaceFirst("<<dateTo>>", dateTo);

		JsonMessage<? extends JsonMessageContent> response  = sendRequestAndWaitForResults(gameListByStatusJsonRequest);
		assertMessageContentType(response, CompletedGameListResponse.class);
		CompletedGameListResponse completedGameListResponse = (CompletedGameListResponse) response.getContent();
		assertEquals(expectedItemCount, completedGameListResponse.getItems().size());
        return completedGameListResponse;
    }

	private CompletedGameListResponse getCompletedGameListForUserResponse(String jsonRequestFileName, String userId,
			int limit, int offset, List<GameType> gameTypes, String dateFrom, String dateTo, int expectedItemCount) throws Exception {
		final String gameListByStatusJsonRequest = getPlainTextJsonFromResources(jsonRequestFileName, KeyStoreTestUtil.REGISTERED_CLIENT_INFO)
				.replaceFirst("<<userId>>", userId)
				.replaceFirst("\"<<limit>>\"", String.valueOf(limit))
				.replaceFirst("\"<<offset>>\"", String.valueOf(offset))
				.replaceFirst("\"<<gameTypes>>\"", jsonUtil.toJson(gameTypes))
				.replaceFirst("<<dateFrom>>", dateFrom)
				.replaceFirst("<<dateTo>>", dateTo);

		JsonMessage<? extends JsonMessageContent> response  = sendRequestAndWaitForResults(gameListByStatusJsonRequest);
		assertMessageContentType(response, CompletedGameListResponse.class);
		CompletedGameListResponse completedGameListResponse = (CompletedGameListResponse) response.getContent();
		assertEquals(expectedItemCount, completedGameListResponse.getItems().size());
		return completedGameListResponse;
	}

	private void assertWinning(UserWinning winning, String amount, Currency currency) {
		assertEquals("Test Game 1", winning.getTitle());
		assertEquals(UserWinningType.TOURNAMENT, winning.getType());
		assertEquals(new BigDecimal(amount), winning.getAmount());
		assertEquals(currency, winning.getCurrency());
		assertEquals("Test Game Descr.", winning.getDetails());
		assertEquals("pic1", winning.getImageId());
	}

	private JsonMessage<? extends JsonMessageContent> sendRequestAndWaitForResults(String requestJson) throws Exception {
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), requestJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()),
				RetriedAssert.DEFAULT_TIMEOUT_MS * 2);
		JsonMessage<? extends JsonMessageContent> response = testClientReceivedMessageCollector.getReceivedMessageList().get(0);
		testClientReceivedMessageCollector.clearReceivedMessageList();
		return response;
	}
	
	private String getStoreUserWinningComponentRequest() throws IOException {
		return getPlainTextJsonFromResources("storeUserWinningComponentRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
	}
	
	private String getGetMultiplayerResultRequest() throws IOException {
		return getPlainTextJsonFromResources("getMultiplayerResultsRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
	}
	
	private String getMultiplayerResultsGlobalListRequest(boolean amongAllHandicapRanges) throws IOException {
		return getPlainTextJsonFromResources("multiplayerResultsGlobalListRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replace("<<amongAllHandicapRanges>>", String.valueOf(amongAllHandicapRanges));
	}
	
	private String getMultiplayerResultsBuddiesListRequest(boolean amongAllHandicapRanges) throws IOException {
		return getPlainTextJsonFromResources("multiplayerResultsBuddiesListRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replace("<<amongAllHandicapRanges>>", String.valueOf(amongAllHandicapRanges));
	}
	
	private String getCalculateMultiplayerResultRequest() throws IOException {
		return getPlainTextJsonFromResources("calculateMultiplayerResultsRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
	}

	private String getMoveResultsEvent() throws IOException {
		return getPlainTextJsonFromResources("moveResultsEvent.json");
	}

	private String getMoveResultsRequest() throws IOException {
		return getPlainTextJsonFromResources("moveResultsRequest.json");
	}
	
	private String getGetResultRequest() throws IOException {
		return getPlainTextJsonFromResources("getResultsRequest.json");
	}
	
	private String getRespondToPayForWinningRequest() throws IOException {
		return getPlainTextJsonFromResources("respondToPayForWinningRequest.json");
	}
	
	private String getCalculateResultRequest(boolean winningComponent) throws IOException {
		return getCalculateResultRequest(DEFAULT_SPECIAL_PRIZE_CORRECT_ANSWERS_PERCENT, winningComponent, null);
	}

	private String getCalculateResultRequest(boolean winningComponent, ClientInfo clientInfo) throws IOException {
		return getCalculateResultRequest(DEFAULT_SPECIAL_PRIZE_CORRECT_ANSWERS_PERCENT, winningComponent, clientInfo);
	}

	private String getCalculateResultRequest(String specialPrizeCorrectAnswersPercent, boolean winningComponent, ClientInfo clientInfo) throws IOException {
		String result = getPlainTextJsonFromResources("calculateResultsRequest.json", clientInfo != null ? clientInfo : REGISTERED_CLIENT_INFO)
				.replace("\"<<specialPrizeCorrectAnswersPercent>>\"", specialPrizeCorrectAnswersPercent)
				.replace("\"<<playerReadinessDuel>>\"", DUEL_PLAYER_READINESS+"");
		if (winningComponent) {
			return result.replace("<<winningComponents>>", ",\"winningComponents\": ["
					+ "{\"winningComponentId\": \"fwc1\",\"isPaid\": false,\"rightAnswerPercentage\": 60}"
					+ ",{\"winningComponentId\": \"fwc2\",\"isPaid\": false,\"rightAnswerPercentage\": 60}"
					+ ",{\"winningComponentId\": \"pwc1\",\"isPaid\": true,\"amount\":115.0,\"currency\":\"BONUS\",\"rightAnswerPercentage\": 60}"
					+ ",{\"winningComponentId\": \"pwc2\",\"isPaid\": true,\"amount\":115.0,\"currency\":\"BONUS\",\"rightAnswerPercentage\": 60}"
					+ "]");
		} else {
			return result.replaceAll("<<winningComponents>>", "");
		}
	}

	private String getCalculateTournamentResultRequest(int i) throws IOException {
		return getPlainTextJsonFromResources("calculateTournamentResultsRequest" + i + ".json", ANONYMOUS_CLIENT_INFO)
				.replace("\"<<playerReadinessDuel>>\"", DUEL_PLAYER_READINESS+"");
	}

	private String prepareMultiplayerGameInstance(String calculateResultRequest, String userId) throws Exception {
		CustomGameConfig customGameConfig = CustomGameConfigBuilder.create("tenantId", "appId", "gameId", userId)
				.withPlayingRegions("region")
				.withPools("poolIds")
				.withNumberOfQuestions(3)
				.withMaxNumberOfParticipants(2)
				.withEntryFee(BigDecimal.valueOf(5.5), Currency.MONEY)
				.withExpiryDateTime(DateTimeUtil.getCurrentDateTime().plusSeconds(5))
				.withStartDateTime(DateTimeUtil.getCurrentDateTime().minusSeconds(10))
				.withEndDateTime(DateTimeUtil.getCurrentDateTime().plusSeconds(10))
				.withEmailNotification(DUEL_EMAIL_NOTIFICATION)
				.withPlayerGameReadiness(DUEL_PLAYER_READINESS)
				.withGameType(GameType.DUEL)
				.build();
		String mpgiId = multiplayerGameInstanceDao.create(userId, customGameConfig);
		GameInstance gameInstance = prepareGameInstance(calculateResultRequest, mpgiId);
		multiplayerGameInstanceDao.registerForGame(mpgiId, new ClientInfo("t1", "a1", userId, "ip", null), gameInstance.getId());
		return mpgiId;
	}

	private GameInstance prepareGameInstance(String calculateResultRequest, String mpgiId) {
		JsonMessage<CalculateResultsRequest> request = jsonMessageUtil.fromJson(calculateResultRequest);
		GameInstance gameInstance = new GameInstance(request.getContent().getGameInstance());
		gameInstance.setMgiId(mpgiId);
		gameInstanceDao.createJson("gameInstance", gameEnginePrimaryKeyUtil.createPrimaryKey(gameInstance.getId()),
				CommonGameInstanceAerospikeDao.BLOB_BIN_NAME, gameInstance.getJsonObject().toString());
		return gameInstance;
	}
	
	@SuppressWarnings("unchecked")
	private void registerUsers(String mpgiId, String inviter, Pair<String, String>... calculateResultRequests) {
		for (Pair<String, String> calculateResultRequest : calculateResultRequests) {
			JsonMessage<CalculateResultsRequest> request = jsonMessageUtil.fromJson(calculateResultRequest.getRight());
			GameInstance gameInstance = new GameInstance(request.getContent().getGameInstance());
			gameInstance.setMgiId(mpgiId);
			gameInstanceDao.createJson("gameInstance", gameEnginePrimaryKeyUtil.createPrimaryKey(gameInstance.getId()),
					CommonGameInstanceAerospikeDao.BLOB_BIN_NAME, gameInstance.getJsonObject().toString());
			multiplayerGameInstanceDao.addUser(mpgiId, calculateResultRequest.getLeft(), inviter, MultiplayerGameInstanceState.INVITED);
			multiplayerGameInstanceDao.registerForGame(mpgiId, new ClientInfo("t1", "a1", calculateResultRequest.getLeft(), "ip", null), gameInstance.getId());
		}
	}
	
	private void prepareMgiConfig(String mgiId, String userId){
		CustomGameConfig customGameConfig = CustomGameConfigBuilder.create(userId).build();
		multiplayerGameInstanceDao.create(userId, customGameConfig);
	}
	
	private Game prepareGame(GameType gameType) {
		Game game = new Game();
		game.setGameId(GAME_ID1);
		game.setType(gameType);
		return game;
	}

}
