package de.ascendro.f4m.service.game.engine.server;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.ascendro.f4m.server.game.GameUtil;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypes;
import de.ascendro.f4m.service.game.engine.advertisement.AdvertisementUtil;
import de.ascendro.f4m.service.game.engine.exception.F4MGameFlowViolation;
import de.ascendro.f4m.service.game.engine.exception.F4MAllowedGamePlaysExhaustedException;
import de.ascendro.f4m.service.game.engine.exception.F4MQuestionCannotBeReadFromPool;
import de.ascendro.f4m.service.game.engine.exception.F4MQuestionsNotAvailableInPool;
import de.ascendro.f4m.service.game.engine.health.HealthCheckManager;
import de.ascendro.f4m.service.game.engine.health.HealthCheckRequestInfoImpl;
import de.ascendro.f4m.service.game.engine.joker.JokerRequestHandler;
import de.ascendro.f4m.service.game.engine.model.CloseUpReason;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.engine.model.answer.AnswerQuestionRequest;
import de.ascendro.f4m.service.game.engine.model.answer.AnswerQuestionResponse;
import de.ascendro.f4m.service.game.engine.model.answer.NextStepRequest;
import de.ascendro.f4m.service.game.engine.model.cancel.CancelGameRequest;
import de.ascendro.f4m.service.game.engine.model.joker.JokerInformation;
import de.ascendro.f4m.service.game.engine.model.joker.JokersAvailableRequest;
import de.ascendro.f4m.service.game.engine.model.joker.JokersAvailableResponse;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseJokerRequest;
import de.ascendro.f4m.service.game.engine.model.ready.ReadyToPlayRequest;
import de.ascendro.f4m.service.game.engine.model.register.RegisterRequest;
import de.ascendro.f4m.service.game.engine.model.register.RegisterResponse;
import de.ascendro.f4m.service.game.engine.model.start.game.StartGameRequest;
import de.ascendro.f4m.service.game.engine.model.start.game.StartGameResponse;
import de.ascendro.f4m.service.game.engine.server.subscription.EventSubscriptionManager;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.JokerConfiguration;
import de.ascendro.f4m.service.game.selection.model.game.JokerType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.singleplayer.SinglePlayerGameParameters;
import de.ascendro.f4m.service.game.usergameaccess.UserGameAccessService;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.util.ServiceUtil;

/**
 * Game Engine via Web Socket connection received message handler
 */
public class GameEngineServiceServerMessageHandler extends DefaultJsonMessageHandler {
	private static final String CLIENT_ID_MANDATORY_MESSAGE = "Client id is mandatory";
	private static final Logger LOGGER = LoggerFactory.getLogger(GameEngineServiceServerMessageHandler.class);

	private final GameEngine gameEngine;
	private final JokerRequestHandler jokerRequestHandler;
	private final HealthCheckManager healthCheckManager;
	private final EventSubscriptionManager subscriptionManager;
	private final ServiceUtil serviceUtil;
	private final CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao;
	private final MessageCoordinator serviceCommunicator;
	private final UserGameAccessService specialGameService;
	private final ScheduledExecutorService scheduledExecutorService;
	
	private enum GameRegisterStatus {
		PROCEED_AND_DECREMENT_ACCESS_COUNT, PROCEED, DONT_PROCEED
	}	

	public GameEngineServiceServerMessageHandler(GameEngine gameEngine, 
			JokerRequestHandler jokerRequestHandler,HealthCheckManager healthCheckManager,
			ServiceUtil serviceUtil, CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao,
			EventSubscriptionManager subscriptionManager, MessageCoordinator messageCoordinator, UserGameAccessService gameChecker) {
		this.gameEngine = gameEngine;
		this.jokerRequestHandler = jokerRequestHandler;
		this.healthCheckManager = healthCheckManager;
		this.serviceUtil = serviceUtil;
		this.subscriptionManager = subscriptionManager;
		this.commonMultiplayerGameInstanceDao = commonMultiplayerGameInstanceDao;
		this.serviceCommunicator = messageCoordinator;
		this.specialGameService = gameChecker;
		scheduledExecutorService = Executors.newScheduledThreadPool(5, 
				new ThreadFactoryBuilder().setNameFormat("Delayed-response-task-%d").build());
	}

