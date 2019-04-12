package de.ascendro.f4m.service.game.engine.integration;

import static com.google.common.primitives.Longs.asList;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.GAME_ID;
import static de.ascendro.f4m.service.game.engine.json.GameStartJsonBuilder.startGame;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_LANGUAGE;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.REGISTERED_CLIENT_INFO;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsArrayContainingInAnyOrder.arrayContainingInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;

import com.aerospike.client.IAerospikeClient;
import com.google.common.primitives.Longs;
import com.google.inject.Injector;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.history.dao.CommonGameHistoryDao;
import de.ascendro.f4m.server.history.dao.CommonGameHistoryDaoImpl;
import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.exception.ExceptionType;
import de.ascendro.f4m.service.exception.UnexpectedTestException;
import de.ascendro.f4m.service.exception.client.F4MClientException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypes;
import de.ascendro.f4m.service.game.engine.GameEngineServiceStartup;
import de.ascendro.f4m.service.game.engine.advertisement.AdvertisementUtil;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.dao.history.GameHistoryDao;
import de.ascendro.f4m.service.game.engine.dao.instance.GameInstanceAerospikeDao;
import de.ascendro.f4m.service.game.engine.di.GameEngineDefaultMessageTypeMapper;
import de.ascendro.f4m.service.game.engine.exception.GameEngineExceptionCodes;
import de.ascendro.f4m.service.game.engine.json.GameJsonBuilder;
import de.ascendro.f4m.service.game.engine.json.GameRegisterJsonBuilder;
import de.ascendro.f4m.service.game.engine.json.GameStartJsonBuilder;
import de.ascendro.f4m.service.game.engine.model.Answer;
import de.ascendro.f4m.service.game.engine.model.GameHistory;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameState;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.game.engine.model.QuestionStep;
import de.ascendro.f4m.service.game.engine.model.advertisement.ShowAdvertisementRequest;
import de.ascendro.f4m.service.game.engine.model.answer.AnswerQuestionResponse;
import de.ascendro.f4m.service.game.engine.model.answer.NextStepRequest;
import de.ascendro.f4m.service.game.engine.model.end.EndGameRequest;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.game.engine.model.register.RegisterResponse;
import de.ascendro.f4m.service.game.engine.model.schema.GameEngineMessageSchemaMapper;
import de.ascendro.f4m.service.game.engine.model.start.game.StartGameResponse;
import de.ascendro.f4m.service.game.engine.model.start.step.StartStepRequest;
import de.ascendro.f4m.service.game.selection.GameSelectionMessageTypes;
import de.ascendro.f4m.service.game.selection.exception.F4MNumberOfQuestionsNotValidException;
import de.ascendro.f4m.service.game.selection.exception.F4MPoolsNotValidException;
import de.ascendro.f4m.service.game.selection.exception.GameSelectionExceptionCodes;
import de.ascendro.f4m.service.game.selection.model.game.AdvertisementFrequency;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.GameWinningComponentListItem;
import de.ascendro.f4m.service.game.selection.model.game.ResultConfiguration;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.game.usergameaccess.UserGameAccessDao;
import de.ascendro.f4m.service.gateway.GatewayMessageTypes;
import de.ascendro.f4m.service.integration.F4MAssert;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.collector.ReceivedMessageCollector;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.BindableAbstractModule;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.ThrowingFunction;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.exception.F4MPaymentClientException;
import de.ascendro.f4m.service.payment.exception.PaymentServiceExceptionCodes;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.internal.LoadOrWithdrawWithoutCoverageRequest;
import de.ascendro.f4m.service.payment.model.internal.PaymentDetails;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.result.engine.ResultEngineMessageTypes;
import de.ascendro.f4m.service.result.engine.model.calculate.CalculateResultsRequest;
import de.ascendro.f4m.service.result.engine.model.calculate.CalculateResultsResponse;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.JsonTestUtil;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherReserveRequest;
import de.ascendro.f4m.service.winning.WinningMessageTypes;
import de.ascendro.f4m.service.winning.model.UserWinningComponent;
import de.ascendro.f4m.service.winning.model.WinningComponent;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentAssignRequest;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentAssignResponse;

public abstract class GameApiTest extends F4MServiceWithMockIntegrationTestBase {

	protected static final String FREE_WC_ID = "freeWcId";
	protected static final String GAME_FEE_TRANSACTION_ID = "TRANSACTION_ID_FOR_GAME_ENTRY_FEE";
	protected static final String JOKER_PURCHASE_TRANSACTION_ID = "TRANSACTION_ID_FOR_JOKER_PURCHASE";
	protected static final String VOUCHER_ID = "vouch";
	protected static final String TENANT_ID = "tenantId";
	protected static final String PURCHASE_JOKER_REASON = "Purchase joker";

	protected ReceivedMessageCollector mockServiceReceivedMessageCollector = new ReceivedMessageCollector();

	protected TestDataLoader testDataLoader;

	protected AerospikeDao aerospikeDao;
	protected IAerospikeClient aerospikeClient;
	protected GameInstanceAerospikeDao gameInstanceDao;
	protected CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao;
	protected CommonGameHistoryDao commonGameHistoryDao;
	protected GameHistoryDao gameHistoryDao;
	protected GameEngineServiceStartupUsingAerospikeMock gameEngineServiceStartupUsingAerospikeMock = null;

	protected JsonMessageUtil jsonMessageUtil;
	protected JsonUtil jsonUtil;
	protected AerospikeClientProvider aerospikeClientProvider;
	
	protected List<JsonMessage<? extends JsonMessageContent>> calculateResultsRequests;
	protected List<JsonMessage<? extends JsonMessageContent>> calculateMultiplayerResultsRequests;
	protected List<JsonMessage<? extends JsonMessageContent>> updatePlayedGameRequests;

	protected AtomicBoolean paymentShouldReturnError;
	protected AtomicBoolean resultEngineShouldReturnErrorOnCalculateResults;
	protected AtomicBoolean resultEngineShouldReturnErrorOnCalculateGameResults;
	
	protected UserGameAccessDao userSpecialGameDao;
	protected Injector gameEngineInjector;


