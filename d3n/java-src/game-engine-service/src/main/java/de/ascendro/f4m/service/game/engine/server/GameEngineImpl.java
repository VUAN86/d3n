package de.ascendro.f4m.service.game.engine.server;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;
import de.ascendro.f4m.server.game.GameAerospikeDao;
import de.ascendro.f4m.server.game.GameUtil;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.result.RefundReason;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypes;
import de.ascendro.f4m.service.game.engine.advertisement.GameEngineAdvertisementManager;
import de.ascendro.f4m.service.game.engine.dao.instance.ActiveGameInstanceDao;
import de.ascendro.f4m.service.game.engine.dao.instance.GameInstanceAerospikeDao;
import de.ascendro.f4m.service.game.engine.dao.instance.GameInstanceAerospikeDao.UpdateGameInstanceAction;
import de.ascendro.f4m.service.game.engine.dao.preselected.PreselectedQuestionsDao;
import de.ascendro.f4m.service.game.engine.exception.*;
import de.ascendro.f4m.service.game.engine.health.HealthCheckManager;
import de.ascendro.f4m.service.game.engine.history.GameHistoryManager;
import de.ascendro.f4m.service.game.engine.model.*;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.game.engine.model.joker.JokerInformation;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseJokerRequest;
import de.ascendro.f4m.service.game.engine.model.schema.GameEngineMessageSchemaMapper;
import de.ascendro.f4m.service.game.engine.multiplayer.MultiplayerGameManager;
import de.ascendro.f4m.service.game.selection.model.game.*;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.singleplayer.SinglePlayerGameParameters;
import de.ascendro.f4m.service.game.usergameaccess.UserGameAccessService;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.DateTimeUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class GameEngineImpl implements GameEngine {

	private static final Logger LOGGER = LoggerFactory.getLogger(GameEngineImpl.class);
	
	private final JsonMessageUtil jsonUtil;
	private final GameAerospikeDao gameDAO;
	private final GameInstanceAerospikeDao gameInstanceDao;
	private final QuestionSelector questionSelector;
	private final HealthCheckManager healthCheckManager;
	private final GameHistoryManager gameHistoryManagerImpl;
	private final PreselectedQuestionsDao preselectedQuestionsDao;
	private final CommonProfileAerospikeDao commonProfileAerospikeDao;
	private final MultiplayerGameManager multiplayerGameManager;
	private final GameEngineAdvertisementManager gameEngineAdvertisementManager;
	private final GameEngineMessageSchemaMapper gameEngineMessageSchemaMapper;
	private final ActiveGameInstanceDao activeGameInstanceDao;
	private final MessageCoordinator messageCoordinator;
	private final Set<String> registerMessagePermissions;
	private final UserGameAccessService userGameAccessService;

	@Inject
	public GameEngineImpl(JsonMessageUtil jsonUtil, GameAerospikeDao gameDAO, GameInstanceAerospikeDao gameInstanceDao,
			QuestionSelector questionSelector, HealthCheckManager healthCheckManager,
			GameHistoryManager gameHistoryManagerImpl, PreselectedQuestionsDao preselectedQuestionsDao,
			CommonProfileAerospikeDao commonProfileAerospikeDao, MultiplayerGameManager multiplayerGameManager,
			GameEngineAdvertisementManager gameEngineAdvertisementManager, GameEngineMessageSchemaMapper gameEngineMessageSchemaMapper,
			ActiveGameInstanceDao activeGameInstanceDao, MessageCoordinator messageCoordinator, UserGameAccessService gameChecker) {
		this.jsonUtil = jsonUtil;

		this.gameDAO = gameDAO;
		this.gameInstanceDao = gameInstanceDao;

		this.questionSelector = questionSelector;

		this.healthCheckManager = healthCheckManager;
		this.gameHistoryManagerImpl = gameHistoryManagerImpl;

		this.preselectedQuestionsDao = preselectedQuestionsDao;
		this.commonProfileAerospikeDao = commonProfileAerospikeDao;
		this.multiplayerGameManager = multiplayerGameManager;
		this.gameEngineAdvertisementManager = gameEngineAdvertisementManager;

		this.gameEngineMessageSchemaMapper = gameEngineMessageSchemaMapper;
		this.activeGameInstanceDao = activeGameInstanceDao;
		this.registerMessagePermissions = gameEngineMessageSchemaMapper
				.getMessagePermissions(GameEngineMessageTypes.REGISTER.getMessageName());
		this.messageCoordinator = messageCoordinator;
		this.userGameAccessService = gameChecker;
	}
	
	@Override
	public String registerForFreeQuiz24(ClientInfo clientInfo, String gameId, SinglePlayerGameParameters singlePlayerGameConfig){
		Validate.notNull(gameId, "Quiz24 game must have game id specified");
		final Game game = getGame(gameId);
		final SinglePlayerGameParameters validSinglePlayerGameConfig = getValidSinglePlayerGameConfig(singlePlayerGameConfig, game, true);
		return registerForQuiz24(clientInfo, game, validSinglePlayerGameConfig, null, null, null);
	}
	
	@Override
	public String registerForPaidQuiz24(ClientInfo clientInfo, String gameId, String entryFeeTransactionId,
			BigDecimal entryFeeAmount, Currency entryFeeCurrency, SinglePlayerGameParameters singlePlayerGameConfig) {
		Validate.notNull(gameId, "Quiz24 game must have game id specified");
		final Game game = getGame(gameId);
		final SinglePlayerGameParameters validSinglePlayerGameConfig = getValidSinglePlayerGameConfig(singlePlayerGameConfig, game, false);
		if (GameUtil.haveMultipleGamesPurchase(game)){
			userGameAccessService.grantAccess(game, clientInfo.getUserId());
			userGameAccessService.registerAccess(game, clientInfo.getUserId());
		}
		
		return registerForQuiz24(clientInfo, game, validSinglePlayerGameConfig, entryFeeTransactionId, entryFeeAmount, entryFeeCurrency);
	}
	
	private SinglePlayerGameParameters getValidSinglePlayerGameConfig(SinglePlayerGameParameters singlePlayerGameConfig, Game game, boolean freeGame){
		final SinglePlayerGameParameters validSinglePlayerGameConfig = Optional.ofNullable(singlePlayerGameConfig)
			.orElse(new SinglePlayerGameParameters());
		validSinglePlayerGameConfig.setTrainingMode(validSinglePlayerGameConfig.isTrainingMode() && freeGame);
		validSinglePlayerGameConfig.validate(game);
		return validSinglePlayerGameConfig;
	}
	
	private String registerForQuiz24(ClientInfo clientInfo, Game game, SinglePlayerGameParameters singlePlayerGameConfig,
			String entryFeeTransactionId, BigDecimal entryFeeAmount, Currency entryFeeCurrency) {
		Validate.isTrue(game.isFree() || !singlePlayerGameConfig.isTrainingMode(), "Attempted to play not free QUIZ24 in trainningMode");
		
		validateIfHasPaidEntreeFee(game, null, new GameState(GameStatus.REGISTERED, entryFeeTransactionId), clientInfo.getUserId());
		
		final GameInstanceBuilder gameInstanceBuilder = GameInstanceBuilder.create(clientInfo, game)
				.withEntryFeeTransactionId(entryFeeTransactionId)
				.withEntryFee(entryFeeAmount, entryFeeCurrency)
				.applySinglePlayerGameConfig(singlePlayerGameConfig);
		final String gameInstanceId = createGameInstance(gameInstanceBuilder, null);
		
		gameHistoryManagerImpl.recordGameRegistration(null, clientInfo.getUserId(), gameInstanceId, game.getGameId());
		
		return gameInstanceId;
	}
	
	@Override
	public String registerForMultiplayerGame(ClientInfo clientInfo, String mgiId, String entryFeeTransactionId, BigDecimal entryFeeAmount, Currency entryFeeCurrency) {
		final CustomGameConfig multiplayerGameConfig = multiplayerGameManager.getMultiplayerGameConfig(mgiId);
		
		final Game game = getGame(multiplayerGameConfig.getGameId()); 
		
		//Validate if entry fee paid if needed
		validateIfHasPaidEntreeFee(game, multiplayerGameConfig, new GameState(GameStatus.REGISTERED, entryFeeTransactionId), clientInfo.getUserId());

		final GameInstanceBuilder gameInstanceBuilder = GameInstanceBuilder.create(clientInfo, game)
			.withMgiId(mgiId)
			.withEntryFee(entryFeeAmount, entryFeeCurrency)
			.withEntryFeeTransactionId(entryFeeTransactionId)
			.applyMultiplayerGameConfig(multiplayerGameConfig);
				
		//Create instance
		final String gameInstanceId = createGameInstance(gameInstanceBuilder, multiplayerGameConfig.getExpiryDateTime());
		
		//multiplayer register
		multiplayerGameManager.registerForGame(mgiId, clientInfo, gameInstanceId);
		gameHistoryManagerImpl.recordGameRegistration(mgiId, clientInfo.getUserId(), gameInstanceId, game.getGameId());
		
		return gameInstanceId;
	}
	
	/* (non-Javadoc)
	 * @see de.ascendro.f4m.service.game.engine.server.GameEngine#startGame(java.lang.String, java.lang.String, java.lang.String, java.lang.String[], boolean)
	 */
	@Override
	public GameInstance startGame(String userId, String gameInstanceId, String userLanguage) throws F4MInsufficientRightsException{
		GameInstance gameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		if (gameInstance != null) {
			final Game game = gameInstance.getGame();
			final CustomGameConfig customGameConfig = gameInstance.getMgiId() != null
					? multiplayerGameManager.getMultiplayerGameConfig(gameInstance.getMgiId()) : null;
			
			validateIfHasPaidEntreeFee(game, customGameConfig, gameInstance.getGameState(), userId);
			
			final Double userHandicap = getUserHandicap(userId);
			gameInstance = startGameInstance(gameInstance.getId(), userLanguage, userHandicap);
			if(game.isMultiUserGame()){
				multiplayerGameManager.joinGame(gameInstance.getMgiId(), gameInstance.getId(), userId, userHandicap);
			}
			
			gameHistoryManagerImpl.recordGamePrepared(userId, gameInstance.getId());
			
			return gameInstance;
		} else {
			throw new F4MEntryNotFoundException("Game instance not found by id [" + gameInstanceId + "]");
		}
	}

	@Override
	public void validateSingleGameForRegistration(ClientInfo clientInfo, Game game,
			SinglePlayerGameParameters singlePlayerGameConfig) {
		GameUtil.validateIfGameAvailable(game.getStartDateTime(), game.getEndDateTime(), game.getGameId());
		singlePlayerGameConfig.validate(game);
		validateGameRegistrationPermissions(game, clientInfo);
	}
	
	@Override
	public void validateMultiplayerGameForRegistration(ClientInfo clientInfo, String mgiId, CustomGameConfig customGameConfig) {
		GameUtil.validateIfGameAvailable(customGameConfig.getStartDateTime(), customGameConfig.getEndDateTime(), customGameConfig.getGameId());
		if (GameUtil.isGameInvitationExpired(customGameConfig)) {
			multiplayerGameManager.markAsExpired(mgiId);
			throw new F4MGameNotAvailableException(String.format("Duel [%s] is expired", customGameConfig.getGameId()));
		}
		validateGameRegistrationPermissions(customGameConfig, clientInfo);
	}
	
	/* (non-Javadoc)
	 * @see de.ascendro.f4m.service.game.engine.server.GameEngine#readyToPlay(java.lang.String, long)
	 */
	@Override
	public GameInstance readyToPlay(String gameInstanceId, long messageReceiveTimestamp, boolean startImmediately) {
		final GameInstance updatedGameInstance = gameInstanceDao.update(gameInstanceId, new UpdateGameInstanceAction(jsonUtil) {
			@Override
			public String updateGameInstance(GameInstance gameInstance) {
				if(startImmediately){
					gameInstance.startPlaying(messageReceiveTimestamp);				
					gameHistoryManagerImpl.recordStartPlaying(gameInstance.getUserId(), gameInstanceId);
				}else{
					gameInstance.getGameState().readyToPlay();
					gameHistoryManagerImpl.recordGameReadyToPlay(gameInstance.getUserId(), gameInstanceId);
				}
				return gameInstance.getAsString();
			}
		});
		updateGameState(gameInstanceId, updatedGameInstance.getGameState());
		return updatedGameInstance;
	}
	
	@Override
	public GameEndStatus cancelGameByClient(ClientInfo clientInfo, String gameInstanceId) {
		GameInstance gameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		
		validateGameInstanceForCancel(clientInfo.getUserId(), gameInstance);
		
		gameInstance = gameInstanceDao.cancelGameInstance(gameInstanceId);
		gameHistoryManagerImpl.recordGameCancellation(gameInstance.getUserId(), gameInstanceId);
		multiplayerGameManager.cancelGame(gameInstance.getUserId(), gameInstance.getMgiId(), gameInstanceId);
		activeGameInstanceDao.delete(gameInstanceId);
		return gameInstance.getGameState().getGameEndStatus();
	}
	
	@Override
	public GameInstance validateIfGameNotCancelled(String gameInstanceId) {
		final GameInstance gameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		if (gameInstance != null && gameInstance.getGameState() != null) {
			if (gameInstance.getGameState().getGameStatus() == GameStatus.CANCELLED) {
				throw new F4MGameCancelledException("Referred game instance state is cancelled");
			}
		} else {
			throw new F4MFatalErrorException("Referred unknown game instance");
		}
		return gameInstance;
	}
	
	@Override
	public boolean isGameCompleted(String gameInstanceId) {
		final GameInstance gameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		if(gameInstance != null){
			final GameState gameState = gameInstance.getGameState();
			return gameState.isCompleted();
		}else{
			throw new F4MEntryNotFoundException("Game instance not found by id [" + gameInstanceId + "]");
		}
	}

	private void validateGameInstanceForCancel(String userId, GameInstance gameInstance) {
		if (gameInstance == null || gameInstance.getGameState() == null) {
			throw new F4MEntryNotFoundException("Game Instance or its state not available");
		} else if (userId != null && !userId.equals(gameInstance.getUserId())) {
			throw new F4MInsufficientRightsException("Cannot cancel other user game instance");
		} else if (gameInstance.getGameState().getGameEndStatus() != null) {
			throw new F4MGameFlowViolation("Cannot cancel already ended game");
		} else if (gameInstance.getGameState().getGameStatus() == GameStatus.CANCELLED) {
			throw new F4MGameCancelledException("Cannot cancel already cancelled game");
		} else if (gameInstance.getGameState().getGameStatus() == GameStatus.COMPLETED) {
			throw new F4MGameFlowViolation("Cannot cancel already completed game");
		}
	}

	/**
	 * Create game instance within aerospike:
	 * -- regular and active game instance
	 * @param gameInstanceBuilder
	 * @param invitationExpireDateTime - multiplayer invitation expiration date-time if present
	 * @return new game instance id
	 */
	private String createGameInstance(GameInstanceBuilder gameInstanceBuilder, ZonedDateTime invitationExpireDateTime) {
		final GameInstance gameInstance = gameInstanceBuilder.build();
		final String gameInstanceId = gameInstanceDao.create(gameInstance);
		activeGameInstanceDao.create(gameInstance, invitationExpireDateTime);
		return gameInstanceId;
	}
	
	protected GameInstance startGameInstance(String gameInstanceId, String userLanguage, final Double userHandicap){
		final GameInstance gameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		final Game game = gameInstance.getGame();
		
		final String[] questionPools = gameInstance.getQuestionPoolIds();
		final int skipsAvailable = game.getTotalSkipsAvailable();
		final int numberOfQuestions = gameInstance.getNumberOfQuestions() + skipsAvailable; // Have to select more questions to support skips
		final List<Question> questions = getQuestions(gameInstance.getUserId(), gameInstance.getMgiId(), 
				game, userLanguage, questionPools, numberOfQuestions, skipsAvailable);
		
		final GameInstance updatedGameInstance = gameInstanceDao.update(gameInstanceId, new UpdateGameInstanceAction(jsonUtil) {
			@Override
			public String updateGameInstance(GameInstance gameInstance) {
				final GameState gameState = gameInstance.getGameState();
				gameState.startGame();
				
				gameInstance.setUserLanguage(userLanguage);
				gameInstance.setUserHandicap(userHandicap);
				gameInstance.setQuestions(questions);
				gameInstance.setStartDateTime(DateTimeUtil.getCurrentDateTime());

				final String loadingBlobKey = gameEngineAdvertisementManager.getLoadingBlobKey(game);
				if (loadingBlobKey!=null) {
					gameInstance.setLoadingBlobKey(loadingBlobKey);
				} else if (game.hasLoadingScreen()) {
					LOGGER.warn("Game has loadingSreen defined and enabled, but loadingSreen provider counter is empty/missing");
				}
				final String[] advertisementBlobKeys = gameEngineAdvertisementManager.getAdvertisementBlobKeys(game, numberOfQuestions);
				if (ArrayUtils.isNotEmpty(advertisementBlobKeys)) {
					gameInstance.setAdvertisementBlobKeys(advertisementBlobKeys);
					gameState.setCurrentAdvertisementIndex(0);
				} else if (game.hasAdvertisements()) {
					LOGGER.warn("Game has advertisements defined and enabled, but advertisement provider counter is empty/missing");
				}
				
				return gameInstance.getAsString();
			}
		});
		updateGameState(gameInstanceId, updatedGameInstance.getGameState());
		return updatedGameInstance;
	}
	
	private void updateGameState(String gameInstanceId, GameState gameState) {
		if (gameState != null) {
			activeGameInstanceDao.updateStatus(gameInstanceId, gameState.getGameStatus(), gameState.getGameEndStatus());
		}
	}

	private Double getUserHandicap(String userId) {
		final Profile profile = commonProfileAerospikeDao.getProfile(userId);
		return profile != null ? profile.getHandicap() : null;
	}

	private List<Question> getQuestions(String userId, final String mgiId, final Game game, String userLanguage,
			String[] questionPools, int numberOfQuestions, int skipsAvailable) {
		final List<Question> questions;
		if (game.isTypeOf(GameType.USER_TOURNAMENT, GameType.PLAYOFF_TOURNAMENT, GameType.TOURNAMENT, GameType.QUIZ24)) {//different set of questions
			questions = questionSelector.getQuestions(userId, game, userLanguage, questionPools, numberOfQuestions, skipsAvailable);
		} else if (game.isTypeOf(GameType.DUEL, GameType.LIVE_TOURNAMENT, GameType.USER_LIVE_TOURNAMENT)) { //same set of questions
			questions = preselectQuestions(userId, game, userLanguage, questionPools, numberOfQuestions, skipsAvailable, mgiId);
		} else{
			throw new IllegalStateException("Game type not supported for question selection");
		}
		return questions;
	}

	private List<Question> preselectQuestions(String userId, Game game, String userLanguage, String[] questionPools,
			int numberOfQuestions, int skipsAvailable, String mgiId) {
		List<QuestionIndex> questionIndexesByMgi = preselectedQuestionsDao.getQuestionIndexesByMgi(mgiId);
		if (questionIndexesByMgi == null) {
			if (!game.hasInternationalQuestions()) { // only single language configured for game
				List<Question> questions = questionSelector.getQuestions(userId, game, userLanguage, questionPools, numberOfQuestions, skipsAvailable);
				questionIndexesByMgi = questions.stream()
						.map(q -> q.getMultiIndex())
						.collect(Collectors.toList());
			} else {
				questionIndexesByMgi = questionSelector.getInternationQuestionIndexes(game, questionPools, numberOfQuestions, skipsAvailable);
			}
			questionIndexesByMgi = saveOrGetExistingQuestionIndexes(userId, mgiId, questionIndexesByMgi);
		}
		return questionSelector.getQuestions(questionIndexesByMgi, userLanguage);
	}
	
	private List<QuestionIndex> saveOrGetExistingQuestionIndexes(String userId, String mgiId,
			List<QuestionIndex> questionIndexesByMgi) {
		List<QuestionIndex> questionIndexes;
		try {
			preselectedQuestionsDao.create(mgiId, userId, questionIndexesByMgi);
			questionIndexes = questionIndexesByMgi;
		} catch (AerospikeException aEx) {
			if (aEx.getResultCode() == ResultCode.KEY_EXISTS_ERROR) {
				// Another friend has created the game instance first
				questionIndexes = preselectedQuestionsDao.getQuestionIndexesByMgi(mgiId);
			} else {
				throw aEx;
			}
		}
		return questionIndexes;
	}
	
	protected void validateGameRegistrationPermissions(EntryFee entryFee, ClientInfo clientInfo) {
		Profile profile = commonProfileAerospikeDao.getProfile(clientInfo.getUserId());
		GameUtil.validateUserAgeForEntryFee(entryFee, profile);
		if (ArrayUtils.isNotEmpty(clientInfo.getRoles())) {
			final Set<String> userPermissions = gameEngineMessageSchemaMapper.getRolePermissions(clientInfo.getRoles());
			userPermissions.retainAll(registerMessagePermissions); // intersection of sets
			GameUtil.validateUserPermissionsForEntryFee(entryFee, userPermissions);
		} else {
			throw new F4MInsufficientRightsException("User roles required");
		}
	}

	private Game getGame(String gameId) {
		return gameId != null 
				? gameDAO.getGame(gameId) 
				: null;
	}

	/* (non-Javadoc)
	 * @see de.ascendro.f4m.service.game.engine.server.GameEngine#performNextGameStep(java.lang.String)
	 */
	@Override
	public GameInstance performNextGameStepOrQuestion(String gameInstanceId, long messageReceiveTimestamp, boolean skipped) throws F4MGameFlowViolation{
		GameInstance updatedGameInstance = gameInstanceDao.update(gameInstanceId, new UpdateGameInstanceAction(jsonUtil) {
			@Override
			public String updateGameInstance(GameInstance gameInstance) {
				if(gameInstance.getGameState().getGameStatus() == GameStatus.READY_TO_PLAY){
					startPlaying(gameInstance);
				}else{
					nextStepOrQuestion(gameInstance, skipped);
				}

				return gameInstance.getAsString();
			}
			
			private void startPlaying(GameInstance gameInstance){
				gameInstance.startPlaying(messageReceiveTimestamp);				
				gameHistoryManagerImpl.recordStartPlaying(gameInstance.getUserId(), gameInstanceId);
			}

			private void nextStepOrQuestion(GameInstance gameInstance, boolean skipped) {
                final boolean isLiveGame = gameInstance.getGame().isLiveGame();
				final GameState state = gameInstance.getGameState();
                final int numberOfQuestions = gameInstance.getNumberOfQuestionsIncludingSkipped();
				final boolean hasNextStep = state.nextStep(isLiveGame);
				if (skipped || !hasNextStep) {
					if (state.hasNextQuestion(numberOfQuestions)) {
						final int currentQuestionIndex = state.getCurrentQuestionStep().getQuestion();
						final Question nextQuestion = gameInstance.getQuestion(currentQuestionIndex + 1);
						state.nextQuestion(nextQuestion, isLiveGame);
					} else {
						LOGGER.error("Attempted to perform next step on gameInstance[{}] with current question step [{}], status[{}] and numberOfQuestions [{}]", gameInstance.getId(),
								gameInstance.getGameState().getCurrentQuestionStep(), gameInstance.getGameState().getGameStatus(), numberOfQuestions);
						throw new F4MGameFlowViolation("No next questions or step to continue with");
					}
				}
			}
		});
		updateGameState(gameInstanceId, updatedGameInstance.getGameState());
		return updatedGameInstance;
	}
	
	@Override
	public GameInstance performEndGame(String gameInstanceId) throws F4MGameFlowViolation {
		final GameInstance updatedGameInstance = gameInstanceDao.update(gameInstanceId, new UpdateGameInstanceAction(jsonUtil) {

			@Override
			public String updateGameInstance(GameInstance gameInstance) {
				if (!gameInstance.hasAnyStepOrQuestion()) {					
					final GameState state = gameInstance.getGameState();
					state.endGame(gameInstance);
					gameInstance.setEndDateTime(DateTimeUtil.getCurrentDateTime());
					gameHistoryManagerImpl.recordGameCompleted(gameInstance.getUserId(), gameInstanceId);				
				}else{
					throw new F4MGameFlowViolation("Not all questions and steps finished to proceed with game end action");
				}
				return gameInstance.getAsString();
			}
		});
		updateGameState(gameInstanceId, updatedGameInstance.getGameState());
		return updatedGameInstance;
	}
	
	@Override
	public GameInstance forceEndGame(String gameInstanceId) {
		final GameInstance updatedGameInstance = gameInstanceDao.update(gameInstanceId, new UpdateGameInstanceAction(jsonUtil) {

			@Override
			public String updateGameInstance(GameInstance gameInstance) {
				final GameState state = gameInstance.getGameState();
				state.endGame(gameInstance);
				gameInstance.setEndDateTime(DateTimeUtil.getCurrentDateTime());
				gameHistoryManagerImpl.recordGameCompleted(gameInstance.getUserId(), gameInstanceId);				
				return gameInstance.getAsString();
			}
		});
		updateGameState(gameInstanceId, updatedGameInstance.getGameState());
		return updatedGameInstance;
	}

	/* (non-Javadoc)
	 * @see de.ascendro.f4m.service.game.engine.server.GameEngine#answerQuestion(java.lang.String, java.lang.String, int, java.lang.Long, long, java.lang.String[])
	 */
	@Override
	public GameInstance answerQuestion(String clientId, String gameInstanceId, int question, Long clientMsT,
			long receiveTimeStamp, String[] answers) {
		final Long propagationDealy = healthCheckManager.getGameInstancePropagationDelay(clientId);
		final GameInstance updatedGameInstance = gameInstanceDao.update(gameInstanceId, new UpdateGameInstanceAction(jsonUtil) {
			@Override
			public String updateGameInstance(GameInstance gameInstance) {
				final GameState gameState = gameInstance.getGameState();

				final QuestionStep currentQuestionStep = gameState.getCurrentQuestionStep();
				if(currentQuestionStep != null && currentQuestionStep.getQuestion() == question){
					final Answer answer = gameState.getAnswer(question);
					if(answer == null || ArrayUtils.isEmpty(answer.getAnswers())){
						gameState.registerAnswer(question, answers, clientMsT, propagationDealy, receiveTimeStamp);
					}else{
						throw new F4MGameQuestionAlreadyAnswered("Question already answered for game instance[" + gameInstanceId + "] question[" + question + "]");
					}
				}else{
					throw new F4MUnexpectedGameQuestionAnswered("Unexpected question answered for game instance["
							+ gameInstanceId + "] question[" + question + "], current state is ["
							+ (currentQuestionStep != null ? currentQuestionStep.getQuestion() : null) + "]");
				}

				return gameInstance.getAsString();
			}
		});
		updateGameState(gameInstanceId, updatedGameInstance.getGameState());
		return updatedGameInstance;
	}

	public long getServerMsT(String gameInstanceId, int question) {
		if (gameInstanceId != null) {
			GameInstance gameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
			final GameState gameState = gameInstance.getGameState();
			return gameState.getServerMsT(question)[0];
		} else return 0;

	}

	@Override
	public GameInstance registerStepSwitch(String clientId, String gameInstanceId, Long clientMsT,
			long receiveTimeStamp) {
		final Long propagationDealy = healthCheckManager.getGameInstancePropagationDelay(clientId);
		final GameInstance updatedGameInstance = gameInstanceDao.update(gameInstanceId, new UpdateGameInstanceAction(jsonUtil) {
			@Override
			public String updateGameInstance(GameInstance gameInstance) {
				final GameState gameState = gameInstance.getGameState();

				// check if this call is made following a showAdvertisement call or not
				if (!gameState.isAdvertisementSentBeforeNextStep()) {
					gameState.registerStepSwitch(clientMsT, propagationDealy, receiveTimeStamp);
				} else {
					gameState.setAdvertisementSentBeforeNextStep(false);
				}
				return gameInstance.getAsString();
			}
		});
		final GameEndStatus gameEndStatus = updatedGameInstance.getGameState().getGameEndStatus();
		if (gameEndStatus != GameEndStatus.CALCULATING_RESULT && gameEndStatus != GameEndStatus.CALCULATED_RESULT &&
				gameEndStatus != GameEndStatus.TERMINATED) {
			updateGameState(gameInstanceId, updatedGameInstance.getGameState());
		}
		return updatedGameInstance;
	}	
	
	private void validateIfHasPaidEntreeFee(Game game, CustomGameConfig customGameConfig, GameState gameState, String userId) {
		Validate.notNull(game, "Registered game configurations are required for entry fee validation");
		Validate.notNull(gameState, "Game entry fee transaction id presense can be validated only via GameState");
		
		boolean canPlay = userGameAccessService.canPlay(game, userId);
		
		if (!GameUtil.isFreeGame(game, customGameConfig) && !gameState.hasEntryFeeTransactionId()
				&& !GameUtil.haveMultipleGamesPurchase(game) && !canPlay) {
			throw new F4MGameRequireEntryFeeException("No entry fee transaction id registered for paid game");
		}
		
	}
	
	@Override
	public void closeUpGameIntanceWithFailedResultCalculation(String userId, String gameInstanceId) {
		gameHistoryManagerImpl.recordMultiplayerResultsNotCalculated(userId, gameInstanceId);
		gameInstanceDao.updateEndStatusToResultsCalculationFailed(gameInstanceId);
		activeGameInstanceDao.delete(gameInstanceId);
	}
	
	@Override
	public GameInstance closeUpGameIntanceWithSuccessfulResultCalculation(String gameInstanceId) {
		final GameInstance gameInstance = gameInstanceDao.updateEndStatusToResultsCalculated(gameInstanceId);
		final String userId = gameInstance.getUserId();
		gameHistoryManagerImpl.recordMultiplayerResultsCalculated(userId, gameInstanceId);
		gameInstanceDao.updateEndStatusToResultsCalculated(gameInstanceId);

		//Multiplayer game end checks		
		if (gameInstance.getGame().isMultiUserGame()) {
			if (gameInstance.getGame().getType().isLive()) {
				multiplayerGameManager.markTournamentAsEnded(gameInstance.getMgiId());
			}
			multiplayerGameManager.markUserResultsAsCalculated(gameInstance.getMgiId(), gameInstance.getId(), userId);
		}
		activeGameInstanceDao.delete(gameInstance.getId());
		return gameInstance;
	}
	
	@Override
	public void closeUpGameIntanceWithFailedProcessing(ClientInfo clientInfo, String gameInstanceId, String errorMessage) {
		gameHistoryManagerImpl.calculateIfUnfinished(clientInfo, gameInstanceId, CloseUpReason.BACKEND_FAILS,
				errorMessage, RefundReason.BACKEND_FAILED);
		activeGameInstanceDao.delete(gameInstanceId);
	}

	@Override
	public Map<JokerType, JokerInformation> getJokerInformation(String gameInstanceId, int questionIndex) {
		GameInstance gameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		if (gameInstance == null) {
			throw new F4MEntryNotFoundException();
		}

		Map<JokerType, JokerInformation> result = new EnumMap<>(JokerType.class);
		for (JokerType type : JokerType.values()) {
			result.put(type, getJokerInformation(type, gameInstance, questionIndex));
		}
		return result;
	}

	@Override
	public boolean initiateJokerPurchase(JokerType jokerType, SessionWrapper sessionWrapper,
			JsonMessage<? extends PurchaseJokerRequest> message) throws F4MEntryNotFoundException {
		GameInstance gameInstance = gameInstanceDao.getGameInstance(message.getContent().getGameInstanceId());
		if (gameInstance == null) {
			throw new F4MEntryNotFoundException();
		}
		if (gameInstance.getGameState().getGameStatus() == GameStatus.CANCELLED) {
			throw new F4MGameCancelledException("Referred game instance state is cancelled");
		}

		JokerInformation jokerInformation = getJokerInformation(jokerType, gameInstance, message.getContent().getQuestion());
		if (! jokerInformation.isAvailableForCurrentQuestion()) {
			throw new F4MJokerNotAvailableException();
		}

		if (jokerInformation.getPrice() == null || jokerInformation.getCurrency() == null || jokerInformation.isAlreadyPurchasedForCurrentQuestion()) {
			// no price or already bought for current question => can process immediately
			return true;
		} else {
			// Otherwise payment has to be made first
			messageCoordinator.requestTransferJokerPurchaseFee(gameInstance.getGame(), jokerInformation.getPrice(),
					jokerInformation.getCurrency(), sessionWrapper, message);
			return false;
		}
	}

	private JokerInformation getJokerInformation(JokerType type, GameInstance gameInstance, int questionIndex) {
		JokerConfiguration configuration = gameInstance.getJokerConfigurationIfAvailable(type);
		if (configuration != null && configuration.isEnabled()) {
			Map<JokerType, Set<Integer>> jokersUsed = gameInstance.getJokersUsed();
			Set<Integer> jokersUsedOnQuestions = jokersUsed.get(type);
			boolean alreadyPurchasedForCurrentQuestion = jokersUsedOnQuestions != null
					&& jokersUsedOnQuestions.contains(questionIndex);
			boolean jokerAvailableForCurrentQuestion = isJokerAvailableForQuestion(gameInstance, type, questionIndex);

			if (configuration.getAvailableCount() == null) {
				// Unlimited
				return new JokerInformation(null, jokerAvailableForCurrentQuestion, alreadyPurchasedForCurrentQuestion,
						configuration.getCurrency(), configuration.getPrice());
			} else {
				// Limited
				Set<Integer> jokerUsed = jokersUsed.get(type);
				int usedJokers = jokerUsed == null ? 0 : jokerUsed.size();
				int availableCount = configuration.getAvailableCount() - usedJokers;
				return new JokerInformation(availableCount, availableCount > 0 && jokerAvailableForCurrentQuestion,
						alreadyPurchasedForCurrentQuestion, configuration.getCurrency(), configuration.getPrice());
			}
		}
		return new JokerInformation(0, false, false, null, null);
	}

	private boolean isJokerAvailableForQuestion(GameInstance gameInstance, JokerType jokerType, int questionIndex) {
		if (gameInstance.getQuestion(questionIndex) == null) {
			// No question by index => no joker available
			return false;
		}

		Game game = gameInstance.getGame();
		if (game.getType() != GameType.QUIZ24) {
			// Available only for quick quizzes
			return false;
		}

		if (jokerType == JokerType.HINT && gameInstance.getQuestionHint(questionIndex) == null) {
			// Hint available only if present in current question
			return false;
		}

		if (jokerType == JokerType.FIFTY_FIFTY && gameInstance.calculateQuestionRemovedAnswers(questionIndex) == null) {
			// 50/50 available only if enough removable questions present
			return false;
		}

		return true;
	}

	@Override
	public GameInstance finalizeJokerPurchase(JokerType jokerType, String gameInstanceId, int questionIndex) {
		validateIfGameNotCancelled(gameInstanceId);
		return gameInstanceDao.update(gameInstanceId, new UpdateGameInstanceAction(jsonUtil) {
			@Override
			public String updateGameInstance(GameInstance gameInstance) {
				final GameState gameState = gameInstance.getGameState();
				final QuestionStep currentQuestionStep = gameState.getCurrentQuestionStep();
				if (currentQuestionStep == null || currentQuestionStep.getQuestion() != questionIndex) {
					throw new F4MUnexpectedGameQuestionAnswered("Unexpected question skipped for game instance ["
							+ gameInstanceId + "] question [" + questionIndex + "], current state is ["
							+ (currentQuestionStep != null ? currentQuestionStep.getQuestion() : null) + "]");
				} else if (jokerType == JokerType.SKIP) {
					// Skips may be done only before answered
					final Answer answer = gameState.getAnswer(questionIndex);
					if (answer != null && ArrayUtils.isNotEmpty(answer.getAnswers())) {
						throw new F4MGameQuestionAlreadyAnswered("Question already answered for game instance [" + gameInstanceId + "] question [" + questionIndex + "]");
					}
				}

				JokerInformation jokerInformation = getJokerInformation(jokerType, gameInstance, questionIndex);
				if (! jokerInformation.isAvailableForCurrentQuestion()) {
					throw new F4MJokerNotAvailableException();
				}

				Map<JokerType, Set<Integer>> jokersUsed = gameInstance.getJokersUsed();
				Set<Integer> questionIndexesForJoker = jokersUsed.get(jokerType);
				if (questionIndexesForJoker == null) {
					questionIndexesForJoker = new HashSet<>();
				}
				questionIndexesForJoker.add(questionIndex);
				jokersUsed.put(jokerType, questionIndexesForJoker);
				gameInstance.setJokersUsed(jokersUsed);

				return gameInstance.getAsString();
			}
		});
	}

}