	@Override
	public JsonMessageContent onUserMessage(RequestContext context) {
		final GameEngineMessageTypes gameEngineMessageType = context.getMessage().getType(GameEngineMessageTypes.class);
		if (gameEngineMessageType != null) {
			return onGameEngineMessage(context, gameEngineMessageType);
		} else {
			throw new F4MValidationFailedException("Unrecognized message " + context.getMessage().getName());
		}
	}

	@SuppressWarnings("unchecked")
	protected JsonMessageContent onGameEngineMessage(RequestContext context, GameEngineMessageTypes gameEngineMessageType) {
		ClientInfo clientInfo = context.getClientInfo();
		JsonMessage<? extends JsonMessageContent> message = context.getMessage();

		switch (gameEngineMessageType) {
		case REGISTER:
			return onRegister(clientInfo, (JsonMessage<RegisterRequest>) message);
		case START_GAME:
			return onStartGame(clientInfo, message);
		case READY_TO_PLAY:
			Validate.notEmpty(message.getClientId(), CLIENT_ID_MANDATORY_MESSAGE);
			return onReadyToPlay((JsonMessage<ReadyToPlayRequest>) message, serviceUtil.getMessageTimestamp());
		case ANSWER_QUESTION:
			Validate.notEmpty(message.getClientId(), CLIENT_ID_MANDATORY_MESSAGE);
			return onAnswerQuestion((JsonMessage<AnswerQuestionRequest>) message, serviceUtil.getMessageTimestamp());
		case NEXT_STEP:
			Validate.notEmpty(message.getClientId(), CLIENT_ID_MANDATORY_MESSAGE);
			onNextStep((JsonMessage<NextStepRequest>) message, serviceUtil.getMessageTimestamp());
			return null;
		case HEALTH_CHECK_RESPONSE:
			onHealthCheckResponse(message, context);
			return null;
		case CANCEL_GAME:
			return onCancelGame((JsonMessage<CancelGameRequest>) message);
		case JOKERS_AVAILABLE:
			return onJokersAvailable((JsonMessage<JokersAvailableRequest>) message);
		case PURCHASE_HINT:
		case PURCHASE_FIFTY_FIFTY:
		case PURCHASE_IMMEDIATE_ANSWER:
		case PURCHASE_SKIP:
			return jokerRequestHandler.onPurchaseJoker((JsonMessage<? extends PurchaseJokerRequest>) message, getSessionWrapper());
		default:
			throw new F4MValidationFailedException("Unsupported message type[" + gameEngineMessageType + "]");
		}
	}

	private JsonMessageContent onJokersAvailable(JsonMessage<JokersAvailableRequest> message) {
		Map<JokerType, JokerInformation> items = gameEngine.getJokerInformation(message.getContent().getGameInstanceId(), message.getContent().getQuestion());
		return new JokersAvailableResponse(items);
	}

	private JsonMessageContent onCancelGame(JsonMessage<CancelGameRequest> cancelGameRequestMessage) {
		serviceCommunicator.cancelGame(cancelGameRequestMessage, getSessionWrapper());
		return new EmptyJsonMessageContent();
	}
	
	private RegisterResponse onRegister(ClientInfo clientInfo, JsonMessage<RegisterRequest> message) {
		final RegisterRequest registerRequest = message.getContent();

		final String gameId = registerRequest.getGameId();
		final String mgiId = registerRequest.getMgiId();
		final Game game = serviceCommunicator.getGameForRegistration(mgiId, gameId);
		final CustomGameConfig customGameConfig = mgiId != null ? commonMultiplayerGameInstanceDao.getConfig(mgiId) : null;
		
		//Validate if game has open registration (important to perform validation before paid for a game)
		if (mgiId == null) {
			gameEngine.validateSingleGameForRegistration(clientInfo, game,
					Optional.ofNullable(registerRequest.getSinglePlayerGameConfig()).orElse(new SinglePlayerGameParameters()));
		} else {
			gameEngine.validateMultiplayerGameForRegistration(clientInfo, mgiId, customGameConfig);
		}
		
		final String gameInstanceId;
		final RegisterResponse registerResponse;
		GameRegisterStatus canProceed = canProceedToGameplayAndRegister(game,customGameConfig,clientInfo);

		if (GameRegisterStatus.PROCEED_AND_DECREMENT_ACCESS_COUNT.equals(canProceed) ||
				GameRegisterStatus.PROCEED.equals(canProceed)) {
			if(game.getType() == GameType.QUIZ24){
				gameInstanceId = gameEngine.registerForFreeQuiz24(clientInfo, gameId, registerRequest.getSinglePlayerGameConfig());
				
			}else if(game.isMultiUserGame()){
				gameInstanceId = gameEngine.registerForMultiplayerGame(clientInfo, mgiId, null, null, null);
			}else{
				throw new F4MFatalErrorException("Unsupported free game type");
			}
			
			registerResponse = new RegisterResponse(gameInstanceId);
		} else if (!specialGameService.isVisible(game, clientInfo.getUserId() )) {
			throw new F4MAllowedGamePlaysExhaustedException();
		} else { //postpone registration until entry fee transfered
				serviceCommunicator.requestTransferGameEntryFee(getSessionWrapper(), message, mgiId, game, customGameConfig);
			registerResponse = null;
		}
		return registerResponse;
	}
	
