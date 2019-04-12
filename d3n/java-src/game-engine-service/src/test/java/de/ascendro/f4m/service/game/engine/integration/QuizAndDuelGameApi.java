package de.ascendro.f4m.service.game.engine.integration;

import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.ADVERTISEMENT_PROVIDER_ID;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.ASSIGNED_POOLS;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.GAME_ID;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.MGI_ID;
import static de.ascendro.f4m.service.game.engine.json.GameJsonBuilder.createGame;
import static de.ascendro.f4m.service.game.engine.json.GameRegisterJsonBuilder.registerForMultiplayerGame;
import static de.ascendro.f4m.service.game.engine.json.GameRegisterJsonBuilder.registerForQuiz24;
import static de.ascendro.f4m.service.game.engine.json.GameStartJsonBuilder.startGame;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_HANDICAP;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.REGISTERED_CLIENT_INFO;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.REGISTERED_USER_HANDICAP;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.REGISTERED_USER_ID;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.Record;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.result.RefundReason;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.exception.ExceptionCodes;
import de.ascendro.f4m.service.exception.ExceptionType;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypes;
import de.ascendro.f4m.service.game.engine.exception.GameEngineExceptionCodes;
import de.ascendro.f4m.service.game.engine.json.GameJsonBuilder;
import de.ascendro.f4m.service.game.engine.json.GameRegisterJsonBuilder;
import de.ascendro.f4m.service.game.engine.json.GameStartJsonBuilder;
import de.ascendro.f4m.service.game.engine.model.CloseUpReason;
import de.ascendro.f4m.service.game.engine.model.GameHistory;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameState;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.game.engine.model.joker.JokersAvailableResponse;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseFiftyFiftyResponse;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseHintResponse;
import de.ascendro.f4m.service.game.engine.model.register.RegisterResponse;
import de.ascendro.f4m.service.game.engine.model.start.game.StartGameResponse;
import de.ascendro.f4m.service.game.engine.multiplayer.TestCustomGameConfigBuilder;
import de.ascendro.f4m.service.game.selection.model.dashboard.PlayedGameInfo;
import de.ascendro.f4m.service.game.selection.model.dashboard.UpdatePlayedGameRequest;
import de.ascendro.f4m.service.game.selection.model.game.AdvertisementFrequency;
import de.ascendro.f4m.service.game.selection.model.game.EntryFee;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.GameTypeConfiguration;
import de.ascendro.f4m.service.game.selection.model.game.JokerConfiguration;
import de.ascendro.f4m.service.game.selection.model.game.JokerType;
import de.ascendro.f4m.service.game.selection.model.game.ResultConfiguration;
import de.ascendro.f4m.service.game.selection.model.game.SingleplayerGameTypeConfigurationData;
import de.ascendro.f4m.service.game.selection.model.game.validate.NumberOfQuestionsRule;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder;
import de.ascendro.f4m.service.game.selection.model.singleplayer.SinglePlayerGameParameters;
import de.ascendro.f4m.service.integration.F4MAssert;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.json.model.user.UserRole;
import de.ascendro.f4m.service.payment.exception.F4MPaymentClientException;
import de.ascendro.f4m.service.payment.exception.PaymentServiceExceptionCodes;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionStatus;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherReleaseRequest;