	@Override
	protected void configureInjectionModuleMock(BindableAbstractModule module) {
		module.bindToModule(JsonMessageTypeMap.class).to(GameEngineDefaultMessageTypeMapper.class);
		module.bindToModule(JsonMessageSchemaMap.class).to(GameEngineMessageSchemaMapper.class);
	};

	@Override
	protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandlerMock() {
		return ctx -> onReceivedMessage(ctx);
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		calculateResultsRequests = new CopyOnWriteArrayList<>();
		calculateMultiplayerResultsRequests = new CopyOnWriteArrayList<>();
		updatePlayedGameRequests = new CopyOnWriteArrayList<>();
		
		paymentShouldReturnError = new AtomicBoolean(false);
		resultEngineShouldReturnErrorOnCalculateResults = new AtomicBoolean(false);
		resultEngineShouldReturnErrorOnCalculateGameResults = new AtomicBoolean(false);
		
		gameEngineInjector = jettyServerRule.getServerStartup().getInjector();
		GameEngineConfig config = gameEngineInjector.getInstance(GameEngineConfig.class);
		aerospikeDao = (AerospikeDao) gameEngineInjector
				.getInstance(GameInstanceAerospikeDao.class);
		aerospikeClientProvider = gameEngineInjector.getInstance(AerospikeClientProvider.class);
		aerospikeClient = aerospikeClientProvider.get();
		aerospikeClient.close();

		gameInstanceDao = gameEngineInjector.getInstance(GameInstanceAerospikeDao.class);
		commonGameHistoryDao = gameEngineInjector.getInstance(CommonGameHistoryDaoImpl.class);
		gameHistoryDao = gameEngineInjector.getInstance(GameHistoryDao.class);		

		userSpecialGameDao = gameEngineInjector.getInstance(UserGameAccessDao.class);
		
		testDataLoader = new TestDataLoader(aerospikeDao, aerospikeClient, config);
		commonMultiplayerGameInstanceDao = gameEngineInjector.getInstance(CommonMultiplayerGameInstanceDao.class);

		jsonMessageUtil = clientInjector.getInstance(JsonMessageUtil.class);
		jsonUtil = clientInjector.getInstance(JsonUtil.class);

		mockServiceReceivedMessageCollector.setSessionWrapper(mock(SessionWrapper.class));

		assertServiceStartup(ResultEngineMessageTypes.SERVICE_NAME, ProfileMessageTypes.SERVICE_NAME,
				PaymentMessageTypes.SERVICE_NAME, WinningMessageTypes.SERVICE_NAME, VoucherMessageTypes.SERVICE_NAME);
	}
	
	@Override
	protected void startTestClient() {
		super.startTestClient();
		Config clientConfig = clientInjector.getInstance(Config.class);
		clientConfig.setProperty(F4MConfigImpl.SERVICE_NAME, GatewayMessageTypes.SERVICE_NAME);
	}
	
	/**
	 * Attempt to register for game and start the game TWICE:
	 * 1) attempt to use custom number of questions and custom pools to play, with appropriate game config
	 * 2) attempt to play customized game but with forbidden game config for customization
	 * 
	 * @param registerJsonBuilder - Game registration call builder
	 * @param gameJsonBuilder - Game data builder
	 * @param customNumberOfQuestions - expected custom number for questions
	 * @param customPoolIds - expected custom pools
	 * @param dataPreparer - data preparer to be used before each playt attempt
	 * @throws Exception
	 */
	protected void assertCustomizedGameRegisterAndStart(GameRegisterJsonBuilder registerJsonBuilder, GameJsonBuilder gameJsonBuilder,
			int customNumberOfQuestions, String[] customPoolIds, Runnable dataPreparer) throws Exception{
		ResultConfiguration resultConfiguration = new ResultConfiguration();
		resultConfiguration.setProperty(ResultConfiguration.PROPERTY_SPECIAL_PRIZE, Boolean.TRUE);
		resultConfiguration.setProperty(ResultConfiguration.PROPERTY_SPECIAL_PRIZE_VOUCHER_ID, VOUCHER_ID);

		//With custom user pools -> use custom pools
		dataPreparer.run();
		testDataLoader.prepareTestGameData(gameJsonBuilder
				.withNumberOfQuestions(0)
				.withCustomUserPools(true)
				.withResultConfiguration(resultConfiguration));
		
		//register & start
		final String customGameInstanceId = requestRegistrationForGame(registerJsonBuilder, GAME_ID);
		requestGameStart(startGame(customGameInstanceId), false);


		UserVoucherReserveRequest reserveVouherRequest = mockServiceReceivedMessageCollector
				.<UserVoucherReserveRequest>getMessageByType(VoucherMessageTypes.USER_VOUCHER_RESERVE)
				.getContent();
		assertEquals(VOUCHER_ID, reserveVouherRequest.getVoucherId());

		//assert custom game instance
		final GameInstance customPoolGameInstance = gameInstanceDao.getGameInstance(customGameInstanceId);
		final Set<String> actualCustomPools = customPoolGameInstance.getQuestionsMap().entrySet().stream()
			.map(e-> jsonUtil.fromJson(e.getValue(), Question.class).getId().split("-")[0])
			.collect(Collectors.toSet());
		assertThat(actualCustomPools, containsInAnyOrder(customPoolIds));
		assertEquals(customNumberOfQuestions, customPoolGameInstance.getQuestionIds().length);
		assertEquals(customNumberOfQuestions, customPoolGameInstance.getNumberOfQuestionsIncludingSkipped());
		assertEquals(ANONYMOUS_USER_LANGUAGE, customPoolGameInstance.getUserLanguage());
		assertThat(customPoolGameInstance.getQuestionPoolIds(), equalTo(customPoolIds));
		
		if (registerJsonBuilder.getMgiId() == null) {
			assertSinglePlayerGameInvalidParameters(registerJsonBuilder, gameJsonBuilder, dataPreparer);
		}
	}

