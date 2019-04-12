package de.ascendro.f4m.service.game.engine.client;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.analytics.model.InvoiceEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.NotifySubscriberMessageContent;
import de.ascendro.f4m.service.event.model.subscribe.SubscribeResponse;
import de.ascendro.f4m.service.event.model.unsubscribe.UnsubscribeRequestResponse;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypes;
import de.ascendro.f4m.service.game.engine.client.payment.PaymentRequestInfo;
import de.ascendro.f4m.service.game.engine.client.results.ResultEngineRequestInfo;
import de.ascendro.f4m.service.game.engine.client.selection.GameSelectionCommunicator;
import de.ascendro.f4m.service.game.engine.client.winning.WinningCommunicator;
import de.ascendro.f4m.service.game.engine.client.winning.WinningRequestInfo;
import de.ascendro.f4m.service.game.engine.joker.JokerRequestHandler;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.end.EndGameRequest;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseJokerRequest;
import de.ascendro.f4m.service.game.engine.model.register.RegisterResponse;
import de.ascendro.f4m.service.game.engine.model.start.game.StartGameRequest;
import de.ascendro.f4m.service.game.engine.model.start.game.StartGameResponse;
import de.ascendro.f4m.service.game.engine.multiplayer.MultiplayerGameManager;
import de.ascendro.f4m.service.game.engine.server.GameEngine;
import de.ascendro.f4m.service.game.engine.server.MessageCoordinator;
import de.ascendro.f4m.service.game.engine.server.subscription.EventSubscriptionManager;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.TransactionStatus;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.result.engine.ResultEngineMessageTypes;
import de.ascendro.f4m.service.result.engine.model.calculate.CalculateResultsResponse;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.ServiceUtil;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;
import de.ascendro.f4m.service.winning.WinningMessageTypes;
import de.ascendro.f4m.service.winning.model.WinningComponentType;