public class QuizAndDuelGameApi extends GameApiTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(QuizAndDuelGameApi.class);
	private static final BigDecimal PRICE = new BigDecimal("3");
	private static final Currency CURRENCY = Currency.CREDIT;

	@Override
	protected JsonMessageContent onReceivedMessage(RequestContext requestContext) throws Exception {
		JsonMessage<? extends JsonMessageContent> originalMessageDecoded = requestContext.getMessage();
		if (GameEngineMessageTypes.START_STEP == originalMessageDecoded.getType(GameEngineMessageTypes.class)) {
			return null;
		} else if(GameEngineMessageTypes.HEALTH_CHECK == originalMessageDecoded.getType(GameEngineMessageTypes.class)) {
			return null;
		}
		return super.onReceivedMessage(requestContext);
	}

	@Test
	public void testRepeatedReadyToPlay() throws IOException, Exception{
		final GameJsonBuilder gameBuilder = createGame(GAME_ID)
				.withGameType(GameType.QUIZ24)
				.withNumberOfQuestions(5);

		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.prepareTestGameData(gameBuilder);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);
		
		final String gameInstanceId = requestRegistrationForGame(registerForQuiz24(GAME_ID, false), GAME_ID);
		requestGameStart(startGame(gameInstanceId), false);
		
		//Ready to play #1
		testClientReceivedMessageCollector.clearReceivedMessageList();
		requestReadyToPlay(ANONYMOUS_CLIENT_INFO, GAME_ID, gameInstanceId);

		//Ready to play #2
		testClientReceivedMessageCollector.clearReceivedMessageList();
		final String readyToPlayJson = testDataLoader.getReadyToPlayJson(gameInstanceId, ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), readyToPlayJson);
		RetriedAssert.assertWithWait(() -> assertNotNull(testClientReceivedMessageCollector.getMessageByType(GameEngineMessageTypes.READY_TO_PLAY_RESPONSE)));
		final JsonMessage<EmptyJsonMessageContent> secondReadyToPlayResponse = testClientReceivedMessageCollector.getMessageByType(GameEngineMessageTypes.READY_TO_PLAY_RESPONSE);
		assertNotNull(secondReadyToPlayResponse.getError());
		assertEquals(GameEngineExceptionCodes.ERR_GAME_FLOW_VIOLATION, secondReadyToPlayResponse.getError().getCode());
	}
	
	@Test
	public void testGameStartOfflineQuiz24() throws Exception {
		final GameJsonBuilder gameJsonBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(5)
				.withGameType(GameType.QUIZ24)
				.withAdvertisement(ADVERTISEMENT_PROVIDER_ID, AdvertisementFrequency.AFTER_EACH_QUESTION)
				.inOfflineMode();
		testDataLoader.prepareTestGameData(gameJsonBuilder);
		testDataLoader.createAdvertisementProvider(ADVERTISEMENT_PROVIDER_ID, 5);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);
		
		final String gameInstanceId = requestRegistrationForGame(registerForQuiz24(GAME_ID, false), GAME_ID);
		requestGameStart(startGame(gameInstanceId), true);
		
		final GameInstance endGameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		//Test is question play is recorded
		for(String questionId : endGameInstance.getQuestionIds()){
			assertTrue(gameHistoryDao.isQuestionPlayed(ANONYMOUS_USER_ID, questionId));
		}
	}
	
	@Test
	public void testStartPaidQuiz24() throws Exception {
		final GameJsonBuilder gameJsonBuilder = createGame(GAME_ID)
				.withEntryFee(new BigDecimal("60"), Currency.BONUS)
				.withGameType(GameType.QUIZ24)
				.withNumberOfQuestions(5);
		testDataLoader.prepareTestGameData(gameJsonBuilder);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);
		final String gameInstanceId = requestRegistrationForGame(registerForQuiz24(GAME_ID, false), GAME_ID);
		requestGameStart(startGame(gameInstanceId), false);
	}
	
	@Test
	public void testStartQuiz24InTrainingMode() throws Exception{
		final GameJsonBuilder gameJsonBuilder = createGame(GAME_ID)
				.withGameType(GameType.QUIZ24)
				.withNumberOfQuestions(5);
		testDataLoader.prepareTestGameData(gameJsonBuilder);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);
		final String gameInstanceId = requestRegistrationForGame(registerForQuiz24(GAME_ID, true), GAME_ID);
		requestGameStart(startGame(gameInstanceId), false);
	}
	
	@Test
	public void testDuelStartWithCustomPoolsAndNumberOfQuestions() throws Exception {
		final int customNumberOfQuestions = NumberOfQuestionsRule.VALID_NUMBER_OF_QUESTIONS_DEFAULT.get(0);
		final String[] customPoolIds = new String[] { TestDataLoader.ASSIGNED_POOLS[0] };
		final CustomGameConfigBuilder customGameConfigBuilder = TestCustomGameConfigBuilder.create(ANONYMOUS_USER_ID)
				.withPools(customPoolIds)
				.withNumberOfQuestions(customNumberOfQuestions);
		
		final Runnable prepareData = () -> {
			try {
				aerospikeClient.close();
				testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, true);
				testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);
				testDataLoader.inviteFriends(aerospikeClientProvider, MGI_ID, ANONYMOUS_USER_ID, customGameConfigBuilder, null);
			} catch (IOException e) {
				errorCollector.addError(e);
				LOGGER.error("Failed to prepare data", e);
			}
		};
		
		final GameJsonBuilder gameBuilder = createGame(GAME_ID)
				.withGameType(GameType.DUEL);
		final GameRegisterJsonBuilder gameRegisterBuilder = registerForMultiplayerGame(MGI_ID);
				
		assertCustomizedGameRegisterAndStart(gameRegisterBuilder, gameBuilder, customNumberOfQuestions, customPoolIds, prepareData);
	}
	
	@Test
	public void testDuelPlayWithCustomPoolsAndNumberOfQuestions() throws Exception {
		final int customNumberOfQuestions = 7;
		final String[] customPoolIds = new String[] { TestDataLoader.ASSIGNED_POOLS[0] };
		final CustomGameConfigBuilder customGameConfigBuilder = TestCustomGameConfigBuilder.create(ANONYMOUS_USER_ID)
				.withPools(customPoolIds)
				.withNumberOfQuestions(customNumberOfQuestions);
		
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, true);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);
		testDataLoader.inviteFriends(aerospikeClientProvider, MGI_ID, ANONYMOUS_USER_ID, customGameConfigBuilder, null);
		
		playGameFlow(createGame(GAME_ID)
				.withGameType(GameType.DUEL), registerForMultiplayerGame(MGI_ID));
	}
	
	@Test
	public void testStartQuiz24WithCustomPoolsAndNumberOfQuestions() throws Exception {
		final int customNumberOfQuestions = NumberOfQuestionsRule.VALID_NUMBER_OF_QUESTIONS_DEFAULT.get(0);
		final String[] customPoolIds = new String[]{TestDataLoader.ASSIGNED_POOLS[0]};

		final SinglePlayerGameParameters singlePlayerGameConfig = new SinglePlayerGameParameters();
		singlePlayerGameConfig.setPoolIds(customPoolIds);
		singlePlayerGameConfig.setNumberOfQuestions(customNumberOfQuestions);
		
		final Runnable prepareData = () -> {
			try {
				aerospikeClient.close();
				testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
				testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);
			} catch (IOException e) {
				LOGGER.error("Failed to prepare data", e);
			}
		};
		
		final GameJsonBuilder gameBuilder = createGame(GAME_ID)
				.withGameType(GameType.QUIZ24);
		final GameRegisterJsonBuilder gameRegisterBuilder = registerForQuiz24(GAME_ID, singlePlayerGameConfig);
		
		assertCustomizedGameRegisterAndStart(gameRegisterBuilder, gameBuilder, customNumberOfQuestions, customPoolIds,
				prepareData);
	}
	
	@Test
	public void testRegisterForGameWithNoUser() throws IOException, URISyntaxException {
		final GameJsonBuilder gameBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(5);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);

		//QUIZ24
		final String registerForQuizWithNoUserJson = registerForQuiz24(GAME_ID, false)
				.byUser(new ClientInfo())
				.buildJson(jsonUtil);
		testDataLoader.prepareTestGameData(gameBuilder.withGameType(GameType.QUIZ24));
		assertResponseError(registerForQuizWithNoUserJson, ExceptionType.AUTH, ExceptionCodes.ERR_INSUFFICIENT_RIGHTS);
		
		//DUEL
		testDataLoader.inviteFriends(aerospikeClientProvider, MGI_ID, ANONYMOUS_USER_ID, TestCustomGameConfigBuilder.create(REGISTERED_USER_ID), null);
		final String registerForDuelWithNoUserJson = registerForMultiplayerGame(MGI_ID)
				.byUser(new ClientInfo())
				.buildJson(jsonUtil);
		testDataLoader.prepareTestGameData(gameBuilder.withGameType(GameType.DUEL));
		assertResponseError(registerForDuelWithNoUserJson, ExceptionType.AUTH, ExceptionCodes.ERR_INSUFFICIENT_RIGHTS);
	}
	
	@Test
	public void testRegisterForExpiredGame() throws IOException, URISyntaxException{
		final GameJsonBuilder gameBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(5)
				.withGameType(GameType.QUIZ24)
				.withStartDateTime(ZonedDateTime.of(DateTimeUtil.getCurrentDateTime().minusDays(1).toLocalDate(),
						LocalTime.MIN, ZoneOffset.UTC))
				.withEndDateTime(ZonedDateTime.of(DateTimeUtil.getCurrentDateTime().minusDays(1).toLocalDate(),
						LocalTime.MAX, DateTimeUtil.TIMEZONE));
		
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);

		//QUIZ24
		testDataLoader.prepareTestGameData(gameBuilder.withGameType(GameType.QUIZ24));
		assertResponseError(registerForQuiz24(GAME_ID, false).buildJson(jsonUtil), ExceptionType.CLIENT, GameEngineExceptionCodes.ERR_GAME_NOT_AVAILABLE);
		
		//DUEL
		testDataLoader.inviteFriends(
				aerospikeClientProvider, MGI_ID, ANONYMOUS_USER_ID, TestCustomGameConfigBuilder
						.create(REGISTERED_USER_ID).withEndDateTime(DateTimeUtil.getCurrentDateTime().minusDays(1)),
				null);
		testDataLoader.prepareTestGameData(gameBuilder.withGameType(GameType.DUEL));
		assertResponseError(registerForMultiplayerGame(MGI_ID).buildJson(jsonUtil), ExceptionType.CLIENT, GameEngineExceptionCodes.ERR_GAME_NOT_AVAILABLE);
	} 

	@Test
	public void testStartGameByInvalidUserRoles() throws IOException, URISyntaxException {
		testDataLoader.prepareTestGameData(createGame(GAME_ID)
				.withNumberOfQuestions(5)
				.withGameType(GameType.QUIZ24));
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);

		final String registerWithInvalidRoles = registerForQuiz24(GAME_ID, false)
				.byUser(KeyStoreTestUtil.INVALID_ROLES_USER_ID)
				.buildJson(jsonUtil);
		assertResponseError(registerWithInvalidRoles, ExceptionType.AUTH, ExceptionCodes.ERR_INSUFFICIENT_RIGHTS);
	}
	
	@Test
	public void testPlayQuizFlow() throws Exception {
		// Game Data
		final GameJsonBuilder quizBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.QUIZ24)
				.withInstantAnswerFeedback()
				.withInstantAnswerFeedbackDelay(0);
		
		
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.createAdvertisementProvider(ADVERTISEMENT_PROVIDER_ID, 300);

		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		// Play quiz
		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));

		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, updatePlayedGameRequests));
		
		// Validate UpdatePlayedGameRequest
		UpdatePlayedGameRequest updatePlayedGameRequest = (UpdatePlayedGameRequest) updatePlayedGameRequests.get(0).getContent();
		assertNull(updatePlayedGameRequest.getMgiId());
		assertThat(updatePlayedGameRequest.getUserId(), equalTo(ANONYMOUS_USER_ID));
		
		PlayedGameInfo playedGameInfo = updatePlayedGameRequest.getPlayedGameInfo();
		assertThat(playedGameInfo.getGameId(), equalTo(GAME_ID));
		assertThat(playedGameInfo.getType(), equalTo(GameType.QUIZ24));
	}
	
	@Test
	public void testPlayDuelFlow() throws Exception {
		//Game Data
		final GameJsonBuilder duelBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.DUEL)
				.withInstantAnswerFeedback()
				.withInstantAnswerFeedbackDelay(0);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, true);
		testDataLoader.createAdvertisementProvider(ADVERTISEMENT_PROVIDER_ID, 300);
		
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);
		testDataLoader.createProfile(REGISTERED_USER_ID, REGISTERED_USER_HANDICAP);
		

		final List<Pair<String, String>> additionalInvitations = Arrays.asList(new ImmutablePair<String, String>(REGISTERED_USER_ID, ANONYMOUS_USER_ID));
		testDataLoader.inviteFriends(aerospikeClientProvider, MGI_ID, ANONYMOUS_USER_ID, 
				TestCustomGameConfigBuilder.createDuel(ANONYMOUS_USER_ID), additionalInvitations);
				
		//Play duel - user 1:ANONYMOUS_USER_ID
		playGameFlow(duelBuilder, registerForMultiplayerGame(MGI_ID));
		
		//Play duel - user 2:REGISTERED_USER_ID
		final GameRegisterJsonBuilder secondUserRegistrationBuilder = registerForMultiplayerGame(MGI_ID)
				.byUser(REGISTERED_CLIENT_INFO);
		playGameFlow(duelBuilder, secondUserRegistrationBuilder);
		
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, calculateMultiplayerResultsRequests));
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, updatePlayedGameRequests));
		
		// Validate UpdatePlayedGameRequest
		UpdatePlayedGameRequest updatePlayedGameRequest = (UpdatePlayedGameRequest) updatePlayedGameRequests.get(0).getContent();
		assertThat(updatePlayedGameRequest.getMgiId(), equalTo(MGI_ID));
	}
	
	@Test
	public void testPlayDuelWithAdvertisements() throws Exception{
		//Game Data
		final GameJsonBuilder duelBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(5)
				.withGameType(GameType.DUEL);
		final GameRegisterJsonBuilder registerJsonBuilder = registerForMultiplayerGame(MGI_ID);
		final AtomicBoolean hasAdvertisementProviderAeropsikeRecord = new AtomicBoolean(true);
		
		final Runnable prepareData = () -> {
			aerospikeClient.close();
			try {
				testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, true);		
				if (hasAdvertisementProviderAeropsikeRecord.get()) {
					testDataLoader.createAdvertisementProvider(ADVERTISEMENT_PROVIDER_ID, 300);
				}
				testDataLoader.inviteFriends(aerospikeClientProvider, MGI_ID, ANONYMOUS_USER_ID, 
						TestCustomGameConfigBuilder.createDuel(ANONYMOUS_USER_ID), null);
				testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);
			} catch (IOException e) {
				LOGGER.error("Failed to prepare data", e);
			}
		};

		// Play duel with advertisement BEFORE_GAME
		prepareData.run();
		playGameFlow(duelBuilder.withAdvertisement(ADVERTISEMENT_PROVIDER_ID, AdvertisementFrequency.BEFORE_GAME), 
				registerJsonBuilder);

		// Play duel with advertisement AFTER_GAME
		prepareData.run();
		playGameFlow(duelBuilder.withAdvertisement(ADVERTISEMENT_PROVIDER_ID, AdvertisementFrequency.AFTER_GAME),
				registerJsonBuilder);

		// Play duel with advertisement AFTER_EACH_QUESTION
		prepareData.run();
		playGameFlow(duelBuilder.withAdvertisement(ADVERTISEMENT_PROVIDER_ID, AdvertisementFrequency.AFTER_EACH_QUESTION),
				registerJsonBuilder);

		// Play duel with advertisement AFTER_EVERY_X_QUESTION
		prepareData.run();
		playGameFlow(duelBuilder.withAdvertisement(ADVERTISEMENT_PROVIDER_ID, 1), registerJsonBuilder);
		prepareData.run();
		playGameFlow(duelBuilder.withAdvertisement(ADVERTISEMENT_PROVIDER_ID, 2), registerJsonBuilder);
		prepareData.run();
		playGameFlow(duelBuilder.withAdvertisement(ADVERTISEMENT_PROVIDER_ID, 3), registerJsonBuilder);
		prepareData.run();
		playGameFlow(duelBuilder.withAdvertisement(ADVERTISEMENT_PROVIDER_ID, 4), registerJsonBuilder);
		prepareData.run();
		playGameFlow(duelBuilder.withAdvertisement(ADVERTISEMENT_PROVIDER_ID, 5), registerJsonBuilder);
		
		//empty advertisement provider
		hasAdvertisementProviderAeropsikeRecord.set(false);
		prepareData.run();
		playGameFlow(duelBuilder.withAdvertisement(ADVERTISEMENT_PROVIDER_ID, AdvertisementFrequency.BEFORE_GAME), registerJsonBuilder);
	}
	
	private void assertDuelPlayWithEntryfee(EntryFee intialEntryFeee, EntryFee customEntryFee, EntryFee expectedEntryFee, boolean entryFeeDecidedByPlayer, 
			ClientInfo firstPlayer, ClientInfo secondPlayer) throws Exception {
		//Game Data
		final GameJsonBuilder duelBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.DUEL)
				.withEntryFee(intialEntryFeee)
				.withInstantAnswerFeedback()
				.withInstantAnswerFeedbackDelay(0)
				.withEntryFeeDecidedByPlayer(entryFeeDecidedByPlayer);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, true);
		
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);
		testDataLoader.createProfile(REGISTERED_USER_ID, REGISTERED_USER_HANDICAP);

		final String mgiId = UUID.randomUUID().toString();
		//invites
		final List<Pair<String, String>> additionalInvitations = Arrays.asList(new ImmutablePair<String, String>(secondPlayer.getUserId(), firstPlayer.getUserId()));
		CustomGameConfigBuilder customGameConfig = TestCustomGameConfigBuilder.createDuel(firstPlayer.getUserId())
				.withEntryFee(customEntryFee);
		testDataLoader.inviteFriends(aerospikeClientProvider, mgiId, firstPlayer.getUserId(), 
				customGameConfig, additionalInvitations);				
		
		//Play duel - user 1
		final String user1GameInstanceId = playGameFlow(duelBuilder, registerForMultiplayerGame(mgiId)
				.byUser(firstPlayer));
		
		//Play duel - user 2
		final String user2GameInstanceId = playGameFlow(duelBuilder, registerForMultiplayerGame(mgiId)
				.byUser(secondPlayer));
		
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, calculateMultiplayerResultsRequests));
		
		if (expectedEntryFee != null) {//assert PAID
			assertEquals(GAME_FEE_TRANSACTION_ID,
					gameInstanceDao.getGameInstance(user1GameInstanceId).getGameState().getEntryFeeTransactionId());
			assertEquals(GAME_FEE_TRANSACTION_ID,
					gameInstanceDao.getGameInstance(user2GameInstanceId).getGameState().getEntryFeeTransactionId());

			final List<Record> transactionLogs = testDataLoader.getTransactionLogs();
			F4MAssert.assertSize(2, transactionLogs);
			transactionLogs.forEach(t -> assertEquals(GAME_FEE_TRANSACTION_ID, t.getString(TransactionLogAerospikeDao.BIN_NAME_TRANSACTION_ID)));
			transactionLogs.forEach(t -> assertEquals(TransactionStatus.COMPLETED.name(), t.getString(TransactionLogAerospikeDao.BIN_NAME_STATUS)));
			transactionLogs.forEach(t -> assertEquals(expectedEntryFee.getEntryFeeAmount().toString(), t.getString(TransactionLogAerospikeDao.BIN_NAME_AMOUNT)));
			transactionLogs.forEach(t -> assertEquals(expectedEntryFee.getEntryFeeCurrency().name(), t.getString(TransactionLogAerospikeDao.BIN_NAME_CURRENCY)));

			//Assert game instance has entry fee
			//--user1
			assertEquals(expectedEntryFee.getEntryFeeAmount(), gameInstanceDao.getGameInstance(user1GameInstanceId).getEntryFeeAmount());
			assertEquals(expectedEntryFee.getEntryFeeCurrency(), gameInstanceDao.getGameInstance(user1GameInstanceId).getEntryFeeCurrency());
			//--user2
			assertEquals(expectedEntryFee.getEntryFeeAmount(), gameInstanceDao.getGameInstance(user2GameInstanceId).getEntryFeeAmount());
			assertEquals(expectedEntryFee.getEntryFeeCurrency(), gameInstanceDao.getGameInstance(user2GameInstanceId).getEntryFeeCurrency());
		} else {//assert FREE
			assertNull(gameInstanceDao.getGameInstance(user1GameInstanceId).getGameState().getEntryFeeTransactionId());
			assertNull(gameInstanceDao.getGameInstance(user2GameInstanceId).getGameState().getEntryFeeTransactionId());
			
			//Assert game instance has no entry fee
			//--user1
			assertNull(gameInstanceDao.getGameInstance(user1GameInstanceId).getEntryFeeAmount());
			assertNull(gameInstanceDao.getGameInstance(user1GameInstanceId).getEntryFeeCurrency());
			//--user2
			assertNull(gameInstanceDao.getGameInstance(user2GameInstanceId).getEntryFeeAmount());
			assertNull(gameInstanceDao.getGameInstance(user2GameInstanceId).getEntryFeeCurrency());
		}
		calculateMultiplayerResultsRequests.clear();
		testDataLoader.clearTransactionLog();
	}
	
	@Test
	public void testPlayDuelWithCustomEntryfee() throws Exception {		
		final EntryFee intialEntryFee = testDataLoader.createEntryFee(new BigDecimal("0.99"), Currency.BONUS);
		final EntryFee anyEntryFee = testDataLoader.createEntryFee(new BigDecimal("500"), Currency.BONUS);
		final EntryFee customEntryFee = testDataLoader.createEntryFee(new BigDecimal("5.99"), Currency.BONUS);
		final EntryFee zeroEntryFee = testDataLoader.createEntryFee(new BigDecimal("0.00"), Currency.BONUS);

		//Customize paid game if cannot decide entry fee
		assertDuelPlayWithEntryfee(intialEntryFee, anyEntryFee, intialEntryFee, false, ANONYMOUS_CLIENT_INFO,
				REGISTERED_CLIENT_INFO);
		aerospikeClient.close();
		
		//Customize free game if cannot decide entry fee
		assertDuelPlayWithEntryfee(null, anyEntryFee, null, false, ANONYMOUS_CLIENT_INFO,
				REGISTERED_CLIENT_INFO);
		aerospikeClient.close();
		
		//Customize entry fee
		assertDuelPlayWithEntryfee(intialEntryFee, customEntryFee, customEntryFee, true, ANONYMOUS_CLIENT_INFO,
				REGISTERED_CLIENT_INFO);
		aerospikeClient.close();
		
		//PAID -> FREE
		assertDuelPlayWithEntryfee(intialEntryFee, zeroEntryFee, null, true, ANONYMOUS_CLIENT_INFO,
				REGISTERED_CLIENT_INFO);
		aerospikeClient.close();
		
		//FREE -> PAID
		assertDuelPlayWithEntryfee(null, customEntryFee, customEntryFee, true, ANONYMOUS_CLIENT_INFO,
				REGISTERED_CLIENT_INFO);
		aerospikeClient.close();
		
		assertDuelPlayWithEntryfee(null, zeroEntryFee, null, true, ANONYMOUS_CLIENT_INFO,
				REGISTERED_CLIENT_INFO);
		aerospikeClient.close();
	}
	
	@Test
	public void testPlayDuelFlowWithSingleUserCancelation() throws Exception{
		ResultConfiguration resultConfiguration = new ResultConfiguration();
		resultConfiguration.setProperty(ResultConfiguration.PROPERTY_SPECIAL_PRIZE, Boolean.TRUE);
		resultConfiguration.setProperty(ResultConfiguration.PROPERTY_SPECIAL_PRIZE_VOUCHER_ID, VOUCHER_ID);

		//Game Data
		final GameJsonBuilder duelBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.DUEL)
				.withResultConfiguration(resultConfiguration);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, true);
		
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);
		
		final List<Pair<String, String>> additionalInvitations = Arrays.asList(new ImmutablePair<String, String>(REGISTERED_USER_ID, ANONYMOUS_USER_ID));
		testDataLoader.inviteFriends(aerospikeClientProvider, MGI_ID, ANONYMOUS_USER_ID, 
				TestCustomGameConfigBuilder.createDuel(ANONYMOUS_USER_ID), additionalInvitations);
				
		//Play duel - user 1
		playGameFlow(duelBuilder, registerForMultiplayerGame(MGI_ID));
		
		//Play duel - user 2
		final String user2GameInstance = requestRegistrationForGame(registerForMultiplayerGame(MGI_ID)
				.byUser(REGISTERED_CLIENT_INFO), GAME_ID);
		requestGameCancellation(user2GameInstance, REGISTERED_CLIENT_INFO);
		
		// no continue for playing
		assertCancelledGameCannotContinue(user2GameInstance);

		UserVoucherReleaseRequest reserveVouherRequest = mockServiceReceivedMessageCollector
				.<UserVoucherReleaseRequest>getMessageByType(VoucherMessageTypes.USER_VOUCHER_RELEASE)
				.getContent();
		assertEquals(VOUCHER_ID, reserveVouherRequest.getVoucherId());

		//history
		assertEquals(GameStatus.CANCELLED, testDataLoader.getGameHistory(REGISTERED_USER_ID, user2GameInstance).getStatus());
		assertNull(testDataLoader.getGameHistory(REGISTERED_USER_ID, user2GameInstance).getEndStatus());
        assertEquals(GameStatus.CANCELLED, gameInstanceDao.getGameInstance(user2GameInstance).getGameState().getGameStatus());
        assertNull(gameInstanceDao.getGameInstance(user2GameInstance).getGameState().getGameEndStatus());
        
        assertEquals(RefundReason.GAME_NOT_PLAYED.name(), gameInstanceDao.getGameInstance(user2GameInstance).getGameState().getRefundReason());
        assertTrue(gameInstanceDao.getGameInstance(user2GameInstance).getGameState().isRefundable());
		
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, calculateMultiplayerResultsRequests));
	}
	
	@Test
	public void testRegisterWithInsufficientFunds() throws Exception {
		errorCollector.setExpectedErrors(new F4MPaymentClientException(PaymentServiceExceptionCodes.ERR_INSUFFICIENT_FUNDS, "anyMessage"));
		
		paymentShouldReturnError.set(true);
		//Game Data
		final GameJsonBuilder gameBuilder = createGame(GAME_ID)
			.withNumberOfQuestions(2)
			.withGameType(GameType.QUIZ24)
			.withEntryFee(new BigDecimal("100"), Currency.MONEY);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP, 20);
		
		//prepare game
		testDataLoader.prepareTestGameData(gameBuilder);		

		//Register for the game
		final String registerForMoneyGame = registerForQuiz24(GAME_ID, false)
				.byUser(ANONYMOUS_USER_ID, UserRole.FULLY_REGISTERED_BANK.name())
				.buildJson(jsonUtil);
		
		assertResponseError(registerForMoneyGame, ExceptionType.CLIENT,
				PaymentServiceExceptionCodes.ERR_INSUFFICIENT_FUNDS);
		
		final List<Record> transactionLogs = testDataLoader.getTransactionLogs();
		F4MAssert.assertSize(1, transactionLogs);
		
		final Record transactionLogAerospikeRecord = transactionLogs.get(0);
		assertNull(transactionLogAerospikeRecord.getString(TransactionLogAerospikeDao.BIN_NAME_TRANSACTION_ID));
		assertEquals(TransactionStatus.ERROR.name(), transactionLogAerospikeRecord.getString(TransactionLogAerospikeDao.BIN_NAME_STATUS));
	}
	
	@Ignore //FIXME: END_GAME_RESPONSE should not be sent via GameEngine in case of error received from other service
	@Test
	public void testFailedGameEnd() throws Exception{
		errorCollector.setExpectedErrors(new F4MFatalErrorException("anyMessage"));
		
		resultEngineShouldReturnErrorOnCalculateResults.set(true);
		//Game Data
		final GameJsonBuilder duelBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.DUEL)
				.withInstantAnswerFeedback()
				.withInstantAnswerFeedbackDelay(0);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, true);
		
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		final List<Pair<String, String>> additionalInvitations = Arrays.asList(new ImmutablePair<String, String>(REGISTERED_USER_ID, ANONYMOUS_USER_ID));
		testDataLoader.inviteFriends(aerospikeClientProvider, MGI_ID, ANONYMOUS_USER_ID, 
				TestCustomGameConfigBuilder.createDuel(ANONYMOUS_USER_ID), additionalInvitations);

		//Register for the game
		playGameFlow(duelBuilder, registerForMultiplayerGame(MGI_ID));
		
		final List<Record> transactionLogs = testDataLoader.getTransactionLogs();
		F4MAssert.assertSize(1, transactionLogs);
		
		final Record transactionLogAerospikeRecord = transactionLogs.get(0);
		assertNull(transactionLogAerospikeRecord.getString(TransactionLogAerospikeDao.BIN_NAME_TRANSACTION_ID));
		assertEquals(TransactionStatus.ERROR.name(), transactionLogAerospikeRecord.getString(TransactionLogAerospikeDao.BIN_NAME_STATUS));
		
		final List<String> gameHistoryList = commonGameHistoryDao.getUserGameHistory(ANONYMOUS_USER_ID);
		F4MAssert.assertSize(1, gameHistoryList);
		final GameHistory gameHistory = new GameHistory(jsonUtil.fromJson(gameHistoryList.get(0), JsonObject.class));
		assertEquals(GameEndStatus.CALCULATING_RESULTS_FAILED, gameHistory.getEndStatus());
		assertNotNull(gameHistory.getGameInstanceId());
		
		assertEquals(GameEndStatus.CALCULATING_RESULTS_FAILED,
				gameInstanceDao.getGameInstance(gameHistory.getGameInstanceId()).getGameState().getGameEndStatus());
		assertNull(testDataLoader.readActiveGameInstance(gameHistory.getGameInstanceId()));
	}
	
	@Test
	public void testFailedStartGame() throws Exception{		
		//Game Data
		final GameJsonBuilder duelBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.DUEL)
				.withEntryFee(new BigDecimal("10"), Currency.BONUS);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, true);
		
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);
		testDataLoader.createProfile(REGISTERED_USER_ID, KeyStoreTestUtil.REGISTERED_USER_HANDICAP);

		final List<Pair<String, String>> additionalInvitations = Arrays.asList(new ImmutablePair<String, String>(REGISTERED_USER_ID, ANONYMOUS_USER_ID));
		testDataLoader.inviteFriends(aerospikeClientProvider, MGI_ID, ANONYMOUS_USER_ID, 
				TestCustomGameConfigBuilder.createDuel(ANONYMOUS_USER_ID), additionalInvitations);

		//Play by first user: ANONYMOUS
		playGameFlow(duelBuilder, registerForMultiplayerGame(MGI_ID)
				.byUser(ANONYMOUS_CLIENT_INFO));
		
		//Failure by second user: REGISTERED
		final String secondUserGameInstanceId = requestRegistrationForGame(registerForMultiplayerGame(MGI_ID)
				.byUser(REGISTERED_CLIENT_INFO), GAME_ID);
		testDataLoader.removeAllQuestions();
		
		GameStartJsonBuilder gameStartJsonBuilder = startGame(secondUserGameInstanceId)
				.byUser(REGISTERED_CLIENT_INFO);
		
		assertResponseError(gameStartJsonBuilder.buildJson(jsonUtil), ExceptionType.SERVER, ExceptionCodes.ERR_FATAL_ERROR);
		
		final GameInstance secondUserGameInstance = gameInstanceDao.getGameInstance(secondUserGameInstanceId);
		final GameState secondUserGameState = secondUserGameInstance.getGameState();
		assertEquals(GameStatus.CANCELLED, secondUserGameState.getGameStatus());
		assertEquals(GameEndStatus.TERMINATED, secondUserGameState.getGameEndStatus());
		assertEquals(CloseUpReason.BACKEND_FAILS, secondUserGameState.getCloseUpReason());
		assertNotNull(secondUserGameState.getCloseUpErrorMessage());
		assertTrue(secondUserGameState.isRefundable());
		assertEquals(RefundReason.BACKEND_FAILED.name(), secondUserGameState.getRefundReason());
		
		final List<Record> transactionLogs = testDataLoader.getTransactionLogs();
		F4MAssert.assertSize(2, transactionLogs);
	}

	@Test
	public void testPlayQuizFlowNoGamblingCountry() throws Exception {
		// Game Data
		final GameJsonBuilder quizBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.QUIZ24)
				.withInstantAnswerFeedback()
				.withInstantAnswerFeedbackDelay(0);
		
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.createAdvertisementProvider(ADVERTISEMENT_PROVIDER_ID, 300);

		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		// Play quiz
		GameRegisterJsonBuilder quizBuilderForGame = registerForQuiz24(GAME_ID, false);
		quizBuilderForGame.getClientInfo().setCountryCode(ISOCountry.valueOf("SA"));

		final ClientInfo clientInfo = quizBuilderForGame.getClientInfo();
		testDataLoader.prepareTestGameData(quizBuilder);
		
		//Register for the game
		final String gameInstanceId = requestRegistrationForGame(quizBuilderForGame, GAME_ID);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), 
				startGame(gameInstanceId).byUser(clientInfo).buildJson(jsonUtil));
		assertReceivedMessagesWithWait(GameEngineMessageTypes.START_GAME_RESPONSE);
		
		final JsonMessage<StartGameResponse> startGameResponseMessage = testClientReceivedMessageCollector
				.getMessageByType(GameEngineMessageTypes.START_GAME_RESPONSE);
		assertMessageContentType(startGameResponseMessage, StartGameResponse.class);

		testClientReceivedMessageCollector.clearReceivedMessageList();
		final StartGameResponse startGameResponse = (StartGameResponse) startGameResponseMessage.getContent();
		assertEquals(0, startGameResponse.getWinningComponentIds().length);

	}

	@Test
	public void testJokersAvailableCall() throws IOException, Exception {

		JokerConfiguration[] jokersConfiguration = getJokerConfiguration();

		final GameJsonBuilder gameBuilder = createGame(GAME_ID)
				.withGameType(GameType.QUIZ24)
				.withJokersConfiguration(jokersConfiguration)
				.withNumberOfQuestions(5);

		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.prepareTestGameData(gameBuilder);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		final String gameInstanceId = requestRegistrationForGame(registerForQuiz24(GAME_ID, false), GAME_ID);
		requestGameStart(startGame(gameInstanceId), false);

		requestReadyToPlay(ANONYMOUS_CLIENT_INFO, GAME_ID, gameInstanceId);
		final String jokersAvailableJson = testDataLoader.getJokersAvailableJson(gameInstanceId, ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), jokersAvailableJson);


		assertReceivedMessagesWithWait(GameEngineMessageTypes.JOKERS_AVAILABLE_RESPONSE);

		final JsonMessage<JokersAvailableResponse> jokersAvailableResponse = testClientReceivedMessageCollector
				.getMessageByType(GameEngineMessageTypes.JOKERS_AVAILABLE_RESPONSE);
		assertMessageContentType(jokersAvailableResponse, JokersAvailableResponse.class);

		assertAvailableJokers(jokersAvailableResponse.getContent());
	}

	@Test
	public void testPurchaseHintCall() throws IOException, Exception {

		JokerConfiguration[] jokersConfiguration = getJokerConfiguration();
		Stream.of(jokersConfiguration).forEach(conf ->  {conf.setPrice(PRICE); conf.setCurrency(CURRENCY);});
		final GameJsonBuilder gameBuilder = createGame(GAME_ID)
				.withGameType(GameType.QUIZ24)
				.withJokersConfiguration(jokersConfiguration)
				.withNumberOfQuestions(5);

		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.prepareTestGameData(gameBuilder);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		final String gameInstanceId = requestRegistrationForGame(registerForQuiz24(GAME_ID, false), GAME_ID);
		requestGameStart(startGame(gameInstanceId), false);

		requestReadyToPlay(ANONYMOUS_CLIENT_INFO, GAME_ID, gameInstanceId);

		final String purchaseHintJson = testDataLoader.getPurchaseHintJson(gameInstanceId, ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), purchaseHintJson);

		assertReceivedMessagesWithWait(GameEngineMessageTypes.PURCHASE_HINT_RESPONSE);

		final JsonMessage<PurchaseHintResponse> response = testClientReceivedMessageCollector
				.getMessageByType(GameEngineMessageTypes.PURCHASE_HINT_RESPONSE);
		assertMessageContentType(response, PurchaseHintResponse.class);

		// Hints are defined in questions.json
		assertTrue(response.getContent().getHint().contains("hint"));

		final GameInstance gameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		final Map<JokerType, Set<Integer>> usedJokers = gameInstance.getJokersUsed();
		assertEquals(1, usedJokers.get(JokerType.HINT).size());
		assertTrue(usedJokers.get(JokerType.HINT).contains(0));

		final List<Record> transactionLogs = testDataLoader.getTransactionLogs();
		F4MAssert.assertSize(1, transactionLogs);
		Record transaction = transactionLogs.get(0);
		assertEquals(JOKER_PURCHASE_TRANSACTION_ID, transaction.getString(TransactionLogAerospikeDao.BIN_NAME_TRANSACTION_ID));
		assertEquals(TransactionStatus.COMPLETED.name(), transaction.getString(TransactionLogAerospikeDao.BIN_NAME_STATUS));
		assertEquals(PRICE.negate().toString(), transaction.getString(TransactionLogAerospikeDao.BIN_NAME_AMOUNT));
		assertEquals(CURRENCY.name(), transaction.getString(TransactionLogAerospikeDao.BIN_NAME_CURRENCY));
		assertTrue(transaction.getString(TransactionLogAerospikeDao.BIN_NAME_REASON).startsWith(PURCHASE_JOKER_REASON));
		testDataLoader.clearTransactionLog();
	}

	@Test
	public void testPurchaseJokerSkipCall() throws IOException, Exception {
		JokerConfiguration[] jokersConfiguration = getJokerConfiguration();
		Stream.of(jokersConfiguration).forEach(conf ->  {conf.setPrice(PRICE); conf.setCurrency(CURRENCY);});
		final GameJsonBuilder gameBuilder = createGame(GAME_ID)
				.withGameType(GameType.QUIZ24)
				.withJokersConfiguration(jokersConfiguration)
				.withNumberOfQuestions(5);

		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.prepareTestGameData(gameBuilder);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		final String gameInstanceId = requestRegistrationForGame(registerForQuiz24(GAME_ID, false), GAME_ID);
		requestGameStart(startGame(gameInstanceId), false);

		requestReadyToPlay(ANONYMOUS_CLIENT_INFO, GAME_ID, gameInstanceId);

		// check question number :
		final GameInstance initialInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		assertEquals(0, initialInstance.getGameState().getCurrentQuestionStep().getQuestion());

		final String purchaseJokerSkipJson = testDataLoader.getPurchaseSkipJson(gameInstanceId, ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), purchaseJokerSkipJson);
		assertReceivedMessagesWithWait(GameEngineMessageTypes.PURCHASE_SKIP_RESPONSE);

		final JsonMessage<EmptyJsonMessageContent> response = testClientReceivedMessageCollector
				.getMessageByType(GameEngineMessageTypes.PURCHASE_SKIP_RESPONSE);
		assertMessageContentType(response, EmptyJsonMessageContent.class);

		final List<Record> transactionLogs = testDataLoader.getTransactionLogs();
		F4MAssert.assertSize(1, transactionLogs);
		Record transaction = transactionLogs.get(0);
		assertEquals(JOKER_PURCHASE_TRANSACTION_ID, transaction.getString(TransactionLogAerospikeDao.BIN_NAME_TRANSACTION_ID));
		assertEquals(TransactionStatus.COMPLETED.name(), transaction.getString(TransactionLogAerospikeDao.BIN_NAME_STATUS));
		assertEquals(PRICE.negate().toString(), transaction.getString(TransactionLogAerospikeDao.BIN_NAME_AMOUNT));
		assertEquals(CURRENCY.name(), transaction.getString(TransactionLogAerospikeDao.BIN_NAME_CURRENCY));
		assertTrue(transaction.getString(TransactionLogAerospikeDao.BIN_NAME_REASON).startsWith(PURCHASE_JOKER_REASON));
		testDataLoader.clearTransactionLog();

		// check question number after skip purchased :
		final GameInstance gameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		assertEquals(1, gameInstance.getGameState().getCurrentQuestionStep().getQuestion());
	}

	@Test
	public void testPurchaseJokerFiftyFiftyCall() throws IOException, Exception {
		JokerConfiguration[] jokersConfiguration = getJokerConfiguration();
		Stream.of(jokersConfiguration).forEach(conf ->  {conf.setPrice(PRICE); conf.setCurrency(CURRENCY);});
		final GameJsonBuilder gameBuilder = createGame(GAME_ID)
				.withGameType(GameType.QUIZ24)
				.withJokersConfiguration(jokersConfiguration)
				.withNumberOfQuestions(5);

		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.prepareTestGameData(gameBuilder);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		final String gameInstanceId = requestRegistrationForGame(registerForQuiz24(GAME_ID, false), GAME_ID);
		requestGameStart(startGame(gameInstanceId), false);

		requestReadyToPlay(ANONYMOUS_CLIENT_INFO, GAME_ID, gameInstanceId);

		final String freeJokerFiftyFifty = testDataLoader.getPurchaseFiftyFiftyJson(gameInstanceId, ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), freeJokerFiftyFifty);
		assertReceivedMessagesWithWait(GameEngineMessageTypes.PURCHASE_FIFTY_FIFTY_RESPONSE);

		final JsonMessage<PurchaseFiftyFiftyResponse> response = testClientReceivedMessageCollector
				.getMessageByType(GameEngineMessageTypes.PURCHASE_FIFTY_FIFTY_RESPONSE);
		assertMessageContentType(response, PurchaseFiftyFiftyResponse.class);

		assertEquals(2, response.getContent().getRemovedAnswers().length);
		final List<Record> transactionLogs = testDataLoader.getTransactionLogs();
		F4MAssert.assertSize(1, transactionLogs);
		Record transaction = transactionLogs.get(0);
		assertEquals(JOKER_PURCHASE_TRANSACTION_ID, transaction.getString(TransactionLogAerospikeDao.BIN_NAME_TRANSACTION_ID));
		assertEquals(TransactionStatus.COMPLETED.name(), transaction.getString(TransactionLogAerospikeDao.BIN_NAME_STATUS));
		assertEquals(PRICE.negate().toString(), transaction.getString(TransactionLogAerospikeDao.BIN_NAME_AMOUNT));
		assertEquals(CURRENCY.name(), transaction.getString(TransactionLogAerospikeDao.BIN_NAME_CURRENCY));
		assertTrue(transaction.getString(TransactionLogAerospikeDao.BIN_NAME_REASON).startsWith(PURCHASE_JOKER_REASON));
		testDataLoader.clearTransactionLog();
	}

	@Test
	public void testUseFreeJokerFiftyFiftyCall() throws IOException, Exception {
		JokerConfiguration[] jokersConfiguration = getJokerConfiguration();
		final GameJsonBuilder gameBuilder = createGame(GAME_ID)
				.withGameType(GameType.QUIZ24)
				.withJokersConfiguration(jokersConfiguration)
				.withNumberOfQuestions(5);

		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.prepareTestGameData(gameBuilder);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		final String gameInstanceId = requestRegistrationForGame(registerForQuiz24(GAME_ID, false), GAME_ID);
		requestGameStart(startGame(gameInstanceId), false);

		requestReadyToPlay(ANONYMOUS_CLIENT_INFO, GAME_ID, gameInstanceId);

		final String freeJokerFiftyFifty = testDataLoader.getPurchaseFiftyFiftyJson(gameInstanceId, ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), freeJokerFiftyFifty);

		assertReceivedMessagesWithWait(GameEngineMessageTypes.PURCHASE_FIFTY_FIFTY_RESPONSE);

		final JsonMessage<PurchaseFiftyFiftyResponse> response = testClientReceivedMessageCollector
				.getMessageByType(GameEngineMessageTypes.PURCHASE_FIFTY_FIFTY_RESPONSE);
		assertMessageContentType(response, PurchaseFiftyFiftyResponse.class);

		assertEquals(2, response.getContent().getRemovedAnswers().length);

		final List<Record> transactionLogs = testDataLoader.getTransactionLogs();
		F4MAssert.assertSize(0, transactionLogs);
	}


	@Test
	public void testQuizSpecialGame() throws Exception {
		// Game Data
		final GameJsonBuilder quizBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.QUIZ24)
				.withGameTypeConfiguration(new GameTypeConfiguration(null, null, new SingleplayerGameTypeConfigurationData(true, 0, "")))
				.withInstantAnswerFeedback()
				.withSpecialGame(3)
				.withInstantAnswerFeedbackDelay(0);
		
		
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.createAdvertisementProvider(ADVERTISEMENT_PROVIDER_ID, 300);

		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		// Play quiz
		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));
		
		Game gameForTest = new Game();
		gameForTest.setGameId(GAME_ID);
		Long specialGame = userSpecialGameDao.getUserGameCountLeft(gameForTest, ANONYMOUS_USER_ID);
		assertEquals(2, specialGame.intValue());
		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));
		Long specialGame2 = userSpecialGameDao.getUserGameCountLeft(gameForTest, ANONYMOUS_USER_ID);
		assertEquals(1, specialGame2.intValue());
	}
	
	@Test
	public void testPaidQuiz24SpecialGame() throws Exception {
		final GameJsonBuilder gameJsonBuilder = createGame(GAME_ID)
				.withEntryFee(new BigDecimal("60"), Currency.BONUS)
				.withGameType(GameType.QUIZ24)
				.withGameTypeConfiguration(new GameTypeConfiguration(null, null, new SingleplayerGameTypeConfigurationData(true, 0, "")))
				.withSpecialGame(3)
				.withNumberOfQuestions(5);
		
		testDataLoader.prepareTestGameData(gameJsonBuilder);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);
		
		final String gameInstanceId = requestRegistrationForGame(registerForQuiz24(GAME_ID, false), GAME_ID);
		requestGameStart(startGame(gameInstanceId), false);

		Game gameForTest = new Game();
		gameForTest.setGameId(GAME_ID);
		Long specialGame1 = userSpecialGameDao.getUserGameCountLeft(gameForTest, ANONYMOUS_USER_ID);
		assertEquals(2, specialGame1.intValue());
	}
	
	
	@Test
	public void testQuizSpecialGamePaidTwoTimes() throws Exception {
		// Game Data
		final GameJsonBuilder quizBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.QUIZ24)
				.withGameTypeConfiguration(new GameTypeConfiguration(null, null, new SingleplayerGameTypeConfigurationData(true, 0, "")))
				.withInstantAnswerFeedback()
				.withEntryFee(new BigDecimal("60"), Currency.BONUS)
				.withSpecialGame(3)
				.withInstantAnswerFeedbackDelay(0);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.createAdvertisementProvider(ADVERTISEMENT_PROVIDER_ID, 300);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		// Play quiz
		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));
		
		Game gameForTest = new Game();
		gameForTest.setGameId(GAME_ID);
		Long specialGame = userSpecialGameDao.getUserGameCountLeft(gameForTest, ANONYMOUS_USER_ID);
		assertEquals(2, specialGame.intValue());
		
		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));
		Long specialGame2 = userSpecialGameDao.getUserGameCountLeft(gameForTest, ANONYMOUS_USER_ID);
		assertEquals(1, specialGame2.intValue());
	}
	
	@Test
	public void testPlayQuizFlowTwoTimes() throws Exception {
		// Game Data
		final GameJsonBuilder quizBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.QUIZ24)
				.withMultiplePurchaseAllowed(false)
				.withInstantAnswerFeedbackDelay(0);
		
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.createAdvertisementProvider(ADVERTISEMENT_PROVIDER_ID, 300);

		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		// Play quiz
		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));
		
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, updatePlayedGameRequests));
		
		// Validate UpdatePlayedGameRequest
		UpdatePlayedGameRequest updatePlayedGameRequest = (UpdatePlayedGameRequest) updatePlayedGameRequests.get(0).getContent();
		assertNull(updatePlayedGameRequest.getMgiId());
		assertThat(updatePlayedGameRequest.getUserId(), equalTo(ANONYMOUS_USER_ID));
		
		PlayedGameInfo playedGameInfo = updatePlayedGameRequest.getPlayedGameInfo();
		assertThat(playedGameInfo.getGameId(), equalTo(GAME_ID));
		assertThat(playedGameInfo.getType(), equalTo(GameType.QUIZ24));
		
		// Play quiz
		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));
		
		UpdatePlayedGameRequest updatePlayedGameRequest2 = (UpdatePlayedGameRequest) updatePlayedGameRequests.get(1).getContent();
		assertNull(updatePlayedGameRequest2.getMgiId());
		assertThat(updatePlayedGameRequest2.getUserId(), equalTo(ANONYMOUS_USER_ID));
		
		PlayedGameInfo playedGameInfo2 = updatePlayedGameRequest.getPlayedGameInfo();
		assertThat(playedGameInfo2.getGameId(), equalTo(GAME_ID));
		assertThat(playedGameInfo2.getType(), equalTo(GameType.QUIZ24));
		
	}
	
	@Test
	public void testPlayQuizFlowWithZeroGamesTwoTimes() throws Exception {
		// Game Data
		final GameJsonBuilder quizBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.QUIZ24)
				.withInstantAnswerFeedback()
				.withMultipleFeeGame(0)
				.withMultiplePurchaseAllowed(false)
				.withInstantAnswerFeedbackDelay(0);
		
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.createAdvertisementProvider(ADVERTISEMENT_PROVIDER_ID, 300);

		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		// Play quiz
		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));
		
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, updatePlayedGameRequests));
		
		// Validate UpdatePlayedGameRequest
		UpdatePlayedGameRequest updatePlayedGameRequest = (UpdatePlayedGameRequest) updatePlayedGameRequests.get(0).getContent();
		assertNull(updatePlayedGameRequest.getMgiId());
		assertThat(updatePlayedGameRequest.getUserId(), equalTo(ANONYMOUS_USER_ID));
		
		PlayedGameInfo playedGameInfo = updatePlayedGameRequest.getPlayedGameInfo();
		assertThat(playedGameInfo.getGameId(), equalTo(GAME_ID));
		assertThat(playedGameInfo.getType(), equalTo(GameType.QUIZ24));
		
		// Play quiz
		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));
		
		UpdatePlayedGameRequest updatePlayedGameRequest2 = (UpdatePlayedGameRequest) updatePlayedGameRequests.get(1).getContent();
		assertNull(updatePlayedGameRequest2.getMgiId());
		assertThat(updatePlayedGameRequest2.getUserId(), equalTo(ANONYMOUS_USER_ID));
		
		PlayedGameInfo playedGameInfo2 = updatePlayedGameRequest.getPlayedGameInfo();
		assertThat(playedGameInfo2.getGameId(), equalTo(GAME_ID));
		assertThat(playedGameInfo2.getType(), equalTo(GameType.QUIZ24));
	}
	
	
	@Test
	public void testQuizSpecialFreeGameRunOutOfTries() throws Exception {
		// Game Data
		final GameJsonBuilder quizBuilder = 
			createGame(GAME_ID)
			.withNumberOfQuestions(2)
			.withGameType(GameType.QUIZ24)
			.withGameTypeConfiguration(
						new GameTypeConfiguration(null, null, new SingleplayerGameTypeConfigurationData(true, 0, "")))
			.withInstantAnswerFeedback()
			.withMultipleFeeGame(2)
			.withMultiplePurchaseAllowed(false)
			.withInstantAnswerFeedbackDelay(0);

		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.createAdvertisementProvider(ADVERTISEMENT_PROVIDER_ID, 300);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		// Play quiz
		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));

		Game gameForTest = new Game();
		gameForTest.setGameId(GAME_ID);
		Long specialGame = userSpecialGameDao.getUserGameCountLeft(gameForTest, ANONYMOUS_USER_ID);
		assertEquals(1, specialGame.intValue());

		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));
		Long specialGame2 = userSpecialGameDao.getUserGameCountLeft(gameForTest, ANONYMOUS_USER_ID);
		assertEquals(0, specialGame2.intValue());

		GameRegisterJsonBuilder gameRegisterJsonBuilder = registerForQuiz24(GAME_ID, false);
		
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), gameRegisterJsonBuilder.buildJson(jsonUtil));
		assertReceivedMessagesWithWait(GameEngineMessageTypes.REGISTER_RESPONSE);
		final JsonMessage<RegisterResponse> registerResponseMessage = testClientReceivedMessageCollector
				.getMessageByType(GameEngineMessageTypes.REGISTER_RESPONSE);
		
		assertEquals("ERR_ALLOWED_GAME_PLAYS_EXHAUSTED", registerResponseMessage.getError().getCode());
	}	
	
	/**
	 * Create a game that can be paid for multipleTimes check if it works as needed
	 * */
	@Test
	public void testQuizMultipleFeeGamePaidTwoTimes() throws Exception {
		// Game Data
		final GameJsonBuilder quizBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.QUIZ24)
				.withInstantAnswerFeedback()
				.withEntryFee(new BigDecimal("60"), Currency.BONUS)
				.withMultipleFeeGame(2)
				.withInstantAnswerFeedbackDelay(0);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.createAdvertisementProvider(ADVERTISEMENT_PROVIDER_ID, 300);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		// Play quiz
		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));
		
		Game gameForTest = new Game();
		gameForTest.setGameId(GAME_ID);
		Long specialGame = userSpecialGameDao.getUserGameCountLeft(gameForTest, ANONYMOUS_USER_ID);
		assertEquals(1, specialGame.intValue());
		
		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));
		Long specialGame2 = userSpecialGameDao.getUserGameCountLeft(gameForTest, ANONYMOUS_USER_ID);
		assertEquals(0, specialGame2.intValue());
		
		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));
		Long specialGame3 = userSpecialGameDao.getUserGameCountLeft(gameForTest, ANONYMOUS_USER_ID);
		assertEquals(1, specialGame3.intValue());
		
	}
	
	/**
	 * Tests if user can play set amount of games even if game pay count has changed
	 * */
	@Test
	public void testQuizSpecialGamePaidPayCountChanged() throws Exception {
		// Game Data
		GameJsonBuilder quizBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.QUIZ24)
				.withInstantAnswerFeedback()
				.withEntryFee(new BigDecimal("60"), Currency.BONUS)
				.withSpecialGame(5)
				.withInstantAnswerFeedbackDelay(0);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.createAdvertisementProvider(ADVERTISEMENT_PROVIDER_ID, 300);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		// Play quiz
		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));
		Game gameForTest = new Game();
		gameForTest.setGameId(GAME_ID);
		Long specialGame = userSpecialGameDao.getUserGameCountLeft(gameForTest, ANONYMOUS_USER_ID);
		assertEquals(4, specialGame.intValue());
		
		GameJsonBuilder quizBuilder2 = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.QUIZ24)
				.withInstantAnswerFeedback()
				.withEntryFee(new BigDecimal("60"), Currency.BONUS)
				.withSpecialGame(2)
				.withInstantAnswerFeedbackDelay(0);
		
		playGameFlow(quizBuilder2, registerForQuiz24(GAME_ID, false));
		Game gameForTest2 = new Game();
		gameForTest2.setGameId(GAME_ID);
		Long specialGame2 = userSpecialGameDao.getUserGameCountLeft(gameForTest, ANONYMOUS_USER_ID);
		assertEquals(3, specialGame2.intValue());
	}
	

	/**
	 * Tests if user can play set amount of games even if game payment has changed to pay for single game
	 * */
	@Test
	public void testQuizSpecialGamePaidPayCountChangedToSinglePayGame() throws Exception {
		// Game Data
		GameJsonBuilder quizBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.QUIZ24)
				.withGameTypeConfiguration(new GameTypeConfiguration(null, null, new SingleplayerGameTypeConfigurationData(true, 0, "")))
				.withInstantAnswerFeedback()
				.withEntryFee(new BigDecimal("60"), Currency.BONUS)
				.withSpecialGame(5)
				.withInstantAnswerFeedbackDelay(0);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.createAdvertisementProvider(ADVERTISEMENT_PROVIDER_ID, 300);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		// Play quiz
		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));
		Game gameForTest = new Game();
		gameForTest.setGameId(GAME_ID);
		Long specialGame = userSpecialGameDao.getUserGameCountLeft(gameForTest, ANONYMOUS_USER_ID);
		assertEquals(4, specialGame.intValue());
		
		GameJsonBuilder quizBuilder2 = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.QUIZ24)
				.withInstantAnswerFeedback()
				.withEntryFee(new BigDecimal("60"), Currency.BONUS)
				.withInstantAnswerFeedbackDelay(0);
		
		playGameFlow(quizBuilder2, registerForQuiz24(GAME_ID, false));
		Game gameForTest2 = new Game();
		gameForTest2.setGameId(GAME_ID);
		Long specialGame2 = userSpecialGameDao.getUserGameCountLeft(gameForTest2, ANONYMOUS_USER_ID);
		assertEquals(3, specialGame2.intValue());
	}

	
	/**
	 * Tests if user can play set amount of games even if game payment has changed to single pay free game
	 * */
	@Test
	public void testQuizSpecialGamePaidPayCountChangedToSinglePayFreeGame() throws Exception {
		// Game Data
		GameJsonBuilder quizBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.QUIZ24)
				.withGameTypeConfiguration(new GameTypeConfiguration(null, null, new SingleplayerGameTypeConfigurationData(true, 0, "")))
				.withInstantAnswerFeedback()
				.withEntryFee(new BigDecimal("60"), Currency.BONUS)
				.withSpecialGame(5)
				.withInstantAnswerFeedbackDelay(0);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.createAdvertisementProvider(ADVERTISEMENT_PROVIDER_ID, 300);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		// Play quiz
		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));
		Game gameForTest = new Game();
		gameForTest.setGameId(GAME_ID);
		Long specialGame = userSpecialGameDao.getUserGameCountLeft(gameForTest, ANONYMOUS_USER_ID);
		assertEquals(4, specialGame.intValue());
		
		GameJsonBuilder quizBuilder2 = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.QUIZ24)
				.withInstantAnswerFeedback()
				.withInstantAnswerFeedbackDelay(0);
		
		playGameFlow(quizBuilder2, registerForQuiz24(GAME_ID, false));
		Game gameForTest2 = new Game();
		gameForTest2.setGameId(GAME_ID);
		Long specialGame2 = userSpecialGameDao.getUserGameCountLeft(gameForTest2, ANONYMOUS_USER_ID);
		assertEquals(4, specialGame2.intValue());
	}
	
	/**
	 * Tests if user are not allowed to play more
	 * */
	@Test
	public void testQuizSpecialGamePaidCannotPlayMore() throws Exception {
		// Game Data
		final GameJsonBuilder quizBuilder = createGame(GAME_ID)
				.withNumberOfQuestions(2)
				.withGameType(GameType.QUIZ24)
				.withGameTypeConfiguration(new GameTypeConfiguration(null, null, new SingleplayerGameTypeConfigurationData(true, 0, "")))
				.withInstantAnswerFeedback()
				.withEntryFee(new BigDecimal("60"), Currency.BONUS)
				.withSpecialGame(2)
				.withInstantAnswerFeedbackDelay(0);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.createAdvertisementProvider(ADVERTISEMENT_PROVIDER_ID, 300);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		// Play quiz
		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));
		
		Game gameForTest = new Game();
		gameForTest.setGameId(GAME_ID);
		Long specialGame = userSpecialGameDao.getUserGameCountLeft(gameForTest, ANONYMOUS_USER_ID);
		assertEquals(1, specialGame.intValue());
		
		playGameFlow(quizBuilder, registerForQuiz24(GAME_ID, false));
		Long specialGame2 = userSpecialGameDao.getUserGameCountLeft(gameForTest, ANONYMOUS_USER_ID);
		assertEquals(0, specialGame2.intValue());
		
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), registerForQuiz24(GAME_ID, false).buildJson(jsonUtil));
		assertReceivedMessagesWithWait(GameEngineMessageTypes.REGISTER_RESPONSE);
		final JsonMessage<RegisterResponse> registerResponseMessage = testClientReceivedMessageCollector
				.getMessageByType(GameEngineMessageTypes.REGISTER_RESPONSE);
		assertEquals("client",registerResponseMessage.getError().getType());
		assertEquals("ERR_ALLOWED_GAME_PLAYS_EXHAUSTED",registerResponseMessage.getError().getCode());
	}
	
	
	@Test
	public void testUseJokerFiftyFiftyNoJokerSetCall() throws IOException, Exception {
		final GameJsonBuilder gameBuilder = createGame(GAME_ID)
				.withGameType(GameType.QUIZ24)
				.withNumberOfQuestions(5);

		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, false);
		testDataLoader.prepareTestGameData(gameBuilder);
		testDataLoader.createProfile(ANONYMOUS_USER_ID, ANONYMOUS_USER_HANDICAP);

		final String gameInstanceId = requestRegistrationForGame(registerForQuiz24(GAME_ID, false), GAME_ID);
		requestGameStart(startGame(gameInstanceId), false);

		requestReadyToPlay(ANONYMOUS_CLIENT_INFO, GAME_ID, gameInstanceId);

		final String freeJokerFiftyFifty = testDataLoader.getPurchaseFiftyFiftyJson(gameInstanceId, ANONYMOUS_CLIENT_INFO);
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), freeJokerFiftyFifty);

		assertReceivedMessagesWithWait(GameEngineMessageTypes.PURCHASE_FIFTY_FIFTY_RESPONSE);

		final JsonMessage<PurchaseFiftyFiftyResponse> response = testClientReceivedMessageCollector
				.getMessageByType(GameEngineMessageTypes.PURCHASE_FIFTY_FIFTY_RESPONSE);

		assertNotNull("Message must not be null", response);
		assertNotNull("Message has no errors", response.getError());
		assertNull("Message content must be null", response.getContent());
		assertEquals(GameEngineExceptionCodes.ERR_JOKER_NOT_AVAILABLE, response.getError().getCode());
	}

	private void assertAvailableJokers(JokersAvailableResponse response) {
		assertEquals(3, response.getItems().get(JokerType.FIFTY_FIFTY).getAvailableCount().intValue());
		assertEquals(2, response.getItems().get(JokerType.HINT).getAvailableCount().intValue());
		assertEquals(1, response.getItems().get(JokerType.IMMEDIATE_ANSWER).getAvailableCount().intValue());
		assertEquals(JokerType.SKIP.getMaxLimit(), response.getItems().get(JokerType.SKIP).getAvailableCount());
		assertEquals(4, response.getItems().size());

		assertEquals(true, response.getItems().get(JokerType.FIFTY_FIFTY).isAvailableForCurrentQuestion());

		// Hint is false, since currentQuestion has no hint.
		assertEquals(true, response.getItems().get(JokerType.HINT).isAvailableForCurrentQuestion());
		assertEquals(true, response.getItems().get(JokerType.IMMEDIATE_ANSWER).isAvailableForCurrentQuestion());
		assertEquals(true, response.getItems().get(JokerType.SKIP).isAvailableForCurrentQuestion());
	}
	
	
	private JokerConfiguration[] getJokerConfiguration() {
		JokerConfiguration configuration1 = new JokerConfiguration();
		configuration1.setEnabled(true);
		configuration1.setAvailableCount(3);
		configuration1.setType(JokerType.FIFTY_FIFTY);


		JokerConfiguration configuration2 = new JokerConfiguration();
		configuration2.setEnabled(true);
		configuration2.setAvailableCount(2);
		configuration2.setType(JokerType.HINT);

		JokerConfiguration configuration3 = new JokerConfiguration();
		configuration3.setEnabled(true);
		configuration3.setAvailableCount(1);
		configuration3.setType(JokerType.IMMEDIATE_ANSWER);

		JokerConfiguration configuration4 = new JokerConfiguration();
		configuration4.setEnabled(true);
		configuration4.setAvailableCount(null);
		configuration4.setType(JokerType.SKIP);

		JokerConfiguration[] jokerConfigurations = new JokerConfiguration[4];
		jokerConfigurations[0] = configuration1;
		jokerConfigurations[1] = configuration2;
		jokerConfigurations[2] = configuration3;
		jokerConfigurations[3] = configuration4;

		return jokerConfigurations;
	}



}