	private GameRegisterStatus canProceedToGameplayAndRegister(Game game, CustomGameConfig customGameConfig, ClientInfo clientInfo) {
		//in case of simple free game, check if registration is needed and register it
		if (GameUtil.haveMultipleGamesPurchase(game) && GameUtil.isFreeGame(game, customGameConfig)) {
			specialGameService.grantAccess(game, clientInfo.getUserId());
		}
		
		GameRegisterStatus canProceed = GameRegisterStatus.DONT_PROCEED;
		
		/*
		 * Users can proceed to gameplay on following occasions
		 * 1. This is free game
		 * 2. This is multiplePurchases game and user has already paid for it
		 * 3. this is paid single purchase game that was multi purchase game before and user has paid for it
		 */
		if (GameUtil.isFreeGame(game, customGameConfig) && !GameUtil.haveMultipleGamesPurchase(game)){
			canProceed = GameRegisterStatus.PROCEED; 
		} else if (GameUtil.haveMultipleGamesPurchase(game) && specialGameService.havePlaysLeft(game, clientInfo.getUserId())){
			canProceed = GameRegisterStatus.PROCEED_AND_DECREMENT_ACCESS_COUNT;
		} else if (!GameUtil.isFreeGame(game, customGameConfig) && specialGameService.havePlaysLeft(game, clientInfo.getUserId())){
			canProceed = GameRegisterStatus.PROCEED_AND_DECREMENT_ACCESS_COUNT;
		}
		
		if (GameRegisterStatus.PROCEED_AND_DECREMENT_ACCESS_COUNT.equals(canProceed)) {
			specialGameService.registerAccess(game, clientInfo.getUserId());
		}
		
		return canProceed;
	}
	

	private void onNextStep(JsonMessage<NextStepRequest> nextStepRequestMessage, long receiveTimestamp) {
		final NextStepRequest nextStepRequest = nextStepRequestMessage.getContent();
		final String gameInstanceId = nextStepRequest.getGameInstanceId();
		gameEngine.validateIfGameNotCancelled(gameInstanceId);
		final GameInstance gameInstance = gameEngine.registerStepSwitch(nextStepRequestMessage.getClientId(),
				gameInstanceId, nextStepRequest.getClientMsT(), receiveTimestamp);
		performNextGameAction(receiveTimestamp, gameInstance, nextStepRequestMessage.getClientInfo());
	}

	private boolean isLive(GameInstance gameInstance) {
		return gameInstance.getGame().isLiveGame();
	}

	protected StartGameResponse onStartGame(ClientInfo clientInfo, JsonMessage<? extends JsonMessageContent> message) {
		final StartGameRequest startGameRequest = (StartGameRequest) message.getContent();
		String gameInstanceId = startGameRequest.getGameInstanceId();

		final boolean waitForOtherServiceResponse = serviceCommunicator.reserveUserVoucher(gameInstanceId, message,
				getSessionWrapper());

		StartGameResponse response = null;

		if (!waitForOtherServiceResponse) {
			final GameInstance gameInstance = serviceCommunicator.startGame(gameInstanceId,
					clientInfo.getUserId(), startGameRequest.getUserLanguage());
            if (gameInstance.getGame().isDuel() && startGameRequest.getMultiplayerGameInstanceId() != null) {
				try {
					serviceCommunicator.requestActivateInvitations(startGameRequest.getMultiplayerGameInstanceId(), clientInfo);

				} catch (Exception e) {
					LOGGER.debug("StartGameResponse onStartGame e {}", e.getMessage());

				}
            }
			response = serviceCommunicator.prepareStartGameResponse(gameInstance, clientInfo);
		}

		return response;
	}
	
