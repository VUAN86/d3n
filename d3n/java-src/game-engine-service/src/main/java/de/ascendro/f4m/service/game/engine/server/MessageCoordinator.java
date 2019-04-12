package de.ascendro.f4m.service.game.engine.server;

import com.google.gson.JsonObject;
import de.ascendro.f4m.server.country.nogambling.NoGamblingCountry;
import de.ascendro.f4m.server.game.GameAerospikeDao;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypes;
import de.ascendro.f4m.service.game.engine.advertisement.AdvertisementUtil;
import de.ascendro.f4m.service.game.engine.advertisement.GameEngineAdvertisementManager;
import de.ascendro.f4m.service.game.engine.client.payment.PaymentCommunicator;
import de.ascendro.f4m.service.game.engine.client.results.ResultEngineCommunicator;
import de.ascendro.f4m.service.game.engine.client.voucher.VoucherCommunicator;
import de.ascendro.f4m.service.game.engine.dao.instance.GameInstanceAerospikeDao;
import de.ascendro.f4m.service.game.engine.feeder.QuestionFeeder;
import de.ascendro.f4m.service.game.engine.health.HealthCheckRequestInfoImpl;
import de.ascendro.f4m.service.game.engine.history.GameHistoryManager;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.game.engine.model.advertisement.ShowAdvertisementRequest;
import de.ascendro.f4m.service.game.engine.model.cancel.CancelGameRequest;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseJokerRequest;
import de.ascendro.f4m.service.game.engine.model.register.RegisterRequest;
import de.ascendro.f4m.service.game.engine.model.start.game.GameInstanceRequest;
import de.ascendro.f4m.service.game.engine.model.start.game.StartGameResponse;
import de.ascendro.f4m.service.game.engine.model.start.step.StartStepRequest;
import de.ascendro.f4m.service.game.engine.multiplayer.MultiplayerGameManager;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameWinningComponentListItem;
import de.ascendro.f4m.service.game.selection.model.game.JokerType;
import de.ascendro.f4m.service.game.selection.model.game.ResultConfiguration;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.ServiceUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