	private void assertSinglePlayerGameInvalidParameters(GameRegisterJsonBuilder registerJsonBuilder,
			GameJsonBuilder gameJsonBuilder, Runnable dataPreparer)
					throws IOException, Exception {
		// Game without option to override pools
		dataPreparer.run();
		testDataLoader.prepareTestGameData(gameJsonBuilder
				.withNumberOfQuestions(0)
				.withCustomUserPools(false)); // forbidden customization
		
		F4MClientException expectedPoolsNotValid = new F4MPoolsNotValidException("any message");
		requestRegistrationWithInvalidParameters(registerJsonBuilder, GAME_ID, expectedPoolsNotValid, GameSelectionExceptionCodes.ERR_POOLS_NOT_VALID);
		
		// Game without option to override number of questions
		dataPreparer.run();
		testDataLoader.prepareTestGameData(gameJsonBuilder
				.withNumberOfQuestions(42) // forbidden customization
				.withCustomUserPools(true));
		
		F4MNumberOfQuestionsNotValidException exptectedQuestionsNotValid = new F4MNumberOfQuestionsNotValidException("any message");
		requestRegistrationWithInvalidParameters(registerJsonBuilder, GAME_ID, exptectedQuestionsNotValid, GameSelectionExceptionCodes.ERR_NUMBER_OF_QUESTIONS_NOT_VALID);
	}
	
	/**
	 * Expect error for API messages, which are related to continue of the game play
	 */
	protected void assertCancelledGameCannotContinue(final String gameInstanceId)
			throws URISyntaxException, IOException {
		//-start
		assertResponseError(startGame(gameInstanceId).buildJson(jsonUtil), ExceptionType.CLIENT,
				GameEngineExceptionCodes.ERR_GAME_CANCELLED);
		//-redyToPlay
		final String readyToPlayJson = testDataLoader.getReadyToPlayJson(gameInstanceId, REGISTERED_CLIENT_INFO);				
		assertResponseError(readyToPlayJson, ExceptionType.CLIENT, GameEngineExceptionCodes.ERR_GAME_CANCELLED);
		//-answer
		final String anyAnswerQuestionJson = testDataLoader.getAnswerQuestionJson(REGISTERED_CLIENT_INFO, gameInstanceId, 1, 1, new String[] { "any" }, 100);
		assertResponseError(anyAnswerQuestionJson, ExceptionType.CLIENT, GameEngineExceptionCodes.ERR_GAME_CANCELLED);
		//-next step
		final String anyNextStepJson = testDataLoader.getNextStepJson(REGISTERED_CLIENT_INFO, gameInstanceId, 1, 1, 100);
		assertResponseError(anyNextStepJson, ExceptionType.CLIENT, GameEngineExceptionCodes.ERR_GAME_CANCELLED);
		//-retry cancel
		final String cancelGameJson = testDataLoader.getGameCancellationJson(gameInstanceId, REGISTERED_CLIENT_INFO);
		assertResponseError(cancelGameJson, ExceptionType.CLIENT, GameEngineExceptionCodes.ERR_GAME_CANCELLED);
	}
	