	protected EmptyJsonMessageContent onReadyToPlay(JsonMessage<ReadyToPlayRequest> readyToPlayRequestMessage, long messageReceiveTimestamp) {
		final ReadyToPlayRequest readyToPlayRequest = readyToPlayRequestMessage.getContent();
		final String gameInstanceId = readyToPlayRequest.getGameInstanceId();
		GameInstance gameInstance = gameEngine.validateIfGameNotCancelled(gameInstanceId);
		
		if(gameInstance.getGameState().getGameStatus() == GameStatus.PREPARED){//Ready to play should be called only once		
			final boolean hasAwaitingAdvertisement = AdvertisementUtil.hasAwaitingAdvertisement(gameInstance, GameStatus.READY_TO_PLAY);
			final boolean startImmediately = !hasAwaitingAdvertisement && !gameInstance.getGame().getType().isLive();
			
			gameInstance = gameEngine.readyToPlay(gameInstanceId, messageReceiveTimestamp, startImmediately);
	
			serviceCommunicator.sendHealthCheck(getSessionWrapper(), readyToPlayRequestMessage.getClientId());
			if(hasAwaitingAdvertisement){
				serviceCommunicator.sendShowAdvertisement(getSessionWrapper(), gameInstance,
						readyToPlayRequestMessage.getClientInfo());
			}else{
				if (gameInstance.getGame().isLiveGame()) {
					subscriptionManager.subscribeClientGameInstanceId( readyToPlayRequestMessage.getClientId(), gameInstance);
				}else{
					serviceCommunicator.sendStartStep(getSessionWrapper(), readyToPlayRequestMessage.getClientId(), gameInstance);
				}
			}
			return new EmptyJsonMessageContent();
		}else{
			throw new F4MGameFlowViolation("Repeated ready to play call, as game instance in "
					+ gameInstance.getGameState().getGameStatus() + " status");
		}
	}

	protected AnswerQuestionResponse onAnswerQuestion(JsonMessage<AnswerQuestionRequest> answerQuestionRequestMessage, long receiveTimestamp) {
		final AnswerQuestionRequest answerQuestionRequest = answerQuestionRequestMessage.getContent();
		if (answerQuestionRequest != null && answerQuestionRequest.getClientMsT() != null) {
			final String clientId = answerQuestionRequestMessage.getClientId();
			final String gameInstanceId = answerQuestionRequest.getGameInstanceId();
			final int answerQuestionIndex = answerQuestionRequest.getQuestion();
			gameEngine.validateIfGameNotCancelled(gameInstanceId);
			final GameInstance gameInstance = gameEngine.answerQuestion(clientId,
					gameInstanceId, answerQuestionIndex,
					answerQuestionRequest.getClientMsT(), receiveTimestamp, answerQuestionRequest.getAnswers());
			final boolean immediateAnswerPurchased = ! gameInstance.getJokersUsed(JokerType.IMMEDIATE_ANSWER).isEmpty();
			final ClientInfo clientInfo = answerQuestionRequestMessage.getClientInfo();
			if (immediateAnswerPurchased || gameInstance.instantlySendAnswer()) {
				// If immediate answer was purchased, delay sending of next action by the time configured to show the answer
				// Same has to be done also if instantAnswerFeedback is set in game configuration
				final Integer displayTime;
				if (gameInstance.instantlySendAnswer()) {
					displayTime = gameInstance.getGame().getInstantAnswerFeedbackDelay();
				} else {
					final JokerConfiguration immediateAnswerConfig = gameInstance.getGame().getJokerConfiguration().get(JokerType.IMMEDIATE_ANSWER);
					displayTime = immediateAnswerConfig == null ? null : immediateAnswerConfig.getDisplayTime();
				}
				scheduledExecutorService.schedule(() -> performNextActionAfterQuestionAnswered(gameInstance, answerQuestionIndex, clientInfo, receiveTimestamp), 
						displayTime == null ? 1 : displayTime, TimeUnit.SECONDS);
			} else {
				performNextActionAfterQuestionAnswered(gameInstance, answerQuestionIndex, clientInfo, receiveTimestamp);
			}
			return getAnswerQuestionResponse(gameInstance, answerQuestionIndex, immediateAnswerPurchased, gameEngine.getServerMsT(gameInstanceId, answerQuestionIndex));
		} else {
			throw new F4MValidationFailedException("Message tClientMs missing");
		}
	}

