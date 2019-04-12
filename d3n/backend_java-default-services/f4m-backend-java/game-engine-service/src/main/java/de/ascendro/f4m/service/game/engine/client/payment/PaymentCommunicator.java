package de.ascendro.f4m.service.game.engine.client.payment;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.request.jackpot.GameIdCancelGameTournamentResponse;
import de.ascendro.f4m.server.request.jackpot.PaymentGetJackpotRequestInformation;
import de.ascendro.f4m.server.request.jackpot.PaymentServiceCommunicator;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.engine.client.payment.PaymentRequestInfo.Type;
import de.ascendro.f4m.service.game.engine.model.cancelTournament.CancelTournamentGame;
import de.ascendro.f4m.service.game.engine.model.joker.PurchaseJokerRequest;
import de.ascendro.f4m.service.game.engine.model.register.RegisterRequest;
import de.ascendro.f4m.service.game.selection.GameSelectionMessageTypes;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerUserGameInstance;
import de.ascendro.f4m.service.game.selection.model.multiplayer.message.ActivateInvitationsRequest;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.*;
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.LoadOrWithdrawWithoutCoverageRequest;
import de.ascendro.f4m.service.payment.model.internal.PaymentDetails;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PaymentCommunicator {
	private static final String GAME_ENTRY_FEE_REASON = "Game entry fee";
	private static final String PURCHASE_JOKER_REASON = "Purchase joker ";

	private final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private final JsonMessageUtil jsonMessageUtil;
	private final ServiceRegistryClient serviceRegistryClient;
	private final TransactionLogAerospikeDao transactionLogAerospikeDao;
	private final PaymentServiceCommunicator paymentServiceCommunicator;

	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentCommunicator.class);

	@Inject
	public PaymentCommunicator(
			JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, JsonMessageUtil jsonMessageUtil, ServiceRegistryClient serviceRegistryClient,
			TransactionLogAerospikeDao transactionLogAerospikeDao, PaymentServiceCommunicator paymentServiceCommunicator)
	{
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.jsonMessageUtil = jsonMessageUtil;
		this.serviceRegistryClient = serviceRegistryClient;
		this.transactionLogAerospikeDao = transactionLogAerospikeDao;
		this.paymentServiceCommunicator = paymentServiceCommunicator;
	}

	public void requestGameEntryFeeTransfer(String gameId, String mgiId, BigDecimal gameEntryFeeAmount, Currency gameEntryFeeCurrency,
			GameType gameType, SessionWrapper sourceSession, JsonMessage<RegisterRequest> sourceMessage){
		final String appId = sourceMessage.getClientInfo().getAppId();
		final String userId = sourceMessage.getClientInfo().getUserId();
		final String toTenantId = sourceMessage.getClientInfo().getTenantId();
		TransferFundsRequest messageContent;
		PaymentMessageTypes messageType;
		TransferFundsRequestBuilder requestBuilder = new TransferFundsRequestBuilder()
				.fromProfileToTenant(userId, toTenantId)
				.withMultiplayerGameInstanceId(mgiId)
				.amount(gameEntryFeeAmount)
				.withPaymentDetails(new PaymentDetailsBuilder().gameId(gameId).multiplayerGameInstanceId(mgiId).appId(appId).build());
		if (gameType.isMultiUser()) {
			messageContent = requestBuilder.buildJackpotBuyInRequest();
			messageType = requestBuilder.getBuildJackpotBuyInRequestType();
		} else {
			requestBuilder.currency(gameEntryFeeCurrency); //not used for multiplayer games
			messageContent = requestBuilder.buildSingleUserPaymentForGame();
			messageType = requestBuilder.getBuildSingleUserPaymentForGameRequestType();
		}

		final String transactionLogId = transactionLogAerospikeDao
				.createTransactionLog(new TransactionLog(messageContent, gameEntryFeeCurrency, GAME_ENTRY_FEE_REASON, appId));

		final JsonMessage<JsonMessageContent> transactionRequestMessage = jsonMessageUtil.createNewMessage(
				messageType, messageContent);

		final PaymentRequestInfo requestInfo = new PaymentRequestInfo(Type.ENTRY_FEE, gameId, mgiId, transactionLogId, gameEntryFeeAmount, gameEntryFeeCurrency, gameType);
		if (!gameType.isMultiUser()) {
			requestInfo.setSinglePlayerGameConfig(sourceMessage.getContent().getSinglePlayerGameConfig());
		}
		requestInfo.setSourceSession(sourceSession);
		requestInfo.setSourceMessage(sourceMessage);


		try {
			final ServiceConnectionInformation paymentServiceConnInfo = serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(paymentServiceConnInfo, transactionRequestMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send game [" + gameId + "] entry fee transfer from profile["
					+ userId + "] to tenant[" + toTenantId + "]", e);
		}
	}

	public void requestJokerPurchaseFeeTransfer(String gameId, BigDecimal jokerPurchaseFeeAmount, Currency jokerPurchaseFeeCurrency,
			GameType gameType, SessionWrapper sourceSession, JsonMessage<? extends PurchaseJokerRequest> sourceMessage) {
		final String appId = sourceMessage.getClientInfo().getAppId();
		final String userId = sourceMessage.getClientInfo().getUserId();
		final String toTenantId = sourceMessage.getClientInfo().getTenantId();
		TransferFundsRequestBuilder requestBuilder = new TransferFundsRequestBuilder()
				.fromProfileToTenant(userId, toTenantId)
				.amount(jokerPurchaseFeeAmount, jokerPurchaseFeeCurrency)
				.withPaymentDetails(new PaymentDetailsBuilder()
						.gameId(gameId)
						.gameInstanceId(sourceMessage.getContent().getGameInstanceId())
						.appId(appId)
						.additionalInfo(PURCHASE_JOKER_REASON + sourceMessage.getContent().getType().getValue())
						.build());
		TransferFundsRequest messageContent = requestBuilder.buildSingleUserPaymentForGame();
		PaymentMessageTypes messageType = requestBuilder.getBuildSingleUserPaymentForGameRequestType();

		final String transactionLogId = transactionLogAerospikeDao
				.createTransactionLog(new TransactionLog(messageContent, jokerPurchaseFeeCurrency, PURCHASE_JOKER_REASON, appId));

		final JsonMessage<JsonMessageContent> transactionRequestMessage = jsonMessageUtil.createNewMessage(
				messageType, messageContent);

		final PaymentRequestInfo requestInfo = new PaymentRequestInfo(Type.JOKER_PURCHASE, gameId, null, transactionLogId, jokerPurchaseFeeAmount, jokerPurchaseFeeCurrency, gameType);
		requestInfo.setSourceSession(sourceSession);
		requestInfo.setSourceMessage(sourceMessage);

		try {
			final ServiceConnectionInformation paymentServiceConnInfo = serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(paymentServiceConnInfo, transactionRequestMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send game [" + gameId + "] joker purchase transfer from profile["
					+ userId + "] to tenant [" + toTenantId + "]", e);
		}
	}

	public void requestGameEntryFeeTransferBehindTarget(JsonMessage<RegisterRequest> sourceMessage, final String mgiId, final CustomGameConfig customGameConfig) {
		final String appId = sourceMessage.getClientInfo().getAppId();
		final String userId = sourceMessage.getClientInfo().getUserId();
		final String toTenantId = sourceMessage.getClientInfo().getTenantId();
		BigDecimal amount = customGameConfig.getEntryFeeAmount();
		Currency currency = customGameConfig.getEntryFeeCurrency();
		TransferFundsRequestBuilder builder = new TransferFundsRequestBuilder().fromProfileToTenant(userId, toTenantId).amount(amount, currency).withPaymentDetails(new PaymentDetailsBuilder().appId(appId).multiplayerGameInstanceId(mgiId).build());
		TransferFundsRequest transactionRequest = builder.buildSingleUserPaymentForGame();
		final JsonMessage<TransferFundsRequest> transactionRequestMessage = jsonMessageUtil.createNewMessage(builder.getBuildSingleUserPaymentForGameRequestType(), transactionRequest);
		final EntranceFeeRequestInfo requestInfo = new EntranceFeeRequestInfo();

		String transactionLogId = transactionLogAerospikeDao.createTransactionLog(new TransactionLog(transactionRequest, currency, "", appId));
		requestInfo.setTransactionLogId(transactionLogId);

		requestInfo.setCurrency(currency);
		try {
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME), transactionRequestMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send transferTransaction request to payment service", e);
		}
	}


	// getting the balance of the game
	public void sendGetJackpotRequest(String mgiId, CustomGameConfig config, SessionWrapper sessionWrapper, JsonMessage<?> sourceMessage) {
		GameIdCancelGameTournamentResponse gameCancel = new GameIdCancelGameTournamentResponse(config.getGameId());

		BigDecimal entryFeeAmount = config.getEntryFeeAmount();
		Currency currency = config.getEntryFeeCurrency();
		String gameId = config.getGameId();

		PaymentGetJackpotRequestInformation paymentGetJackpotRequestInfo =
				new PaymentGetJackpotRequestInformation(gameId, mgiId, entryFeeAmount, currency, sourceMessage, sessionWrapper, gameCancel);

		paymentServiceCommunicator.sendGetJackpotRequest(mgiId, config.getTenantId(), paymentGetJackpotRequestInfo);
	}


	public void requestCancelTournamentGame(
			List<MultiplayerUserGameInstance> multiplayerUserGameInstances,
			CancelTournamentGame cancel,
			String mgiId,
			CustomGameConfig config,
			RequestInfo info,
			boolean isEntryFeeMoney)
	{
		final CloseJackpotRequest request = new CloseJackpotRequest();
		request.setPaymentDetails(new PaymentDetailsBuilder()
				.gameId(cancel.getGameId())
				.gameInstanceId(null) //multiple values userResult.getGameInstanceId(), so don't use any
				.appId(config.getAppId())
				.multiplayerGameInstanceId(mgiId)
				.additionalInfo("Cancellation of the tournament.")
				.build());
		request.setResponseToForward(new GameIdCancelGameTournamentResponse(cancel.getGameId()));
		request.setMultiplayerGameInstanceId(mgiId);
		request.setTenantId(config.getTenantId());
		if (isEntryFeeMoney) {
			request.setPayouts(addPayout(multiplayerUserGameInstances, config.getEntryFeeAmount()));
		} else {
			request.setPayouts(Collections.emptyList());
			multiplayerUserGameInstances.forEach(gameInstance -> winSurcharge(
					config.getAppId(),
					config.getTenantId(),
					gameInstance.getUserId(),
					config.getEntryFeeAmount(),
					config.getEntryFeeCurrency(),
					gameInstance.getGameInstanceId()));
		}

		List<String> transactionIds = request
				.getPayouts()
				.stream()
				.map(item -> transactionLogAerospikeDao.createTransactionLog(
						new TransactionLog(
								null,
								config.getTenantId(),
								item.getProfileId(),
								config.getGameId(),
								mgiId, null,
								item.getAmount(),
								config.getEntryFeeCurrency(),
								request.getPaymentDetails().getAdditionalInfo(),
								config.getAppId())))
				.collect(Collectors.toList());

		CancelGameTournamentRequestInfo cancelGameTournamentRequestInfo = new CancelGameTournamentRequestInfo(null, transactionIds, info);
		cancelGameTournamentRequestInfo.setResponseToForward(new GameIdCancelGameTournamentResponse(config.getGameId()));
		try {
			final ServiceConnectionInformation paymentServiceConnInfo = serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(
					paymentServiceConnInfo,
					jsonMessageUtil.createNewMessage(PaymentMessageTypes.CLOSE_JACKPOT, request),
					cancelGameTournamentRequestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to initiate cancel for tournament mgiId=" + mgiId);
		}
	}

	private List<CloseJackpotRequest.PayoutItem> addPayout(List<MultiplayerUserGameInstance> gameInstances, BigDecimal entryFeeAmount) {
		return gameInstances
				.stream()
				.map(multiplayerUserGameInstance -> new CloseJackpotRequest.PayoutItem(multiplayerUserGameInstance.getUserId(), entryFeeAmount))
				.collect(Collectors.toList());
	}

	private void winSurcharge(
			String appId,
			String tenantId,
			String profileId,
			BigDecimal amount,
			Currency currency,
			String gameInstanceId
	) {
		LoadOrWithdrawWithoutCoverageRequest request = new LoadOrWithdrawWithoutCoverageRequest();
		request.setTenantId(tenantId);
		request.setProfileId(profileId);
		request.setCurrency(currency);
		request.setAmount(amount);
		PaymentDetails details = new PaymentDetails();
		request.setPaymentDetails(details);
		WinSurchargeRequestInfo winRequestInfo = new WinSurchargeRequestInfo(gameInstanceId, currency);
		final JsonMessage<TransferFundsRequest> transactionRequestMessage = jsonMessageUtil.createNewMessage(
				PaymentMessageTypes.LOAD_OR_WITHDRAW_WITHOUT_COVERAGE, request);
		String transactionLogId = transactionLogAerospikeDao.createTransactionLog(new TransactionLog(request, currency, "Supplement win the tournament", appId));
		winRequestInfo.setTransactionLogId(transactionLogId);
		try {
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME),
					transactionRequestMessage, winRequestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send transferTransaction request to payment service", e);
		}
	}



	public void requestActivateInvitations(String mgiId, ClientInfo sourceClientInfo) {
		ActivateInvitationsRequest content = new ActivateInvitationsRequest(mgiId);
		JsonMessage<ActivateInvitationsRequest> message = jsonMessageUtil.createNewMessage(GameSelectionMessageTypes.ACTIVATE_INVITATIONS, content);
		message.setClientInfo(sourceClientInfo);
		sendRequest(message);
	}

	private void sendRequest(JsonMessage<? extends JsonMessageContent> message) {
		try {
			ServiceConnectionInformation gameSelectionConnInfo = serviceRegistryClient.getServiceConnectionInformation(GameSelectionMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessage(gameSelectionConnInfo, message);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException(String.format("Unable to send [%s] request to GameSelection service", message.getTypeName()), e);
		}
	}



	private class EntranceFeeRequestInfo extends RequestInfoImpl {
		private Currency currency;
		private String transactionLogId;

		EntranceFeeRequestInfo() {
		}

		public String getTransactionLogId() {
			return transactionLogId;
		}

		public void setTransactionLogId(String transactionLogId) {
			this.transactionLogId = transactionLogId;
		}

		public Currency getCurrency() {
			return currency;
		}

		public void setCurrency(Currency currency) {
			this.currency = currency;
		}
	}

}