	protected void requestGameCancellation(String gameInstanceId, ClientInfo clientInfo) throws IOException, URISyntaxException {
		final String cancelGameJson = testDataLoader.getGameCancellationJson(gameInstanceId, clientInfo);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), cancelGameJson);
		assertReceivedMessagesWithWait(GameEngineMessageTypes.CANCEL_GAME_RESPONSE);

		final JsonMessage<EmptyJsonMessageContent> cancelGameResponseMessage = testClientReceivedMessageCollector
				.getMessageByType(GameEngineMessageTypes.CANCEL_GAME_RESPONSE);
		assertMessageContentType(cancelGameResponseMessage, EmptyJsonMessageContent.class);
		assertEquals(clientInfo.getClientId(), cancelGameResponseMessage.getClientInfo().getClientId());
	}

	protected void requestReadyToPlay(ClientInfo clientInfo, String gameId, String gameInstanceId) throws URISyntaxException, IOException {
		final String readyToPlayJson = testDataLoader.getReadyToPlayJson(gameInstanceId, clientInfo);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), readyToPlayJson);
		
		assertReceivedMessagesWithWait(ReceivedMessagesAssertMode.ALL, GameEngineMessageTypes.HEALTH_CHECK, GameEngineMessageTypes.READY_TO_PLAY_RESPONSE);		
		
		final JsonMessage<EmptyJsonMessageContent> readyToPlayResponseMessage = testClientReceivedMessageCollector
				.getMessageByType(GameEngineMessageTypes.READY_TO_PLAY_RESPONSE);
		assertMessageContentType(readyToPlayResponseMessage, EmptyJsonMessageContent.class);
		assertNotNull(readyToPlayResponseMessage.getClientId());

		final GameInstance gameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		assertNotNull(gameInstance);
		assertNotNull(gameInstance.getGameState());

		//GameStatus
		final boolean hasAwaitingAdvertisement = AdvertisementUtil.hasAwaitingAdvertisement(gameInstance, GameStatus.READY_TO_PLAY);
		final boolean postponedStart = hasAwaitingAdvertisement || gameInstance.getGame().getType().isLive();
		final GameStatus expectedGameStatus = postponedStart ? GameStatus.READY_TO_PLAY : GameStatus.IN_PROGRESS;
		assertEquals(expectedGameStatus, gameInstance.getGameState().getGameStatus());
		assertEquals(expectedGameStatus, testDataLoader.getGameHistory(clientInfo.getUserId(), gameInstanceId).getStatus());
		assertEquals(expectedGameStatus, testDataLoader.readActiveGameInstance(gameInstanceId).getStatus());
	}
	
	protected String playGameFlow(GameJsonBuilder gameJsonBuilder,
			GameRegisterJsonBuilder gameRegisterJsonBuilder) throws Exception {
		final ClientInfo clientInfo = gameRegisterJsonBuilder.getClientInfo();
		final String gameId = gameJsonBuilder.getGameId();
		
		//prepare game
		testDataLoader.prepareTestGameData(gameJsonBuilder);
		final Game game = gameJsonBuilder.buildGame(jsonUtil);

		
		//Register for the game
		final String gameInstanceId = requestRegistrationForGame(gameRegisterJsonBuilder, GAME_ID);
		
		//Start Game
		requestGameStart(startGame(gameInstanceId)
				.byUser(clientInfo), game.isOffline());
		
		//Ready to Play
		requestReadyToPlay(clientInfo, gameId, gameInstanceId);//Expect readyToPlayResponse, nextStep and healthCheck
		assertReceivedMessagesWithWait(ReceivedMessagesAssertMode.ONE_OF, GameEngineMessageTypes.START_STEP, GameEngineMessageTypes.SHOW_ADVERTISEMENT);
		assertAndReplyHealthCheck(testClientReceivedMessageCollector.getMessageByType(GameEngineMessageTypes.HEALTH_CHECK));
		
		int showedAdvertisements = 0;
		
		if(hasAwaitingAdvertisement(gameInstanceId,  GameStatus.PREPARED)){
			processAdvertisementIfHasAwaiting(clientInfo, gameInstanceId, GameStatus.PREPARED);
			showedAdvertisements++;
		}
		assertStartStepRequest(0, 0);
		assertAndReplyHealthCheck(testClientReceivedMessageCollector.getMessageByType(GameEngineMessageTypes.HEALTH_CHECK));

		boolean nextStepPerformed;
		int currentQuestion = 0;

		final Map<Integer, String[]> questionAnswers = new HashMap<>();
		final Map<Integer, List<Long>> stepAnswerTimes = new HashMap<>();

		//Answer questions until the end of the game
		while (!isGameEnded(gameInstanceId)) {
			testClientReceivedMessageCollector.clearReceivedMessageList();
			
			nextStepPerformed = sendNextOrAnswer(gameInstanceId, clientInfo, questionAnswers, stepAnswerTimes);
			//Expecting:
			//1-gameEngine/healthCheck
			//2-gameEngine/startStep | gameEngine/showAdvertisement
			//3-gameEngine/answerQuestionResponse (optional) 
			final boolean hasAwaitingAdvertisement = hasAwaitingInGameAdvertisement(gameInstanceId, currentQuestion);
			final int expectedMessageCount = nextStepPerformed || hasAwaitingAdvertisement? 2 : 3;
			
			RetriedAssert.assertWithWait(() -> {
				if (!isGameEnded(gameInstanceId)) {
					F4MAssert.assertSize(expectedMessageCount,
							testClientReceivedMessageCollector.getReceivedMessageList());
				}
			});

			if (!isGameEnded(gameInstanceId)) {
				if (!nextStepPerformed) {//Validate previous question answer
					assertAnswer(currentQuestion, gameInstanceId, questionAnswers.get(currentQuestion),
							stepAnswerTimes.get(currentQuestion), game.isInstantAnswerFeedback());
					if(hasAwaitingInGameAdvertisement(gameInstanceId, currentQuestion)){
						processAdvertisementIfHasAwaiting(clientInfo, gameInstanceId, GameStatus.IN_PROGRESS);
						showedAdvertisements++;
					}
				}
				
				if(!isGameEnded(gameInstanceId)){
					//Validate StartStepRequest
					final int expectedQuestion = nextStepPerformed ? currentQuestion : currentQuestion + 1;
					final int expectedStep = nextStepPerformed ? 1 : 0;
					currentQuestion = assertStartStepRequest(expectedQuestion, expectedStep);
	
					assertAndReplyHealthCheck(testClientReceivedMessageCollector.getMessageByType(GameEngineMessageTypes.HEALTH_CHECK));
				}
			} else {
				if(hasAwaitingAdvertisement(gameInstanceId, GameStatus.COMPLETED)){
					processAdvertisementIfHasAwaiting(clientInfo, gameInstanceId, GameStatus.COMPLETED);
					showedAdvertisements++;
				}
				assertReceivedMessagesWithWait(GameEngineMessageTypes.ANSWER_QUESTION_RESPONSE, GameEngineMessageTypes.END_GAME);
			}
		}

		//End with results been calculated
		final JsonMessage<EndGameRequest> endGameRequest = testClientReceivedMessageCollector
				.getMessageByType(GameEngineMessageTypes.END_GAME);
		assertMessageContentType(endGameRequest, EndGameRequest.class);
		assertEquals(GameEndStatus.CALCULATED_RESULT, endGameRequest.getContent().getStatus());
		
		//End with calculating results request to result engine and its response
		RetriedAssert.assertWithWait(() -> assertEquals(1, calculateResultsRequests.size()));
		final JsonMessage<? extends JsonMessageContent> calculateRequestMessage = calculateResultsRequests.get(0);
		assertMessageContentType(calculateRequestMessage, CalculateResultsRequest.class);
		calculateResultsRequests.clear();
		
		//Validate Game Instance status
		final GameInstance endGameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		assertNotNull(endGameInstance);
		assertEquals(GameEndStatus.CALCULATED_RESULT, endGameInstance.getGameState().getGameEndStatus());
		assertNull(testDataLoader.readActiveGameInstance(endGameInstance.getId()));
		
		//Validate Game History entry
		final GameHistory endGameHistory = testDataLoader.getGameHistory(clientInfo.getUserId(), gameInstanceId);
		assertNotNull(endGameHistory);
		assertEquals(GameEndStatus.CALCULATED_RESULT, endGameHistory.getEndStatus());
		
		//Test is question play is recorded
		for(String questionId : endGameInstance.getQuestionIds()){
			assertTrue(gameHistoryDao.isQuestionPlayed(clientInfo.getUserId(), questionId));
		}
		
		//Validate multiplayer game instance
		if (game.isMultiUserGame()) {
			final List<MultiplayerUserGameInstance> multiplayerGameInstances = commonMultiplayerGameInstanceDao
					.getGameInstances(gameRegisterJsonBuilder.getMgiId(), MultiplayerGameInstanceState.CALCULATED);
			assertThat(multiplayerGameInstances, hasItem(new MultiplayerUserGameInstance(gameInstanceId, clientInfo)));
		}
		
		//Advertisements
		if (endGameInstance.hasAdvertisements()){
			assertEquals(showedAdvertisements,
					AdvertisementUtil.getAdvertisementCountWithinGame(game.getAdvertisementFrequency(),
							endGameInstance.getNumberOfQuestionsIncludingSkipped(),
							game.getAdvertisementFrequencyXQuestionDefinition()));
		} else {
			assertNull(endGameInstance.getAdvertisementBlobKeys());
		}
		
		return gameInstanceId;
	}
	
	private boolean hasAwaitingInGameAdvertisement(String gameInstanceId, int currentQuestion){
		final GameInstance gameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		final AdvertisementFrequency advertisementFrequency = gameInstance.getGame().getAdvertisementFrequency();
		final Integer advFreqXQuestionDefinition = gameInstance.getGame().getAdvertisementFrequencyXQuestionDefinition();
		return AdvertisementUtil.hasAwaitingAdvertisement(GameStatus.IN_PROGRESS, advertisementFrequency, currentQuestion, advFreqXQuestionDefinition);
	}
	
	private boolean hasAwaitingAdvertisement(String gameInstanceId, GameStatus gameStatus){
		final GameInstance gameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		return AdvertisementUtil.hasAwaitingAdvertisement(gameInstance, gameStatus);
	}

	private void processAdvertisementIfHasAwaiting(ClientInfo clientInfo, String gameInstanceId, GameStatus gameStatus) throws URISyntaxException {
		final JsonMessage<ShowAdvertisementRequest> showAdvertisementMessage = testClientReceivedMessageCollector
				.getMessageByType(GameEngineMessageTypes.SHOW_ADVERTISEMENT);
		assertShowAdvertisementMessge(clientInfo.getClientId(), gameInstanceId, showAdvertisementMessage);
		sendShowAdvertisementReply(clientInfo, showAdvertisementMessage, gameStatus);
		testClientReceivedMessageCollector.removeMessageByType(GameEngineMessageTypes.SHOW_ADVERTISEMENT);
	}

	private void sendShowAdvertisementReply(ClientInfo clientInfo, JsonMessage<ShowAdvertisementRequest> showAdvertisementMessage, GameStatus gameStatus) throws URISyntaxException {
		final ShowAdvertisementRequest showAdvertisementRequest = showAdvertisementMessage.getContent();
		final NextStepRequest nextStepRequest = new NextStepRequest();
		nextStepRequest.setGameInstanceId(showAdvertisementRequest.getGameInstanceId());
		nextStepRequest.setClientMsT((long) 100);
		
		final JsonMessage<NextStepRequest> nextStepMessage = jsonMessageUtil
				.createNewResponseMessage(GameEngineMessageTypes.NEXT_STEP.getMessageName(), nextStepRequest,
						showAdvertisementMessage);
		nextStepMessage.setClientInfo(clientInfo);
		jsonWebSocketClientSessionPool.sendAsyncMessage(getServiceConnectionInformation(), nextStepMessage);
		assertReceivedMessagesWithWait(ReceivedMessagesAssertMode.ONE_OF, GameEngineMessageTypes.END_GAME, GameEngineMessageTypes.START_STEP);
	}

	private void assertShowAdvertisementMessge(String clientId, String gameInstanceId, JsonMessage<ShowAdvertisementRequest> showAdvertisementMessage) {
		assertMessageContentType(showAdvertisementMessage, ShowAdvertisementRequest.class);
		assertEquals(clientId, showAdvertisementMessage.getClientId());
		final ShowAdvertisementRequest showAdvertisementRequest = showAdvertisementMessage.getContent();
		assertEquals(gameInstanceId, showAdvertisementRequest.getGameInstanceId());
		assertNotNull(showAdvertisementRequest.getAdvertisementBlobKey());
		assertThat(Arrays.asList(gameInstanceDao.getGameInstance(gameInstanceId).getAdvertisementBlobKeys()),
				hasItem(showAdvertisementRequest.getAdvertisementBlobKey()));		
	}

	private void assertAnswer(int currentQuestion, String gameInstanceId, String[] expectedAnswers,
			List<Long> clientMsStepTimes, boolean instantAnswerFeedback) {
		final JsonMessage<AnswerQuestionResponse> questionAnswerResponse = testClientReceivedMessageCollector
				.getMessageByType(GameEngineMessageTypes.ANSWER_QUESTION_RESPONSE);
		assertMessageContentType(questionAnswerResponse, AnswerQuestionResponse.class);
		assertNotNull(questionAnswerResponse.getContent().getCorrectAnswers());
		
		assertEquals(instantAnswerFeedback, questionAnswerResponse.getContent().getCorrectAnswers().length > 0);

		final GameInstance gameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		final GameState gameState = gameInstance.getGameState();
		assertNotNull(gameState);
		assertNotNull(gameState.getAnswers());
		assertEquals(gameState.getCurrentQuestionStep().getQuestion() + 1, gameState.getAnswers().length);
//		assertNull(gameState.getAnswer(gameState.getCurrentQuestionStep().getQuestion()).getAnswers());//start step time only

		final Answer answer = gameState.getAnswer(currentQuestion);
		assertNotNull("No answer for question[" + currentQuestion + "]", answer);
		assertThat(asList(answer.getAnswers()), contains(expectedAnswers));
		assertNotNull(answer.getPrDelMsT());
		assertThat("PrDelMsT has values less than zero", asList(answer.getPrDelMsT()), everyItem(greaterThan(0L))); //tPRDEL presence > 0
		assertThat(asList(answer.getClientMsT()), contains(clientMsStepTimes.toArray(new Long[clientMsStepTimes.size()])));

		final Question question = gameInstance.getQuestion(currentQuestion);
		assertThat("Expected answer time for each step", asList(answer.getAnswerReceiveMsT()),
				hasSize(equalTo(question.getStepCount())));
		assertThat(Longs.asList(answer.getAnswerReceiveMsT()), everyItem(greaterThan(0L)));

		assertThat("Expected step start time for each step", asList(answer.getStepStartMsT()),
				hasSize(equalTo(question.getStepCount())));
		assertThat(asList(answer.getStepStartMsT()), everyItem(greaterThan(0L)));

		assertThat("Expected server time for each step", asList(answer.getServerMsT()),
				hasSize(equalTo(question.getStepCount())));
		assertThat(asList(answer.getServerMsT()), everyItem(greaterThan(0L)));

		IntStream.range(0, question.getStepCount()).forEach(
				i -> assertEquals("tServerMs = answerReceiveMsT - stepStartMsT", answer.getServerMsT()[i],
						answer.getAnswerReceiveMsT()[i] - answer.getStepStartMsT()[i]));
		
		if(instantAnswerFeedback){
			assertThat(question.getCorrectAnswers(),
					arrayContainingInAnyOrder(questionAnswerResponse.getContent().getCorrectAnswers()));
		}
	}

	protected void assertAndReplyHealthCheck(JsonMessage<EmptyJsonMessageContent> healthCheckRequestMessage) throws URISyntaxException {
		assertNotNull(healthCheckRequestMessage);
		assertNotNull(healthCheckRequestMessage.getClientId());
		
		final JsonMessage<EmptyJsonMessageContent> healthcheckResponseMessage = jsonMessageUtil
				.createNewResponseMessage(GameEngineMessageTypes.HEALTH_CHECK_RESPONSE.getMessageName(),
						new EmptyJsonMessageContent(), healthCheckRequestMessage);
		jsonWebSocketClientSessionPool.sendAsyncMessage(getServiceConnectionInformation(), healthcheckResponseMessage);
	}

	private int assertStartStepRequest(int question, int step) {
		final JsonMessage<StartStepRequest> startStepRequestMessage = testClientReceivedMessageCollector
				.getMessageByType(GameEngineMessageTypes.START_STEP);
		assertMessageContentType(startStepRequestMessage, StartStepRequest.class);
		assertNotNull(startStepRequestMessage.getClientId());
		final StartStepRequest startStepRequest = (StartStepRequest) startStepRequestMessage.getContent();
		assertNotNull(startStepRequest.getDecryptionKey());
		assertNotNull(startStepRequest.getQuestionStepBlobKey());
		assertEquals(question, startStepRequest.getQuestion());
		assertEquals(Integer.valueOf(step), startStepRequest.getStep());

		return startStepRequest.getQuestion();
	}
	
	protected GameInstance assertCommonRegisteredGameInstance(RegisterResponse registerResponse, String gameId, String mgiId, boolean trainingMode) {
		assertNotNull(registerResponse.getGameInstanceId());

		final GameInstance gameInstance = gameInstanceDao.getGameInstance(registerResponse.getGameInstanceId());
		
		//Game
		final Game game = gameInstance.getGame();
		assertNotNull(game);
		assertEquals(gameId, game.getGameId());
		assertEquals(mgiId, gameInstance.getMgiId());
		assertEquals(game.isTournament() ? gameId : null, gameInstance.getTournamentId());
		
		//Questions
		F4MAssert.assertSize(0, Arrays.asList(gameInstance.getAllDecryptionKeys()));
		F4MAssert.assertSize(0, Arrays.asList(gameInstance.getAllQuestionBlobKeys()));
		
		//Game state
		final GameState gameState = gameInstance.getGameState();
		assertNotNull(gameState);
		assertEquals(GameStatus.REGISTERED, gameState.getGameStatus());
		assertEquals(GameStatus.REGISTERED, testDataLoader.readActiveGameInstance(gameInstance.getId()).getStatus());

		if (gameInstance.getEntryFeeAmount() != null) {
			assertTrue(gameState.hasEntryFeeTransactionId());
		}

		assertEquals(gameInstance.getUserId(), testDataLoader.readActiveGameInstance(gameInstance.getId()).getUserId());
		assertEquals(trainingMode, gameInstance.isTrainingMode());
		
		assertEquals(gameInstance.getEntryFeeAmount(), testDataLoader.readActiveGameInstance(gameInstance.getId()).getEntryFeeAmount());
		assertEquals(gameInstance.getEntryFeeCurrency(), testDataLoader.readActiveGameInstance(gameInstance.getId()).getEntryFeeCurrency());
		
		testClientReceivedMessageCollector.clearReceivedMessageList();
		
		return gameInstance;
	}

	protected GameInstance assertCommonStartedGameInstance(StartGameResponse startGameResponse, boolean offline,
			boolean live) {
		assertNotNull(startGameResponse.getGameInstanceId());

		if (offline) {
			assertNotNull(startGameResponse.getDecryptionKeys());
		} else {
			assertNull(startGameResponse.getDecryptionKeys());
		}

		final GameInstance gameInstance = gameInstanceDao.getGameInstance(startGameResponse.getGameInstanceId());
		assertEquals(GameStatus.PREPARED, gameInstance.getGameState().getGameStatus());
		assertEquals(GameStatus.PREPARED, testDataLoader.readActiveGameInstance(gameInstance.getId()).getStatus());
		if (!live) {
			assertNotNull(startGameResponse.getQuestionBlobKeys());
			
			assertTrue(startGameResponse.getQuestionBlobKeys().length > 0);
			assertThat(asList(startGameResponse.getQuestionBlobKeys()),
					is(contains(gameInstance.getAllQuestionBlobKeys())));
		
			assertEquals(gameInstance.getAllQuestionImageBlobKeys().length, startGameResponse.getQuestionImageBlobKeys().length);
		
			assertThat(asList(startGameResponse.getWinningComponentIds()),
					is(contains((String[]) Arrays.stream(TestDataLoader.WINNING_COMPONENTS).map(GameWinningComponentListItem::getWinningComponentId).toArray(size -> new String[size]))));

			assertThat(asList(startGameResponse.getVoucherIds()),
					is(contains(TestDataLoader.VOUCHER_IDS)));
			
			//assertAdvertisements
			final Game game = gameInstance.getGame();
			if(!gameInstance.hasAdvertisements()){
				assertNull(startGameResponse.getAdvertisementBlobKeys());
			}else{
				final int expectedAdvertisementCount = AdvertisementUtil.getAdvertisementCountWithinGame(
						game.getAdvertisementFrequency(), gameInstance.getNumberOfQuestionsIncludingSkipped(),
						game.getAdvertisementFrequencyXQuestionDefinition());
				if(expectedAdvertisementCount > 0){
					F4MAssert.assertSize(expectedAdvertisementCount, Arrays.asList(startGameResponse.getAdvertisementBlobKeys()));
				}
			}
		}
		return gameInstance;
	}

	protected StartGameResponse requestGameStart(GameStartJsonBuilder gameStartJsonBuilder, boolean offlineGame) throws URISyntaxException, IOException {
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), gameStartJsonBuilder.buildJson(jsonUtil));
		assertReceivedMessagesWithWait(GameEngineMessageTypes.START_GAME_RESPONSE);
		
		final JsonMessage<StartGameResponse> startGameResponseMessage = testClientReceivedMessageCollector
				.getMessageByType(GameEngineMessageTypes.START_GAME_RESPONSE);
		assertMessageContentType(startGameResponseMessage, StartGameResponse.class);

		testClientReceivedMessageCollector.clearReceivedMessageList();
		
		final StartGameResponse startGameResponse = (StartGameResponse) startGameResponseMessage.getContent();
		assertCommonStartedGameInstance(startGameResponse, offlineGame, false);
		
		RetriedAssert.assertWithWait(() -> assertEquals(gameStartJsonBuilder.getClientInfo().getHandicap(), 
				gameInstanceDao.getGameInstance(startGameResponse.getGameInstanceId()).getUserHandicap()));
		
		return startGameResponseMessage.getContent();
	}
	
	protected String requestRegistrationForGame(GameRegisterJsonBuilder gameRegisterJsonBuilder, String gameId) throws URISyntaxException, IOException {
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), gameRegisterJsonBuilder.buildJson(jsonUtil));
		assertReceivedMessagesWithWait(GameEngineMessageTypes.REGISTER_RESPONSE);

		final JsonMessage<RegisterResponse> registerResponseMessage = testClientReceivedMessageCollector
				.getMessageByType(GameEngineMessageTypes.REGISTER_RESPONSE);
		assertMessageContentType(registerResponseMessage, RegisterResponse.class);

		testClientReceivedMessageCollector.clearReceivedMessageList();
		
		final RegisterResponse registerResponse = registerResponseMessage.getContent();
		assertCommonRegisteredGameInstance(registerResponse, gameId,
				gameRegisterJsonBuilder.getMgiId(), gameRegisterJsonBuilder.isTrainingMode());		
		return registerResponse.getGameInstanceId();
	}
	
	protected void requestRegistrationWithInvalidParameters(GameRegisterJsonBuilder gameRegisterJsonBuilder,
			String gameId, F4MClientException expectedException, String exceptionCode) throws Exception {
		errorCollector.setExpectedErrors(expectedException);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), gameRegisterJsonBuilder.buildJson(jsonUtil));
		assertReceivedMessagesWithWait(GameEngineMessageTypes.REGISTER_RESPONSE);
		
		JsonMessage<JsonMessageContent> response = testClientReceivedMessageCollector.getMessageByType(GameEngineMessageTypes.REGISTER_RESPONSE);
		assertNull(response.getContent());
		assertNotNull(response.getError());
		assertEquals(exceptionCode, response.getError().getCode());
		
		errorCollector.clearCollectedErrors();
		testClientReceivedMessageCollector.clearReceivedMessageList();
	}

	protected boolean sendNextOrAnswer(final String gameInstanceId, ClientInfo clientInfo, 
			final Map<Integer, String[]> questionAnswers, final Map<Integer, List<Long>> stepAnswerTimes)
			throws IOException, URISyntaxException {
		final boolean nextStepPerformed;
		if (isAnswerExpected(gameInstanceId)) {
			final String answerMessage = createAnswerMessage(gameInstanceId, clientInfo, questionAnswers, stepAnswerTimes);
			jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), answerMessage);
			nextStepPerformed = false;
		} else {
			final String nextStepMessage = createNextStepMessage(gameInstanceId, clientInfo, stepAnswerTimes);
			jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), nextStepMessage);
			nextStepPerformed = true;
		}
		return nextStepPerformed;
	}

	protected boolean isAnswerExpected(String gameInstanceId) {
		final GameInstance currentGameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		final GameState currentGameState = currentGameInstance.getGameState();
		final QuestionStep questionStep = currentGameState.getCurrentQuestionStep();
		return !questionStep.hasNextStep();
	}

	protected boolean isGameEnded(String gameInstanceId) {
		final GameInstance currentGameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		final GameState currentGameState = currentGameInstance.getGameState();
		return currentGameState.isCompleted();
	}

	protected String createAnswerMessage(String gameInstanceId, ClientInfo clientInfo,
			Map<Integer, String[]> questionAnswers, Map<Integer, List<Long>> stepAnswerTimes) throws IOException {

		final GameInstance currentGameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		final GameState currentGameState = currentGameInstance.getGameState();
		final int currentQuestionIndex = currentGameState.getCurrentQuestionStep().getQuestion();
		final int currentStepIndex = currentGameState.getCurrentQuestionStep().getStep();
		final Question currentQuestion = currentGameInstance.getQuestion(currentQuestionIndex);

		final String[] randomAnswers = testDataLoader.selectRandomAnswers(currentQuestion);
		questionAnswers.put(currentQuestionIndex, randomAnswers);

		final long tClientMs = testDataLoader.selectRandomClientMsT(currentQuestion, currentStepIndex);

		addQuestionStepResonseTime(stepAnswerTimes, currentQuestionIndex, tClientMs);

		return testDataLoader.getAnswerQuestionJson(clientInfo, gameInstanceId, currentQuestionIndex, currentStepIndex, randomAnswers, tClientMs);
	}

	protected String createNextStepMessage(String gameInstanceId, ClientInfo clientInfo, Map<Integer, List<Long>> stepAnswerTimes)
			throws IOException {
		final GameInstance currentGameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		final GameState currentGameState = currentGameInstance.getGameState();
		final int currentQuestionIndex = currentGameState.getCurrentQuestionStep().getQuestion();
		final int currentStepIndex = currentGameState.getCurrentQuestionStep().getStep();
		final Question currentQuestion = currentGameInstance.getQuestion(currentQuestionIndex);

		final long tClientMs = testDataLoader.selectRandomClientMsT(currentQuestion, currentStepIndex);

		addQuestionStepResonseTime(stepAnswerTimes, currentQuestionIndex, tClientMs);

		return testDataLoader.getNextStepJson(clientInfo, gameInstanceId, currentQuestionIndex, currentStepIndex, tClientMs);
	}

	protected void addQuestionStepResonseTime(Map<Integer, List<Long>> stepAnswerTimes, final int currentQuestionIndex,
			final long tClientMs) {
		if (!stepAnswerTimes.containsKey(currentQuestionIndex)) {
			stepAnswerTimes.put(currentQuestionIndex, Arrays.asList(tClientMs));
		} else {
			@SuppressWarnings("unchecked")
			final List<Long> tClientMsTimes = ListUtils.union(stepAnswerTimes.get(currentQuestionIndex),
					Arrays.<Long> asList(tClientMs));
			stepAnswerTimes.put(currentQuestionIndex, tClientMsTimes);
		}
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		if (StringUtils.isBlank(System.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST))) {
			if(gameEngineServiceStartupUsingAerospikeMock == null) {
				gameEngineServiceStartupUsingAerospikeMock = new GameEngineServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE);
			}
			return gameEngineServiceStartupUsingAerospikeMock;
		} else {
			return new GameEngineServiceStartup(DEFAULT_TEST_STAGE);
		}
	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(GameEngineDefaultMessageTypeMapper.class, GameEngineMessageSchemaMapper.class);
	}

	protected JsonMessageContent onReceivedMessage(RequestContext requestContext)
			throws Exception {
		mockServiceReceivedMessageCollector.onProcess(requestContext);
		JsonMessage<? extends JsonMessageContent> originalMessageDecoded = requestContext.getMessage();
		if (GameEngineMessageTypes.HEALTH_CHECK == originalMessageDecoded.getType(GameEngineMessageTypes.class)) {
			originalMessageDecoded.setClientInfo(KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
			final String requestJson = jsonMessageUtil.toJson(originalMessageDecoded);

			final String expectedRequestJson = jsonLoader.getPlainTextJsonFromResources("healthCheckRequestExpected.json", ANONYMOUS_CLIENT_INFO);
			
			//ignore clientId and userId in compare, use anonymous user as default
			JsonTestUtil.assertJsonContentEqualIgnoringSeq(expectedRequestJson, requestJson);

			final String responseJson = jsonLoader.getPlainTextJsonFromResources("healthCheckResponse.json", ANONYMOUS_CLIENT_INFO);
			return jsonMessageUtil.fromJson(responseJson).getContent();
		} else if (ResultEngineMessageTypes.CALCULATE_RESULTS == originalMessageDecoded
				.getType(ResultEngineMessageTypes.class)) {
			if (resultEngineShouldReturnErrorOnCalculateResults.get()) {
				throw new F4MFatalErrorException("resultEngineShouldReturnErrorOnCalculateResults is set to true");
			} else {
				calculateResultsRequests.add(originalMessageDecoded);
			}
			return new CalculateResultsResponse(null, FREE_WC_ID);
		} else if (PaymentMessageTypes.TRANSFER_BETWEEN_ACCOUNTS == originalMessageDecoded.getType(PaymentMessageTypes.class)
				|| PaymentMessageTypes.TRANSFER_JACKPOT == originalMessageDecoded.getType(PaymentMessageTypes.class)
				|| PaymentMessageTypes.LOAD_OR_WITHDRAW_WITHOUT_COVERAGE == originalMessageDecoded.getType(PaymentMessageTypes.class)) {
			if (paymentShouldReturnError.get()) {
				throw new F4MPaymentClientException(PaymentServiceExceptionCodes.ERR_INSUFFICIENT_FUNDS,
						"paymentShouldReturnError is set to true");
			} else {
				if (isPurchaseJokerTransaction(originalMessageDecoded)) {
					return new TransactionId(JOKER_PURCHASE_TRANSACTION_ID);
				} else {
					return new TransactionId(GAME_FEE_TRANSACTION_ID);
				}
			}
		} else if (ResultEngineMessageTypes.CALCULATE_MULTIPLAYER_RESULTS == originalMessageDecoded
				.getType(ResultEngineMessageTypes.class)) {
			if (resultEngineShouldReturnErrorOnCalculateGameResults.get()) {
				throw new F4MFatalErrorException("resultEngineShouldReturnErrorOnCalculateGameResults is set to true");
			} else {
				calculateMultiplayerResultsRequests.add(originalMessageDecoded);
			}
			return new CalculateResultsResponse(TestDataLoader.PAID_WINNING_COMPONENT_IDS[0],
					TestDataLoader.WINNING_COMPONENTS[3].getWinningComponentId());
		} else if (WinningMessageTypes.USER_WINNING_COMPONENT_ASSIGN == originalMessageDecoded.getType(WinningMessageTypes.class)) {
			UserWinningComponentAssignRequest request = (UserWinningComponentAssignRequest) originalMessageDecoded.getContent();
			return new UserWinningComponentAssignResponse(new UserWinningComponent(UUID.randomUUID().toString(), 
					new WinningComponent(new String[] { TENANT_ID }, TestDataLoader.WINNING_COMPONENTS[3].getWinningComponentId(),  
							Collections.emptyList(), "Winning Component"), TestDataLoader.WINNING_COMPONENTS[3], request.getGameInstanceId(), "gameId", GameType.DUEL));
		} else if (EventMessageTypes.RESUBSCRIBE == originalMessageDecoded.getType(EventMessageTypes.class)) {
			return null;// ignore event/resubscribe
		} else if (GameSelectionMessageTypes.UPDATE_PLAYED_GAME == originalMessageDecoded.getType(GameSelectionMessageTypes.class)) {
			updatePlayedGameRequests.add(originalMessageDecoded);
			return null;
		} else if (VoucherMessageTypes.USER_VOUCHER_RESERVE == originalMessageDecoded.getType(VoucherMessageTypes.class)) {
			return new EmptyJsonMessageContent();
		} else if (VoucherMessageTypes.USER_VOUCHER_RELEASE == originalMessageDecoded.getType(VoucherMessageTypes.class)) {
			return new EmptyJsonMessageContent();
		} else if (GameSelectionMessageTypes.ACTIVATE_INVITATIONS == originalMessageDecoded.getType(GameSelectionMessageTypes.class)) {
			return null;
		} else {
			throw new UnexpectedTestException("Unexpected message: " + originalMessageDecoded.getName());
		}
	}

	private boolean isPurchaseJokerTransaction(JsonMessage<? extends JsonMessageContent> originalMessageDecoded) {
		if (! (originalMessageDecoded.getContent() instanceof LoadOrWithdrawWithoutCoverageRequest)) {
			return false;
		}
		LoadOrWithdrawWithoutCoverageRequest content = (LoadOrWithdrawWithoutCoverageRequest) originalMessageDecoded.getContent();
		PaymentDetails paymentDetails = content.getPaymentDetails();
		String reason = paymentDetails.getAdditionalInfo();
		return StringUtils.startsWith(reason, PURCHASE_JOKER_REASON);
	}
}