public class GameEngineServiceClientMessageHandler extends DefaultJsonMessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GameEngineServiceClientMessageHandler.class);
	
	private final GameEngine gameEngine;
	private final JokerRequestHandler jokerRequestHandler;
	private final TransactionLogAerospikeDao transactionLogAerospikeDao;
	private final WinningCommunicator winningCommunicator;
	private final EventSubscriptionManager eventSubscriptionManager;
	private final MessageCoordinator messageCoordinator;
	private final Tracker tracker;
	private final GameSelectionCommunicator gameSelectionCommunicator;
	private final MultiplayerGameManager multiplayerGameManager;
	private final ServiceUtil serviceUtil;

	public GameEngineServiceClientMessageHandler(GameEngine gameEngine,
			JokerRequestHandler jokerRequestHandler,
			TransactionLogAerospikeDao transactionLogAerospikeDao, WinningCommunicator winningCommunicator,
			EventSubscriptionManager eventSubscriptionManager, MessageCoordinator messageCoordinator, Tracker tracker,
			GameSelectionCommunicator gameSelectionCommunicator, MultiplayerGameManager multiplayerGameManager,
			ServiceUtil serviceUtil) {
		this.gameEngine = gameEngine;
		this.jokerRequestHandler = jokerRequestHandler;
		this.transactionLogAerospikeDao = transactionLogAerospikeDao;
		this.winningCommunicator = winningCommunicator;
		this.eventSubscriptionManager = eventSubscriptionManager;
		this.messageCoordinator = messageCoordinator;
		this.tracker = tracker;
		this.gameSelectionCommunicator = gameSelectionCommunicator;
		this.multiplayerGameManager = multiplayerGameManager;
		this.serviceUtil = serviceUtil;
	}

	@Override
	public JsonMessageContent onUserMessage(RequestContext context) {
		JsonMessage<? extends JsonMessageContent> message = context.getMessage();
		final ResultEngineMessageTypes resultEngineMessageType;
		final PaymentMessageTypes paymentMessageTypes;
		final WinningMessageTypes winningMessageTypes;
		final EventMessageTypes eventMessageTypes;
		final VoucherMessageTypes voucherMessageTypes;

		JsonMessageContent responseContent = null;
		if ((resultEngineMessageType = message.getType(ResultEngineMessageTypes.class)) != null) {
			onResultEngineMessage(resultEngineMessageType, context);
		} else if ((paymentMessageTypes = message.getType(PaymentMessageTypes.class)) != null) {
			onPaymentServiceMessage(paymentMessageTypes, context);
		} else if ((winningMessageTypes = message.getType(WinningMessageTypes.class)) != null) {
			onWinningServiceMessage(winningMessageTypes, context);
		} else if ((eventMessageTypes = message.getType(EventMessageTypes.class)) != null) {
			onEventServiceMessage(eventMessageTypes, context);
		} else if ((voucherMessageTypes = message.getType(VoucherMessageTypes.class)) != null) {
			onVoucherServiceMessage(voucherMessageTypes, context);
		} else {
			throw new F4MValidationFailedException("Unrecognized message " + message.getName());
		}
		return responseContent;
	}

	private void onVoucherServiceMessage(VoucherMessageTypes voucherMessageType, RequestContext context) {
		switch (voucherMessageType) {
		case USER_VOUCHER_RESERVE_RESPONSE:
			onUserVoucherReserveResponse(context.getOriginalRequestInfo());
			break;
		case USER_VOUCHER_RELEASE_RESPONSE:
			// Nothing to do
			break;
		default:
			throw new F4MValidationFailedException("Unsupported message type[" + voucherMessageType + "]");
		}
	}

	private void onUserVoucherReserveResponse(RequestInfo requestInfo) {
		GameEngineMessageTypes sourceMessageType = requestInfo.getSourceMessage().getType(GameEngineMessageTypes.class);
		if (sourceMessageType.equals(GameEngineMessageTypes.START_GAME)) {
			startGame(requestInfo);
		} else {
			throw new F4MValidationFailedException("Unsupported source message type received on userVoucherReserve " +
					"response[" + sourceMessageType + "]");
		}
	}

	private void startGame(RequestInfo requestInfo) {
		final StartGameRequest startGameRequest = (StartGameRequest) requestInfo.getSourceMessage().getContent();
		ClientInfo clientInfo = requestInfo.getSourceMessage().getClientInfo();

		final GameInstance gameInstance = messageCoordinator.startGame(startGameRequest.getGameInstanceId(),
				clientInfo.getUserId(), startGameRequest.getUserLanguage());

		StartGameResponse response = messageCoordinator.prepareStartGameResponse(gameInstance,clientInfo);

		sendResponse(requestInfo.getSourceMessage(), response, requestInfo.getSourceSession());
	}

	@Override
	public void onUserErrorMessage(RequestContext context) {
		final PaymentMessageTypes paymentMessageTypes;
		final ResultEngineMessageTypes resultEngineMessageTypes;
		JsonMessage<?> message = context.getMessage();
		if ((paymentMessageTypes = message.getType(PaymentMessageTypes.class)) != null) {
			onPaymentServiceMessage(paymentMessageTypes, context);
		}else if ((resultEngineMessageTypes = message.getType(ResultEngineMessageTypes.class)) != null) {
			onResultEngineMessage(resultEngineMessageTypes, context);
		}
	}

	private void onPaymentServiceMessage(PaymentMessageTypes paymentMessageType, RequestContext context) {
		if (PaymentMessageTypes.isTransferFundsResponseWithTransactionId(paymentMessageType)) {
			JsonMessage<TransactionId> message = context.getMessage();
			if (message.getError() == null) {
				onTransferBetweenAccountsResponse(message, context.getOriginalRequestInfo());
			} else {
				onTransferBetweenAccountsResponseWithError(context.getOriginalRequestInfo());
			}
		} else {
			throw new F4MValidationFailedException("Unsupported message type[" + paymentMessageType + "]");
		}
	}

	@SuppressWarnings("unchecked")
	private void onTransferBetweenAccountsResponse(JsonMessage<TransactionId> message, PaymentRequestInfo requestInfo) {
		final String userId = requestInfo.getSourceMessage().getUserId();
		Validate.notNull(requestInfo, "Received response of account transfer with no request stored");
		Validate.notNull(userId, "User id is required for game registration");
		Validate.notNull(requestInfo.getGameId(), "Game id is required for game registration");

		final TransactionId transactionId = message.getContent();
		Validate.notNull(transactionId, "No TransactionId on account transfer response");
		Validate.notNull(transactionId.getTransactionId(), "No transaction id on account transfer response");
		transactionLogAerospikeDao.updateTransactionLog(requestInfo.getTransactionLogId(), transactionId.getTransactionId(), TransactionStatus.COMPLETED);
		
        final ClientInfo clientInfo = requestInfo.getSourceMessage().getClientInfo();
		
		final String gameInstanceId;
		JsonMessageContent responseMessageContent;
		final JsonMessage<? extends JsonMessageContent> originalMessage = requestInfo.getSourceMessage();
		if(requestInfo.getGameType().isMultiUser()){
			gameInstanceId = gameEngine.registerForMultiplayerGame(clientInfo, requestInfo.getMgiId(),
					transactionId.getTransactionId(), requestInfo.getEntryFeeAmount(), requestInfo.getEntryFeeCurrency());
			responseMessageContent = new RegisterResponse(gameInstanceId);
		} else {
			if (originalMessage.getContent() instanceof PurchaseJokerRequest) {
				responseMessageContent = jokerRequestHandler.handlePurchaseJokerRequest(
						(JsonMessage<? extends PurchaseJokerRequest>) originalMessage, 
						requestInfo.getSourceSession(), serviceUtil.getMessageTimestamp());
			} else {
				gameInstanceId = gameEngine.registerForPaidQuiz24(clientInfo, requestInfo.getGameId(), 
					transactionId.getTransactionId(), requestInfo.getEntryFeeAmount(), requestInfo.getEntryFeeCurrency(),
					requestInfo.getSinglePlayerGameConfig());
				responseMessageContent = new RegisterResponse(gameInstanceId);
			}
		}

		InvoiceEvent.PaymentType paymentType = InvoiceEvent.PaymentType.ENTRY_FEE;
		if (originalMessage.getContent() instanceof PurchaseJokerRequest) {
			paymentType = InvoiceEvent.PaymentType.JOKER;
		}
		addInvoiceEvent(requestInfo.getSourceMessage().getClientInfo(), requestInfo.getEntryFeeAmount(),
				requestInfo.getEntryFeeCurrency(), requestInfo.getGameType(), paymentType);

		sendResponse(requestInfo.getSourceMessage(), responseMessageContent, requestInfo.getSourceSession());
	}

	private void addInvoiceEvent(ClientInfo clientInfo, BigDecimal amount, Currency currency, GameType gameType,
			InvoiceEvent.PaymentType paymentType) {
		InvoiceEvent invoiceEvent = new InvoiceEvent();
		invoiceEvent.setPaymentType(paymentType);
		invoiceEvent.setPaymentAmount(amount);
		invoiceEvent.setCurrency(currency);
		invoiceEvent.setGameType(gameType);
		tracker.addEvent(clientInfo, invoiceEvent);
	}
	
	private void onTransferBetweenAccountsResponseWithError(PaymentRequestInfo requestInfo) {
		Validate.notNull(requestInfo, "Received response of account transfer with no request stored");
		
		transactionLogAerospikeDao.updateTransactionLog(requestInfo.getTransactionLogId(), null, TransactionStatus.ERROR);
	}

	private void onResultEngineMessage(ResultEngineMessageTypes resultEngineMessageType, RequestContext context) {
		switch (resultEngineMessageType) {
		case CALCULATE_RESULTS_RESPONSE:
			JsonMessage<CalculateResultsResponse> message = context.getMessage();
			if(message.getError() == null){
				onCalculateResultsResponse(message, context.getOriginalRequestInfo());
			}else{
				onCalculateResultsResponseWithError(context.getOriginalRequestInfo());
			}
			break;
		case CALCULATE_MULTIPLAYER_RESULTS_RESPONSE:
			onCalculateMultiplayerResultsResponse(context.getOriginalRequestInfo());
			break;
		default:
			throw new F4MValidationFailedException("Unsupported message type[" + resultEngineMessageType + "]");
		}
	}

	private void onCalculateResultsResponse(JsonMessage<CalculateResultsResponse> message, ResultEngineRequestInfo requestInfo) {
		//Update game end status
		LOGGER.debug("Received response for calculateResults request with {}", requestInfo);
		final ClientInfo sourceClientInfo = requestInfo.getSourceMessage().getClientInfo();
		final SessionWrapper sourceSession = (SessionWrapper) requestInfo.getSourceSession();
		messageCoordinator.closeCalculatedGameInstance(requestInfo.getGameInstanceId(), sourceClientInfo, sourceSession);

		final CalculateResultsResponse response = message.getContent();
		if (requestInfo.getSourceMessage() != null
				&& StringUtils.isNotEmpty(requestInfo.getSourceMessage().getClientId()) && sourceSession != null) {
			// in case the client is disconnected during the game --> session is null
			forwardMultiplayerResults(sourceSession,
					requestInfo.getSourceMessage().getClientInfo().getClientId(), requestInfo.getGameInstanceId(),
					response.getFreeWinningComponentId(), response.getPaidWinningComponentId());
		}
		
		if (!requestInfo.getGameType().isMultiUser()) {
			gameSelectionCommunicator.requestUpdatePlayedGame(sourceClientInfo, requestInfo.getGameId(),
					requestInfo.getGameType(), requestInfo.getGameTitle());
		} else if (isUserMultiplayerGameCreator(sourceClientInfo.getUserId(), requestInfo.getMgiId())) {
			//gameSelectionCommunicator.requestActivateInvitations(requestInfo.getMgiId(), sourceClientInfo);
		}
	}
	
	private boolean isUserMultiplayerGameCreator(String userId, String mgiId) {
		CustomGameConfig multiplayerGameConfig = multiplayerGameManager.getMultiplayerGameConfig(mgiId);
		GameType gameType = multiplayerGameConfig.getGameType();
		return gameType.isOneOf(GameType.DUEL, GameType.USER_TOURNAMENT, GameType.USER_LIVE_TOURNAMENT)
				&& StringUtils.equals(userId, multiplayerGameConfig.getGameCreatorId());
	}
	
	private void onCalculateResultsResponseWithError(ResultEngineRequestInfo requestInfo) {
		final String gameInstanceId = requestInfo.getGameInstanceId();
		final String userId = requestInfo.getSourceMessage().getClientInfo().getUserId();
		gameEngine.closeUpGameIntanceWithFailedResultCalculation(userId, gameInstanceId);
	}
	
	private void onCalculateMultiplayerResultsResponse(ResultEngineRequestInfo requestInfo) {
		final String tenantId = requestInfo.getSourceMessage().getTenantId();
		gameSelectionCommunicator.requestUpdatePlayedGame(tenantId, requestInfo.getMgiId());
	}
	
	private void forwardMultiplayerResults(SessionWrapper sourceSession, String clientId, String gameInstanceId, 
			String freeWinningComponentId, String paidWinningComponentId) {
		LOGGER.debug("Sending endGame to {} for clientId {} and gameInstanceId {}", sourceSession, clientId, gameInstanceId);
		final JsonMessageContent endGameRequest = new EndGameRequest(gameInstanceId, GameEndStatus.CALCULATED_RESULT, 
				paidWinningComponentId, freeWinningComponentId);
		sendExtraResponse(sourceSession, clientId, GameEngineMessageTypes.END_GAME, endGameRequest);
	}

	private void sendExtraResponse(SessionWrapper sourceSession, String clientId, GameEngineMessageTypes type,
			JsonMessageContent responseContent) {
		final JsonMessage<JsonMessageContent> extraResponseMessage = jsonMessageUtil.createNewMessage(type, responseContent);
		extraResponseMessage.setClientId(clientId);
		sourceSession.sendAsynMessage(extraResponseMessage);
	}

	private void onWinningServiceMessage(WinningMessageTypes winningMessageType, RequestContext context) {
		switch (winningMessageType) {
		case USER_WINNING_COMPONENT_ASSIGN_RESPONSE:
			onUserWinningComponentAssignResponse(context.getOriginalRequestInfo());
			break;
		default:
			throw new F4MValidationFailedException("Unsupported message type[" + winningMessageType + "]");
		}
	}

	private void onUserWinningComponentAssignResponse(WinningRequestInfo requestInfo) {
		LOGGER.debug("Received response for winningComponentAssign request with {}", requestInfo);
		if (requestInfo.getSourceSession() != null) {
			// in case the client is disconnected during the game --> session is null
			forwardMultiplayerResults((SessionWrapper) requestInfo.getSourceSession(),
					requestInfo.getSourceMessage().getClientInfo().getClientId(), requestInfo.getGameInstanceId(),
					requestInfo.getFreeWinningComponentId(), requestInfo.getPaidWinningComponentId());
		}
	}
	
	private void onEventServiceMessage(EventMessageTypes eventMessageType, RequestContext context) {
		switch (eventMessageType) {
			case SUBSCRIBE_RESPONSE:			
				onSubscribeResponse(context.getMessage());
				break;
			case NOTIFY_SUBSCRIBER:
				onNotifySubscriber(context.getMessage());
				break;
			case UNSUBSCRIBE:
				onUnsubscribe(context.getMessage());
				break;
			default:
				throw new F4MValidationFailedException("Unsupported message type[" + eventMessageType + "]");
		}
	}	

	private void onUnsubscribe(JsonMessage<UnsubscribeRequestResponse> message) {
		LOGGER.debug("Received unsubscribe request for {}", message.getContent().getSubscription());
	}

	private void onSubscribeResponse(JsonMessage<SubscribeResponse> message) {
		final SubscribeResponse subscribeResponse = message.getContent();
		eventSubscriptionManager.updateSubscriptionIdForTopic(subscribeResponse.isVirtual(), 
				subscribeResponse.getConsumerName(), subscribeResponse.getTopic(), subscribeResponse.getSubscription());		
	}

	private void onNotifySubscriber(JsonMessage<NotifySubscriberMessageContent> message) {
		final String topic = message.getContent().getTopic();
		if (Game.isLiveTournamentStartStepTopic(topic)) {
			eventSubscriptionManager.notifyStep(topic, false);
		} else if (Game.isLiveTournamentEndGameTopic(topic)) {
			eventSubscriptionManager.notifyStep(topic, true);
		} else {
			LOGGER.warn("Received unexpected event notification with topic [{}]", topic);
		}
	}
}
