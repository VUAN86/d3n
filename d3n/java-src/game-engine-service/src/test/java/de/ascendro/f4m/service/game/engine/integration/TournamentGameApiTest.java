package de.ascendro.f4m.service.game.engine.integration;

import static de.ascendro.f4m.matchers.LambdaMatcher.matches;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.ASSIGNED_POOLS;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.GAME_ID;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.MGI_ID;
import static de.ascendro.f4m.service.game.engine.json.GameJsonBuilder.createLiveTournament;
import static de.ascendro.f4m.service.game.engine.json.GameRegisterJsonBuilder.registerForMultiplayerGame;
import static de.ascendro.f4m.service.game.engine.json.GameStartJsonBuilder.startGame;
import static de.ascendro.f4m.service.integration.RetriedAssert.assertWithWait;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.ArrayUtils.toObject;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.config.ElasticConfigImpl;
import de.ascendro.f4m.server.elastic.ElasticsearchTestNode;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.NotifySubscriberMessageContent;
import de.ascendro.f4m.service.event.model.publish.PublishMessageContent;
import de.ascendro.f4m.service.event.model.subscribe.SubscribeRequest;
import de.ascendro.f4m.service.event.model.subscribe.SubscribeResponse;
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypes;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.json.GameJsonBuilder;
import de.ascendro.f4m.service.game.engine.json.GameRegisterJsonBuilder;
import de.ascendro.f4m.service.game.engine.json.GameStartJsonBuilder;
import de.ascendro.f4m.service.game.engine.model.Answer;
import de.ascendro.f4m.service.game.engine.model.GameHistory;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameState;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.engine.model.QuestionStep;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder;
import de.ascendro.f4m.service.gateway.GatewayMessageTypes;
import de.ascendro.f4m.service.integration.F4MAssert;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.rule.MockServiceRule;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class TournamentGameApiTest extends GameApiTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TournamentGameApiTest.class);
	private static final long SUBSCRIPTION_ID = 7464L;
	private static final int ELASTIC_PORT = 9209;
	
	private List<JsonMessage<? extends JsonMessageContent>> subscribeRequestMessages;
	private List<JsonMessage<? extends JsonMessageContent>> unsubscribeRequestMessages;
	private List<PublishMessageContent> publishRequestMessages;

	@BeforeClass
	public static void setUpClass() {
		GameApiTest.setUpClass();
		System.setProperty(GameEngineConfig.QUESTION_FEEDER_GAME_START_MIN_DELAY, String.valueOf(Integer.MAX_VALUE));//never
		System.setProperty(ElasticConfigImpl.ELASTIC_SERVER_HOSTS, "http://localhost:" + ELASTIC_PORT);
		System.setProperty(ElasticConfigImpl.ELASTIC_HEADER_REFRESH, ElasticConfigImpl.ELASTIC_HEADER_VALUE_WAIT_FOR);
	}
	
	@Rule
	public ElasticsearchTestNode elastic = new ElasticsearchTestNode(ELASTIC_PORT);

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		subscribeRequestMessages = new CopyOnWriteArrayList<>();
		unsubscribeRequestMessages = new CopyOnWriteArrayList<>();
		publishRequestMessages = new CopyOnWriteArrayList<>();
		config.setProperty(F4MConfigImpl.EVENT_SERVICE_DISCOVERY_RETRY_DELAY, 500);
		assertServiceStartup(EventMessageTypes.SERVICE_NAME);
	}
	
	@Test
	public void testReadyToPlayTournament() throws Exception {
		final int numberOfQuestions = 3, playerCount = 20;
		final double userHandicap = 4.56d;
		
		final GameJsonBuilder gameBuilder = createLiveTournament(GAME_ID, MGI_ID)
					.withNumberOfQuestions(numberOfQuestions);
		final CustomGameConfigBuilder customGameConfigBuilder = CustomGameConfigBuilder.createLiveTournament()
				.forGame(GAME_ID)
				.withMaxNumberOfParticipants(playerCount)
				.withEndDateTime(gameBuilder.getEndDateTime())
				.withStartDateTime(gameBuilder.getStartDateTime())
				.withPlayDateTime(DateTimeUtil.getCurrentDateTime());
		testDataLoader.createLiveTournament(gameBuilder, customGameConfigBuilder, aerospikeClientProvider);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, true);
		
		//Prepare users' sessions
		final ClientInfo[] clients = prepareUserSessions(userHandicap, playerCount);
		
		//Register
		final String[] gameInstanceIds = joinAndRegisterForTournamentForAll(clients);
		
		//Start
		requesGameStartForAll(clients, gameInstanceIds);
		
		// Same questions for all players
		final Set<String> questionIds = Stream.of(gameInstanceIds)
				.map(id -> gameInstanceDao.getGameInstance(id).getQuestionIds())
				.flatMap(ids -> stream(ids))
				.collect(toSet());
		F4MAssert.assertSize(numberOfQuestions, questionIds);
		
		// Ready to Play
		requestReadyToPlayForAll(clients, gameInstanceIds);

		//assert subscription for tournament/live/<<MULTIPLAYER_GAME_INSTANCE_ID>>/startStep
		//assert subscription also for tournament/live/<<MULTIPLAYER_GAME_INSTANCE_ID>>/endGame
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(2, subscribeRequestMessages));
		final SubscribeRequest subscribeRequestStartStep = getSubscribeRequest(0);
		assertEquals(Game.getLiveTournamentStartStepTopic(MGI_ID), subscribeRequestStartStep.getTopic());

		final SubscribeRequest subscribeRequestGameEnd = getSubscribeRequest(1);
		assertEquals(Game.getLiveTournamentEndGameTopic(MGI_ID), subscribeRequestGameEnd.getTopic());
	}

	@SuppressWarnings("unchecked")
	private SubscribeRequest getSubscribeRequest(int position) {
		final JsonMessage<SubscribeRequest> subscribeRequestMessageStartStep = (JsonMessage<SubscribeRequest>) subscribeRequestMessages.get(position);
		return subscribeRequestMessageStartStep.getContent();
	}
	
	@Test
	public void testPlayLiveTournament() throws Exception {
		final int numberOfQuestions = 3, playerCount = 3;
		final double userHandicap = 5.89d;
		
		final GameJsonBuilder gameBuilder = createLiveTournament(GAME_ID, MGI_ID)
					.withNumberOfQuestions(numberOfQuestions);
		final CustomGameConfigBuilder customGameConfigBuilder = CustomGameConfigBuilder.createLiveTournament()
				.forGame(GAME_ID)
				.withMaxNumberOfParticipants(null)//no max participants, should be triggered via game expiration
				.withStartDateTime(DateTimeUtil.getCurrentDateTime().minusMinutes(2))
				.withEndDateTime(DateTimeUtil.getCurrentDateTime().plusMinutes(2))
				.withPlayDateTime(DateTimeUtil.getCurrentDateTime());
		testDataLoader.createLiveTournament(gameBuilder, customGameConfigBuilder, aerospikeClientProvider);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, true);
		
		//Prepare users' sessions
		final ClientInfo[] clients = prepareUserSessions(userHandicap, playerCount);
		
		//Register
		final String[] gameInstanceIds = joinAndRegisterForTournamentForAll(clients);
		//Start
		requesGameStartForAll(clients, gameInstanceIds);
		// ReadyToPlay
		requestReadyToPlayForAll(clients, gameInstanceIds);
		
		//Notify about game questions		
		do{
			testClientReceivedMessageCollector.clearReceivedMessageList();
			notifyStartStep();
			
			assertReceivedMessagesWithWait(playerCount, GameEngineMessageTypes.HEALTH_CHECK);
			sendHealthCheckResponseToAll();

			assertReceivedMessagesWithWait(playerCount, GameEngineMessageTypes.START_STEP);
			sendNextOrAnswerToAll(gameInstanceIds, clients);
		} while (!isGameEnded(gameInstanceIds[0]));

		assertReceivedMessagesWithWait(playerCount, GameEngineMessageTypes.END_GAME);
		F4MAssert.assertSize(1, unsubscribeRequestMessages);
		
		//validate all questions answered
		final List<GameState> gameStates = Stream.of(gameInstanceIds)
			.map(gId -> gameInstanceDao.getGameInstance(gId).getGameState())
			.collect(toList());
		assertThat(gameStates, everyItem(matches("Game status should be COMPLETED",
				(state) -> state.isCompleted())));
		assertThat(gameStates, everyItem(matches("Game end status should be CALCULATED_RESULT",
				(state) -> state.getGameEndStatus() == GameEndStatus.CALCULATED_RESULT)));
		assertThat(gameStates, everyItem(matches("Expected " + numberOfQuestions + " answers",
				(state) -> state.getAnswers().length == numberOfQuestions)));
		
		//validate history
		final List<GameHistory> allHistory = IntStream.range(0, playerCount)
			.mapToObj(i -> testDataLoader.getGameHistory(clients[i].getUserId(), gameInstanceIds[i]))
			.collect(Collectors.toList());
		assertThat(allHistory, everyItem(matches("Game history status should be COMPLETED",
				(history) -> history.getStatus() == GameStatus.COMPLETED)));
		assertThat(allHistory, everyItem(matches("Game end status should be CALCULATED_RESULT",
				(history) -> history.getEndStatus() == GameEndStatus.CALCULATED_RESULT)));
		
		//validate all answers
		final List<Answer> allAnswers = gameStates.stream()
			.map(s -> asList(s.getAnswers()))
			.flatMap(l -> l.stream())
			.collect(toList());
		assertThat(allAnswers, everyItem(matches("Step start time must be more that 0",
				(answer) -> everyItem(greaterThan(0L)).matches(asList(toObject(answer.getStepStartMsT()))))));
		assertThat(allAnswers, everyItem(matches("Step server time must be more that 0",
				(answer) -> everyItem(greaterThan(0L)).matches(asList(toObject(answer.getServerMsT()))))));
		assertThat(allAnswers, everyItem(matches("Step answer receive time must be more that 0",
				(answer) -> everyItem(greaterThan(0L)).matches(asList(toObject(answer.getAnswerReceiveMsT()))))));
		assertThat(allAnswers, everyItem(matches("Step health check time must be more that 0",
				(answer) -> everyItem(greaterThan(0L)).matches(asList(answer.getPrDelMsT())))));
		assertThat(allAnswers, everyItem(matches("Step client time must be more that 0",
				(answer) -> everyItem(greaterThan(0L)).matches(asList(toObject(answer.getClientMsT()))))));

		//End with calculating single/muliplayer results request to result engine and its response
		F4MAssert.assertSize(playerCount, calculateResultsRequests);
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, calculateMultiplayerResultsRequests));
		
		assertNotNull(commonMultiplayerGameInstanceDao.getConfig(MGI_ID).getEndDateTime());
		assertThat(commonMultiplayerGameInstanceDao.getConfig(MGI_ID).getEndDateTime(), 
				lessThan(DateTimeUtil.getCurrentDateTime().plusSeconds(1)));
	}

	@Test
	public void testGameEndWithNoInteraction() throws Exception {
		//no disconnect
		testGameEndWithNoInteractionAndPossiblePlayerDisconnected(clients -> clients.length);
	}

	@Test
	public void testGameEndWithNoInteractionAndOnePlayerDisconnected() throws Exception {
		testGameEndWithNoInteractionAndPossiblePlayerDisconnected(clients -> {
			disconnectPlayer(clients[0]);
			return clients.length-1;
		});
	}

	private void testGameEndWithNoInteractionAndPossiblePlayerDisconnected(MockServiceRule.ThrowingFunction<ClientInfo[], Integer> disconnectCall) throws Exception {
		TestDataLoader.setQuestionsJsonFileName("onlyOneStepQuestions.json");
		// alter the delays so we don't wait too much, this is a test
		config.setProperty(GameEngineConfig.QUESTION_FEEDER_GAME_START_MIN_DELAY, 100);
		config.setProperty(GameEngineConfig.QUESTION_FEEDER_DISTRIBUTION_DELAY_IN_BETWEEN_QUESTIONS, 100);
		final int numberOfQuestions = 2, playerCount = 2;
		final double userHandicap = 5.89d;

		final GameJsonBuilder gameBuilder = createLiveTournament(GAME_ID, MGI_ID)
				.withNumberOfQuestions(numberOfQuestions);
		final CustomGameConfigBuilder customGameConfigBuilder = CustomGameConfigBuilder.createLiveTournament()
				.forGame(GAME_ID)
				.withGameType(GameType.LIVE_TOURNAMENT)
				.withTenant(TENANT_ID)
				.withMaxNumberOfParticipants(null)//no max participants, should be triggered via game expiration
				.withStartDateTime(DateTimeUtil.getCurrentDateTime().minusMinutes(2))
				.withEndDateTime(DateTimeUtil.getCurrentDateTime().plusMinutes(2))
				.withPlayDateTime(DateTimeUtil.getCurrentDateTime());
		testDataLoader.createLiveTournament(gameBuilder, customGameConfigBuilder, aerospikeClientProvider);
		testDataLoader.prepareTestGameQuestionPools(ASSIGNED_POOLS, true);

		//Prepare users' sessions
		final ClientInfo[] clients = prepareUserSessions(userHandicap, playerCount);

		//Register
		final String[] gameInstanceIds = joinAndRegisterForTournamentForAll(clients);
		//Start
		requesGameStartForAll(clients, gameInstanceIds);
		// ReadyToPlay
		requestReadyToPlayForAll(clients, gameInstanceIds);
		
		int remainingPlayerCountAfterPossibleDisconnect = disconnectCall.apply(clients);

		testClientReceivedMessageCollector.clearReceivedMessageList();
		notifyStartStep();

		assertReceivedMessagesWithWait(remainingPlayerCountAfterPossibleDisconnect, GameEngineMessageTypes.HEALTH_CHECK);
		sendHealthCheckResponseToAll();

		assertReceivedMessagesWithWait(remainingPlayerCountAfterPossibleDisconnect, GameEngineMessageTypes.START_STEP);

		assertWithWait(() -> F4MAssert.assertSize(3, publishRequestMessages), 5000);
		// if endGame is not present, test will fail
		publishRequestMessages.stream().filter(message -> Game.isLiveTournamentEndGameTopic(message.getTopic())).findFirst().get();

		notifyEndGame();

		assertReceivedMessagesWithWaitingLonger(remainingPlayerCountAfterPossibleDisconnect, GameEngineMessageTypes.END_GAME);
		F4MAssert.assertSize(1, unsubscribeRequestMessages);

		//validate all questions answered
		final List<GameState> gameStates = Stream.of(gameInstanceIds)
				.map(gId -> gameInstanceDao.getGameInstance(gId).getGameState())
				.collect(toList());
		assertThat(gameStates, everyItem(matches("Game status should be COMPLETED",
				(state) -> state.isCompleted())));
		assertThat(gameStates, everyItem(matches("Game end status should be CALCULATED_RESULT",
				(state) -> state.getGameEndStatus() == GameEndStatus.CALCULATED_RESULT)));
		assertThat(gameStates, everyItem(matches("Expected " + numberOfQuestions + " answers",
				(state) -> state.getAnswers().length == numberOfQuestions)));
		//validate history
		final List<GameHistory> allHistory = IntStream.range(0, playerCount)
				.mapToObj(i -> testDataLoader.getGameHistory(clients[i].getUserId(), gameInstanceIds[i]))
				.collect(Collectors.toList());
		assertThat(allHistory, everyItem(matches("Game history status should be COMPLETED",
				(history) -> history.getStatus() == GameStatus.COMPLETED)));
		assertThat(allHistory, everyItem(matches("Game end status should be CALCULATED_RESULT",
				(history) -> history.getEndStatus() == GameEndStatus.CALCULATED_RESULT)));

		//validate all answers
		final List<Answer> allAnswers = gameStates.stream()
				.map(s -> asList(s.getAnswers()))
				.flatMap(l -> l.stream())
				.collect(toList());

		// Since the user does not interact at all with the game, the answers are guaranteed to only have server
		// start step time set on them, and guaranteed to be 4
		assertThat(allAnswers, everyItem(matches("Step start time must be more that 0",
				(answer) -> everyItem(greaterThan(0L)).matches(asList(toObject(answer.getStepStartMsT()))))));
		assertEquals(4, allAnswers.size());

		//End with calculating single/muliplayer results request to result engine and its response
		F4MAssert.assertSize(playerCount, calculateResultsRequests);
		RetriedAssert.assertWithWait(() -> F4MAssert.assertSize(1, calculateMultiplayerResultsRequests));

		assertNotNull(commonMultiplayerGameInstanceDao.getConfig(MGI_ID).getEndDateTime());
		assertThat(commonMultiplayerGameInstanceDao.getConfig(MGI_ID).getEndDateTime(),
				lessThan(DateTimeUtil.getCurrentDateTime().plusSeconds(1)));

		// set questions set to default for the TestDataLoader :
		TestDataLoader.resetQuestionsJsonFileName();
	}
	
	private SessionWrapper getClientSessionWrapper(SessionPool gameEngineSessionPool, ClientInfo clientInfo) {
		return gameEngineSessionPool.getSessionByClientIdServiceName(clientInfo.getClientId(), GatewayMessageTypes.SERVICE_NAME);
	}
	
	private void disconnectPlayer(ClientInfo clientInfo) throws URISyntaxException {		
		SessionPool gameEngineSessionPool = gameEngineInjector.getInstance(SessionPool.class);
		assertNotNull(getClientSessionWrapper(gameEngineSessionPool, clientInfo));

		final JsonMessage<? extends JsonMessageContent> clientDiconnectedMessage = jsonMessageUtil
				.createNewMessage(GatewayMessageTypes.CLIENT_DISCONNECT, null);
		clientDiconnectedMessage.setClientId(clientInfo.getClientId());
		clientDiconnectedMessage.setSeq(null);
		jsonWebSocketClientSessionPool.sendAsyncMessage(getServiceConnectionInformation(), clientDiconnectedMessage);
		
		assertWithWait(() -> assertNull(getClientSessionWrapper(gameEngineSessionPool, clientInfo)));
	}

	private void sendHealthCheckResponseToAll() throws URISyntaxException {
		final List<JsonMessage<EmptyJsonMessageContent>> allHealthCheckMessages = testClientReceivedMessageCollector
				.<EmptyJsonMessageContent>getMessagesByType(GameEngineMessageTypes.HEALTH_CHECK);
		for (JsonMessage<EmptyJsonMessageContent> healthCheckMessage : allHealthCheckMessages) {
			assertAndReplyHealthCheck(healthCheckMessage);
		}
	}

	private void sendNextOrAnswerToAll(String[] gameInstanceIds, ClientInfo[] clients) throws Exception{
		final ExecutorService clientThreadPool = Executors.newFixedThreadPool(clients.length);
		 
		testClientReceivedMessageCollector.clearReceivedMessageList();
		final boolean answerExpected = isAnswerExpected(gameInstanceIds[0]);
		for(int i = 0; i < gameInstanceIds.length; i++){
			final String gameInstanceId = gameInstanceIds[i];
			final ClientInfo clientInfo = clients[i];
			
			clientThreadPool.submit(() -> {
				final GameInstance gameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
				final QuestionStep currentQuestionStep = gameInstance.getGameState().getCurrentQuestionStep();
				try {
					LOGGER.debug("Attempting to answer question [{}] for client [{}] gameInstance[{}]", currentQuestionStep, gameInstanceId);
					sendNextOrAnswer(gameInstanceId, clientInfo, new HashMap<>(),
							new HashMap<>());						
				} catch (IOException | URISyntaxException e) {
					LOGGER.error("Failed to send nextStep/answerQuestion for gi[{}] by client[{}]", gameInstanceId, clientInfo.getClientId(), e);
				}
			});
		}
		clientThreadPool.shutdown();
		clientThreadPool.awaitTermination(RetriedAssert.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		
		if(answerExpected){
			LOGGER.debug("Awaiting responses for answers: gameInstances {} for clients {}", gameInstanceIds, clients);
			assertReceivedMessagesWithWait(clients.length, GameEngineMessageTypes.ANSWER_QUESTION_RESPONSE);
		}else{
			LOGGER.debug("Sent next step with no expected response: gameInstances {} for clients {}", gameInstanceIds, clients);
			//all respond to 1. step: (assume that all questions have max 2 steps, so nextStep is called only once for step 1 answer registration)
			RetriedAssert.assertWithWait(() -> assertEquals(gameInstanceIds.length, Stream.of(gameInstanceIds)
						.map(gameInstanceId -> gameInstanceDao.getGameInstance(gameInstanceId).getGameState())
						.filter(state -> !ArrayUtils.isEmpty(state.getAnswer(state.getCurrentQuestionStep().getQuestion()).getClientMsT()))
						.count()));
		}
	}
	
	private void notifyStartStep() throws URISyntaxException{
		notifySubscriber(Game.getLiveTournamentStartStepTopic(MGI_ID));
	}

	private void notifyEndGame() throws URISyntaxException{
		notifySubscriber(Game.getLiveTournamentEndGameTopic(MGI_ID));
	}

	private void notifySubscriber(String topic) {
		final NotifySubscriberMessageContent notifySubscriberMessageContent = new NotifySubscriberMessageContent(SUBSCRIPTION_ID,
				topic);
		notifySubscriberMessageContent.setNotificationContent(new JsonObject());
		final JsonMessage<NotifySubscriberMessageContent> notifySubscriberMessage = jsonMessageUtil
				.createNewMessage(EventMessageTypes.NOTIFY_SUBSCRIBER, notifySubscriberMessageContent);
		sendAsynMessageAsMockClient(notifySubscriberMessage);
	}

	private ClientInfo[] prepareUserSessions(final double userHandicap, int playerCount) throws URISyntaxException,
			IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
		final ClientInfo[] clients = new ClientInfo[playerCount];
		for(int i = 0; i < clients.length; i++){
			final String uniqueSuffix = UUID.randomUUID().toString();
			
			clients[i] = ClientInfo.cloneOf(KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO);
			clients[i].setUserId(String.format("user-%d -> " + uniqueSuffix, i));
			clients[i].setClientId(String.format("client-%d -> " + uniqueSuffix, i));
			clients[i].setTenantId(TENANT_ID);
			clients[i].setHandicap(userHandicap);
			
			testDataLoader.createProfile(clients[i].getUserId(), userHandicap);
		}
		return clients;
	}
	
	private void requestReadyToPlayForAll(final ClientInfo[] clients, final String[] gameInstanceIds) throws URISyntaxException, IOException{
		for (int i = 0; i < clients.length; i++) {
			requestReadyToPlay(clients[i], GAME_ID, gameInstanceIds[i]);
		}
	}

	private GameStartJsonBuilder[] requesGameStartForAll(final ClientInfo[] clients, final String[] gameInstanceIds) throws IOException, URISyntaxException {
		final GameStartJsonBuilder[] gameStartJsonBuilders = new GameStartJsonBuilder[clients.length];
		for(int i = 0; i < clients.length; i++){
			gameStartJsonBuilders[i] = startGame(gameInstanceIds[i])
					.byUser(clients[i]);
			requestGameStart(gameStartJsonBuilders[i], false);
		}
		return gameStartJsonBuilders;
	}
	
	private String[] joinAndRegisterForTournamentForAll(final ClientInfo[] clients) throws IOException, URISyntaxException {
		final String[] gameInstanceIds = new String[clients.length];
		for(int i = 0; i < clients.length; i++){
			testDataLoader.joinTournament(aerospikeClientProvider, MGI_ID, clients[i].getUserId());
			final GameRegisterJsonBuilder registrationJson = registerForMultiplayerGame(MGI_ID).byUser(clients[i]);
			gameInstanceIds[i] = requestRegistrationForGame(registrationJson, GAME_ID);
		}
		return gameInstanceIds;
	}
	
	@Override
	protected JsonMessageContent onReceivedMessage(RequestContext requestContext)
			throws Exception {
		JsonMessage<? extends JsonMessageContent> originalMessageDecoded = requestContext.getMessage();
		if (EventMessageTypes.SUBSCRIBE == originalMessageDecoded.getType(EventMessageTypes.class)) {
			subscribeRequestMessages.add(originalMessageDecoded);
			final SubscribeRequest subscribeRequest = (SubscribeRequest) originalMessageDecoded.getContent();
			return new SubscribeResponse(SUBSCRIPTION_ID, false, null, subscribeRequest.getTopic());
		} else if (EventMessageTypes.UNSUBSCRIBE == originalMessageDecoded.getType(EventMessageTypes.class)) {
			unsubscribeRequestMessages.add(originalMessageDecoded);
			return null;
		} else if(EventMessageTypes.PUBLISH == originalMessageDecoded.getType(EventMessageTypes.class)) {
			publishRequestMessages.add((PublishMessageContent) originalMessageDecoded.getContent());
			return null;
		} else {
			return super.onReceivedMessage(requestContext);
		}
	}	
}