public class MessageCoordinator {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MessageCoordinator.class);
	
	private final GameEngine gameEngine;
	private final ResultEngineCommunicator resultEngineCommunicatorImpl;
	private final GameHistoryManager gameHistoryManagerImpl;
	private final ServiceUtil serviceUtil;
	private final GameInstanceAerospikeDao gameInstanceDao;
	private final GameAerospikeDao gameAerospikeDao;
	private final PaymentCommunicator paymentCommunicator;
	private final CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao;
	private final JsonMessageUtil jsonMessageUtil;
	private final QuestionFeeder questionFeeder;
	private final JsonUtil jsonUtil;
	private final VoucherCommunicator voucherCommunicator;
	private final GameEngineAdvertisementManager gameEngineAdvertisementManager;
	private final MultiplayerGameManager multiplayerGameManager;

	private final NoGamblingCountry noGamblingCountry;
	
	@Inject
	public MessageCoordinator(GameEngine gameEngine, JsonMessageUtil jsonMessageUtil,
			GameHistoryManager gameHistoryManagerImpl, GameAerospikeDao gameAerospikeDao,
			PaymentCommunicator paymentCommunicator, ResultEngineCommunicator resultEngineCommunicatorImpl,
			ServiceUtil serviceUtil, GameInstanceAerospikeDao gameInstanceDao,
			CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao, QuestionFeeder questionFeeder,
			VoucherCommunicator voucherCommunicator, JsonUtil jsonUtil, GameEngineAdvertisementManager gameEngineAdvertisementManager,
			MultiplayerGameManager multiplayerGameManager, NoGamblingCountry noGamblingCountry) {
		this.gameEngine = gameEngine;
		this.jsonMessageUtil = jsonMessageUtil;
		this.gameAerospikeDao = gameAerospikeDao;
		this.paymentCommunicator = paymentCommunicator;
		this.resultEngineCommunicatorImpl = resultEngineCommunicatorImpl;
		this.gameHistoryManagerImpl = gameHistoryManagerImpl;
		this.serviceUtil = serviceUtil;
		this.gameInstanceDao = gameInstanceDao;
		this.commonMultiplayerGameInstanceDao = commonMultiplayerGameInstanceDao;
		this.questionFeeder = questionFeeder;
		this.jsonUtil = jsonUtil;
		this.voucherCommunicator = voucherCommunicator;
		this.gameEngineAdvertisementManager = gameEngineAdvertisementManager;
		this.multiplayerGameManager = multiplayerGameManager;
		
		this.noGamblingCountry = noGamblingCountry;
	}
	
	public void sendStartStep(SessionWrapper sessionWrapper, String clientId, GameInstance gameInstance) {
		if (sessionWrapper != null) {
			LOGGER.debug("Sending startStep to {} for clientId {} and gameInstanceId {}", sessionWrapper, clientId, gameInstance.getId());
			final StartStepRequest startStepRequest = new StartStepRequest(gameInstance);
			
			final String questionId = gameInstance.getQuestion(startStepRequest.getQuestion()).getId();
			gameHistoryManagerImpl.recordQuestionsPlayed(gameInstance.getUserId(), gameInstance.getGame().getGameId(), questionId);
			// in case the client is disconnected during the game --> session is null
			sendExtraResponse(sessionWrapper, clientId, GameEngineMessageTypes.START_STEP, startStepRequest);
		}
	}
	
	public void sendShowAdvertisement(SessionWrapper sessionWrapper, GameInstance gameInstance, ClientInfo clientInfo) {		
		final ShowAdvertisementRequest showAdvertisementRequest = new ShowAdvertisementRequest(gameInstance.getId(),
				AdvertisementUtil.getAwaitingAdvertisement(gameInstance));
		gameEngineAdvertisementManager.markShowAdvertisementWasSent(gameInstance.getId());
		sendExtraResponse(sessionWrapper, clientInfo.getClientId(), GameEngineMessageTypes.SHOW_ADVERTISEMENT, showAdvertisementRequest);
	}

	public void sendHealthCheck(SessionWrapper sessionWrapper, String clientId) {
		if (sessionWrapper != null) {
			// in case the client is disconnected during the game --> session is null
			try {
				final RequestInfo healthCheckRequestInfo = new HealthCheckRequestInfoImpl(clientId, serviceUtil.getMessageTimestamp());
				final JsonMessage<EmptyJsonMessageContent> healthCheckMessage = jsonMessageUtil.createNewGatewayMessage(
						GameEngineMessageTypes.HEALTH_CHECK, new EmptyJsonMessageContent(), clientId);
				
				sessionWrapper.sendAsynMessage(healthCheckMessage, healthCheckRequestInfo, false);
			} catch (Exception e) {
				LOGGER.error("Failed to send health check to client[" + clientId + "]", e);
			}
		}
	}
	
	public void sendExtraResponse(SessionWrapper sessionWrapper, String clientId, GameEngineMessageTypes type, JsonMessageContent responseContent) {
		final JsonMessage<JsonMessageContent> extraResponseMessage = jsonMessageUtil.createNewGatewayMessage(type, responseContent, clientId);
		sessionWrapper.sendAsynMessage(extraResponseMessage);
	}
	
	private String getGameIdFromMgi(String mgiId){
		String gameId = null;
		if(!StringUtils.isBlank(mgiId)){
			final CustomGameConfig customGameConfig = commonMultiplayerGameInstanceDao.getConfig(mgiId);
			if(customGameConfig != null && !StringUtils.isBlank(customGameConfig.getGameId())){
				gameId = customGameConfig.getGameId();
			}else{
				throw new F4MEntryNotFoundException("Game id cannot be found by multiplayer game instance [" + mgiId + "]");
			}
		}
		return gameId;
	}
	
	public Game getGameForRegistration(String mgiId, String gameId){
		String gameIdFromMgi = null;
		if(!StringUtils.isBlank(mgiId)){
			gameIdFromMgi = getGameIdFromMgi(mgiId);
		}else if(StringUtils.isBlank(gameId)){
			throw new F4MValidationFailedException("Neither mgiId or gameId specified");
		}
		
		//load game
		Game game = gameAerospikeDao.getGame(firstNonNull(gameIdFromMgi, gameId));
		if (game != null) {
			return game;
		} else {
			throw new F4MEntryNotFoundException("Game not found by id [" + firstNonNull(gameIdFromMgi, gameId) + "]");
		}
	}
	
	public void requestTransferGameEntryFee(SessionWrapper sessionWrapper, JsonMessage<RegisterRequest> message,
            final String mgiId, final Game game, final CustomGameConfig customGameConfig) {
		BigDecimal entryFeeAmount;
		final Currency entryFeeCurrency;
        if(customGameConfig != null && game.isEntryFeeDecidedByPlayer()){
            entryFeeAmount = customGameConfig.getEntryFeeAmount();
            entryFeeCurrency = customGameConfig.getEntryFeeCurrency();
		} else {
            entryFeeAmount = game.getEntryFeeAmount();
            entryFeeCurrency = game.getEntryFeeCurrency();
		}

		if(entryFeeAmount != null && entryFeeCurrency != null && entryFeeAmount.compareTo(BigDecimal.ZERO) > 0 ){
			if (game.isTournament() && !game.getResultConfiguration().isJackpotCalculateByEntryFee() && game.getEntryFeeCurrency() != Currency.MONEY) {
				entryFeeAmount = new BigDecimal(0);
			}
            paymentCommunicator.requestGameEntryFeeTransfer(game.getGameId(), mgiId,
                    entryFeeAmount, entryFeeCurrency, game.getType(), sessionWrapper, message);

			if (game.isTournament() && !game.getResultConfiguration().isJackpotCalculateByEntryFee() && game.getEntryFeeCurrency() != Currency.MONEY) {
				paymentCommunicator.requestGameEntryFeeTransferBehindTarget(sessionWrapper, message,
						mgiId, game, customGameConfig);
			}

		}else{
			throw new F4MValidationFailedException("Invalid game entry fee: " + entryFeeAmount + " " + entryFeeCurrency);
		}
	}

	public void requestActivateInvitations(String mgiId, ClientInfo sourceClientInfo) {
		paymentCommunicator.requestActivateInvitations(mgiId, sourceClientInfo);
	}

	public void requestTransferJokerPurchaseFee(Game game, BigDecimal jokerPurchaseFeeAmount, Currency jokerPurchaseFeeCurrency,
			SessionWrapper sessionWrapper, JsonMessage<? extends PurchaseJokerRequest> message) {
        paymentCommunicator.requestJokerPurchaseFeeTransfer(game.getGameId(),
                jokerPurchaseFeeAmount, jokerPurchaseFeeCurrency, game.getType(), sessionWrapper, message);
	}
	
	public GameInstance performNextGameStepOrQuestion(SessionWrapper sessionWrapper, String clientId, String gameInstanceId, 
			long messageReceiveTimestamp, boolean skipped) {
		final GameInstance gameInstance = gameEngine.performNextGameStepOrQuestion(gameInstanceId, messageReceiveTimestamp, skipped);
		sendHealthCheck(sessionWrapper, clientId);
		sendStartStep(sessionWrapper, clientId, gameInstance);
		return gameInstance;
	}
	
	public void performGameEnd(SessionWrapper sessionWrapper, final GameInstance gameInstance, ClientInfo clientInfo) {
		final GameInstance endedGameInstance = gameEngine.performEndGame(gameInstance.getId());
		if(sessionWrapper != null && AdvertisementUtil.hasAwaitingAdvertisement(endedGameInstance, GameStatus.COMPLETED)){
			sendShowAdvertisement(sessionWrapper, endedGameInstance, clientInfo);
		}
		resultEngineCommunicatorImpl.requestCalculateResults(clientInfo, endedGameInstance, sessionWrapper);
	}

	public void forceGameEnd(SessionWrapper sessionWrapper, final String gameInstance, ClientInfo clientInfo) {
		final GameInstance endedGameInstance = gameEngine.forceEndGame(gameInstance);
		if(sessionWrapper != null && AdvertisementUtil.hasAwaitingAdvertisement(endedGameInstance, GameStatus.COMPLETED)){
			sendShowAdvertisement(sessionWrapper, endedGameInstance, clientInfo);
		}
		resultEngineCommunicatorImpl.requestCalculateResults(clientInfo, endedGameInstance, sessionWrapper);
	}

	public boolean reserveUserVoucher(String gameInstanceId, JsonMessage<? extends JsonMessageContent> sourceMessage,
			SessionWrapper session) {
		boolean waitForOtherServiceResponse = false;

		final Game game = gameInstanceDao.getGameByInstanceId(gameInstanceId);
		final ResultConfiguration resultConfiguration = game.getResultConfiguration();
		if (resultConfiguration != null && resultConfiguration.isSpecialPrizeEnabled()){
			String voucherId = resultConfiguration.getSpecialPrizeVoucherId();
			if (voucherId != null) {
				voucherCommunicator.requestUserVoucherReserve(voucherId, sourceMessage, session);
				waitForOtherServiceResponse = true;
			} else {
				LOGGER.warn("Special prize turned on, but no voucher specified");
			}
		}

		return waitForOtherServiceResponse;
	}

	private void releaseUserVoucher(String gameInstanceId, JsonMessage<? extends JsonMessageContent> sourceMessage,
			SessionWrapper session) {
		final GameInstance gameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		final Game game = gameInstance.getGame();
		final ResultConfiguration resultConfiguration = game.getResultConfiguration();
		if (resultConfiguration != null && resultConfiguration.isSpecialPrizeEnabled()){
			String voucherId = resultConfiguration.getSpecialPrizeVoucherId();
			if (voucherId != null) {
				voucherCommunicator.requestUserVoucherRelease(voucherId, sourceMessage, session);
			} else {
				LOGGER.warn("Special prize turned on, but no voucher specified");
			}
		}
	}

	public GameInstance startGame(String gameInstanceId, String userId, String userLanguage){
		gameEngine.validateIfGameNotCancelled(gameInstanceId);
		final GameInstance gameInstance = gameEngine.startGame(userId, gameInstanceId,
				userLanguage);

		final Game game = gameInstance.getGame();
		if (game.getType().isLive()) {
			scheduleLiveTournamentQuestions(gameInstance, game);
		}

		return gameInstance;
	}

	public StartGameResponse prepareStartGameResponse(final GameInstance gameInstance, ClientInfo clientInfo) {
		final StartGameResponse response = new StartGameResponse(gameInstance.getId());
		response.setQuestionBlobKeys(gameInstance.getAllQuestionBlobKeys());
		response.setQuestionImageBlobKeys(gameInstance.getAllQuestionImageBlobKeys());
		//Game component ids
		final Game game = gameInstance.getGame();
		
		String[] winngingComponents;
		boolean userCountryCanGamble = noGamblingCountry.userComesFromNonGamblingCountry(clientInfo);
		if (userCountryCanGamble && ArrayUtils.isNotEmpty(game.getWinningComponents())) {
			winngingComponents = Arrays.stream(game.getWinningComponents()).map(GameWinningComponentListItem::getWinningComponentId)
					.distinct().toArray(String[]::new);
		} else {
			winngingComponents = new String[0];
		}
		response.setWinningComponentIds(winngingComponents);
		response.setAdvertisementDuration(game.getAdvertisementDuration());
		response.setAdvertisementSkipping(game.isAdvertisementSkipping());
		response.setLoadingScreenDuration(game.getLoadingScreenDuration());
		response.setVoucherIds(game.getVoucherIds());
		response.setAdvertisementBlobKeys(gameInstance.getAdvertisementBlobKeys());
		response.setLoadingBlobKey(gameInstance.getLoadingBlobKey());
		int skippingQuestionAmount=0;
		if (gameInstance.getJokerConfigurationIfAvailable(JokerType.SKIP)!=null)
			skippingQuestionAmount=gameInstance.getJokerConfigurationIfAvailable(JokerType.SKIP).getAvailableCount();
		response.setNumberOfQuestions(gameInstance.getQuestionIds().length - skippingQuestionAmount);

		if (gameInstance.isOfflineGame()) {
			response.setDecryptionKeys(gameInstance.getAllDecryptionKeys());
			final String gameId = game.getGameId();
			final String userId = gameInstance.getUserId();
			final String[] playedQuestionIds = gameInstance.getQuestionIds();
			gameHistoryManagerImpl.recordQuestionsPlayed(userId, gameId, playedQuestionIds);
		}
		return response;
	}

	public void scheduleLiveTournamentQuestions(final GameInstance gameInstance, final Game game) {
		final JsonObject questionsMap = gameInstance.getQuestionsMap();
		final Question[] questions = questionsMap.entrySet().stream()
			.sorted((e1, e2) -> Long.valueOf(e1.getKey()).compareTo(Long.valueOf(e2.getKey())))
			.map(e -> jsonUtil.fromJson(e.getValue(), Question.class))
			.toArray(Question[]::new);
		final ZonedDateTime playDateTime = commonMultiplayerGameInstanceDao.getConfig(gameInstance.getMgiId())
				.getPlayDateTime();
		questionFeeder.scheduleGameStart(game, gameInstance.getUserId(), gameInstance.getMgiId(), questions,
				playDateTime);
	}

	public void cancelGame(JsonMessage<CancelGameRequest> cancelGameRequestMessage, SessionWrapper sessionWrapper) {
		final String gameInstanceId = cancelGameRequestMessage.getContent().getGameInstanceId();
		final ClientInfo clientInfo = cancelGameRequestMessage.getClientInfo();
		final GameEndStatus endStatus = gameEngine.cancelGameByClient(clientInfo, gameInstanceId);

		final GameInstance gameInstance = gameInstanceDao.getGameInstance(gameInstanceId);
		if (endStatus == GameEndStatus.CALCULATING_RESULT) {
			resultEngineCommunicatorImpl.requestCalculateResults(clientInfo, gameInstance, sessionWrapper);
		} else if (StringUtils.isNotBlank(gameInstance.getMgiId())) {
			multiplayerGameManager.requestCalculateMultiplayerResultsIfPossible(clientInfo, gameInstance.getMgiId(),
					gameInstance.getGame(), sessionWrapper);
		}

		releaseUserVoucher(gameInstanceId, cancelGameRequestMessage, sessionWrapper);
	}
	
	public void closeCalculatedGameInstance(String gameInstanceId, ClientInfo clientInfo, SessionWrapper userSession) {
		final GameInstance gameInstance = gameEngine.closeUpGameIntanceWithSuccessfulResultCalculation(gameInstanceId);
		if (gameInstance.getGame().isMultiUserGame()) {
			multiplayerGameManager.requestCalculateMultiplayerResultsIfPossible(clientInfo, gameInstance.getMgiId(),
					gameInstance.getGame(), userSession);
		}
	}
	
	public void closeFailedGameInstance(JsonMessage<? extends JsonMessageContent> requestMessage, Throwable th){
		final boolean anyTerminateMessage = Stream
				.of(GameEngineMessageTypes.START_GAME,
						GameEngineMessageTypes.READY_TO_PLAY, GameEngineMessageTypes.ANSWER_QUESTION,
						GameEngineMessageTypes.NEXT_STEP, GameEngineMessageTypes.CANCEL_GAME)
				.anyMatch(t -> t == requestMessage.getType(GameEngineMessageTypes.class));
		if(anyTerminateMessage){
			LOGGER.debug("Closing up game instance with failed processing [{}] : {}", requestMessage,
					th != null ? th.getMessage() : null);
			final GameInstanceRequest gameInstanceRequest = (GameInstanceRequest) requestMessage.getContent();
			final String errorMessage = th != null ? Optional.ofNullable(th.getMessage())
					.orElse(th.getClass().getSimpleName()) : null;
			gameEngine.closeUpGameIntanceWithFailedProcessing(requestMessage.getClientInfo(),
					gameInstanceRequest.getGameInstanceId(), errorMessage);
		}
	}
}