	private void performNextActionAfterQuestionAnswered(GameInstance gameInstance, int answerQuestionIndex, ClientInfo clientInfo, long receiveTimestamp) {
		if(AdvertisementUtil.hasAwaitingAdvertisement(gameInstance, GameStatus.IN_PROGRESS)) {
			serviceCommunicator.sendShowAdvertisement(getSessionWrapper(), gameInstance, clientInfo);
			// FIXME: not the best solution - most likely front-end should send nextStep after each advertisement;
			// for now we just force gameEnd for live tournament (#10566)
			if (isLive(gameInstance)) {
				performNextGameAction(receiveTimestamp, gameInstance, clientInfo);
			}
		} else if (answerQuestionIndex >= gameInstance.getNumberOfQuestionsIncludingSkipped()){
			throw new F4MValidationFailedException("Question attempted to answer is out of expected bounds: " + gameInstance.getNumberOfQuestionsIncludingSkipped());
		} else {
			performNextGameAction(receiveTimestamp, gameInstance, clientInfo);
		}
	}

	private void performNextGameAction(long receiveTimestamp, final GameInstance gameInstance, ClientInfo clientInfo) {
		if(gameInstance.hasAnyStepOrQuestion()){
			if(!isLive(gameInstance)){
				serviceCommunicator.performNextGameStepOrQuestion(getSessionWrapper(), clientInfo.getClientId(), gameInstance.getId(), receiveTimestamp, false);
			}
		}else if(!gameInstance.getGameState().isCompleted()){
			serviceCommunicator.performGameEnd(getSessionWrapper(), gameInstance, clientInfo);
		}
	}

    protected AnswerQuestionResponse getAnswerQuestionResponse(GameInstance gameInstance, int answerQuestionIndex, boolean immediateAnswerPurchased, long serverMsT) {
        final AnswerQuestionResponse response = new AnswerQuestionResponse(new String[0]);
        response.setImmediateAnswerPurchased(immediateAnswerPurchased);
        if (gameInstance.instantlySendAnswer() || immediateAnswerPurchased) {
            response.setCorrectAnswers(gameInstance.getQuestion(answerQuestionIndex).getCorrectAnswers());
        }
		response.setServerMsT(String.valueOf(serverMsT));
        return response;
    }

	private void onHealthCheckResponse(JsonMessage<? extends JsonMessageContent> message, RequestContext context) {
		RequestInfo originalRequest = context.getOriginalRequestInfo();
		if (originalRequest != null) {
			healthCheckManager.calculateAndStoreHealthCheck((HealthCheckRequestInfoImpl) originalRequest);
		} else {
			if (ArrayUtils.isNotEmpty(message.getAck())) {
				LOGGER.error("Original RequestInfo not found for ack {}", message.getAck()[0]);
			} else {
				throw new F4MValidationFailedException("Acknowledgement expected in health check response");
			}
		}
	}
	
	@Override
	public void onFailure(String originalMessageEncoded, RequestContext requestContext, Throwable th) {
		try {
			if (!(th instanceof F4MException) 
					|| th instanceof F4MQuestionCannotBeReadFromPool
					|| th instanceof F4MQuestionsNotAvailableInPool) {
				serviceCommunicator.closeFailedGameInstance(requestContext.getMessage(), th);
			}
		} catch (Exception e) {
			LOGGER.error("Failed to close game instance with reason {}", CloseUpReason.BACKEND_FAILS.name(), e);
		} finally {
			super.onFailure(originalMessageEncoded, requestContext, th);
		}
	}

	@PreDestroy
	public void stopScheduler() {
		if (scheduledExecutorService != null) {
			LOGGER.info("Stopping delayed response task scheduler");
			scheduledExecutorService.shutdownNow();
		}
	}

}
