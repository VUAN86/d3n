package de.ascendro.f4m.service.winning.client;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.selection.model.game.GameWinningComponentListItem;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentDetailsBuilder;
import de.ascendro.f4m.service.payment.model.TransactionLog;
import de.ascendro.f4m.service.payment.model.TransferFundsRequest;
import de.ascendro.f4m.service.payment.model.TransferFundsRequestBuilder;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceRequest;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import de.ascendro.f4m.service.winning.dao.SuperPrizeAerospikeDao;
import de.ascendro.f4m.service.winning.model.SuperPrize;
import de.ascendro.f4m.service.winning.model.UserWinningComponent;
import de.ascendro.f4m.service.winning.model.WinningOption;
import de.ascendro.f4m.service.winning.model.WinningOptionType;

public class PaymentServiceCommunicator {

	private static final String BUYING_WINNING_COMPONENT_REASON = "Buying winning component";
	private static final String USER_WINNING_COMPONENT_WINNING_REASON = "User winning component won";

	private final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private final ServiceRegistryClient serviceRegistryClient;
	private final JsonMessageUtil jsonUtil;
	private final TransactionLogAerospikeDao transactionLogAerospikeDao;
	private final SuperPrizeAerospikeDao superPrizeDao;

	@Inject
	public PaymentServiceCommunicator(JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool,
			ServiceRegistryClient serviceRegistryClient, JsonMessageUtil jsonUtil, TransactionLogAerospikeDao transactionLogAerospikeDao,
			SuperPrizeAerospikeDao superPrizeDao) {
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.serviceRegistryClient = serviceRegistryClient;
		this.jsonUtil = jsonUtil;
		this.transactionLogAerospikeDao = transactionLogAerospikeDao;
		this.superPrizeDao = superPrizeDao;
	}

	public void requestWinningTransfer(JsonMessage<? extends JsonMessageContent> sourceMessage, SessionWrapper sourceSession, 
			String gameId, UserWinningComponent userWinningComponent) {
		SuperPrize superPrize = null;
		if (userWinningComponent.getWinning().getType() == WinningOptionType.SUPER) {
			superPrize = superPrizeDao.getSuperPrize(userWinningComponent.getWinning().getPrizeId());
		}
		final UserWinningComponentRequestInfo requestInfo = new UserWinningComponentRequestInfo(sourceMessage, sourceSession, userWinningComponent, superPrize);
		requestTransfer(requestInfo, null, sourceMessage.getClientInfo().getUserId(), getAmount(userWinningComponent.getWinning(), superPrize), 
				getCurrency(userWinningComponent.getWinning().getType()), gameId, userWinningComponent.getGameInstanceId(), userWinningComponent.getWinningComponentId(),
				userWinningComponent.getUserWinningComponentId(), USER_WINNING_COMPONENT_WINNING_REASON, sourceMessage.getClientInfo());
	}

	private BigDecimal getAmount(WinningOption winning, SuperPrize superPrize) {
		switch (winning.getType()) {
		case BONUS:
		case CREDITS:
		case MONEY:
			return winning.getAmount();
		case SUPER:
			return BigDecimal.valueOf(superPrize.getWinning());
		default:
			throw new F4MFatalErrorException("Invalid winning option type for winning transfer: " + winning.getType());
		}
	}
	
	private Currency getCurrency(WinningOptionType type) {
		switch(type) {
		case BONUS:
			return Currency.BONUS;
		case CREDITS:
			return Currency.CREDIT;
		case MONEY:
		case SUPER:
			return Currency.MONEY;
		default:
			throw new F4MFatalErrorException("Invalid winning option type: " + type);
		}
	}

	public void requestAssignWinningComponentPayment(JsonMessage<? extends JsonMessageContent> sourceMessage,
													 SessionWrapper sourceSession,
													 String gameId,
													 String gameInstanceId,
													 GameWinningComponentListItem winningComponentConfiguration,
													 boolean isEligibleToWinnings,
													 boolean isEligibleToComponent) {
		final PaymentRequestInfo requestInfo = new PaymentRequestInfo(sourceMessage, sourceSession, gameId);
		requestInfo.setWinningComponentId(winningComponentConfiguration.getWinningComponentId());
		requestInfo.setEligibleToWinnings(isEligibleToWinnings);
		requestInfo.setEligibleToComponent(isEligibleToComponent);
		requestTransfer(
				requestInfo,
				sourceMessage.getClientInfo().getUserId(),
				null, winningComponentConfiguration.getAmount(),
				winningComponentConfiguration.getCurrency(),
				gameId,
				gameInstanceId,
				winningComponentConfiguration.getWinningComponentId(),
				null,
				BUYING_WINNING_COMPONENT_REASON,
				sourceMessage.getClientInfo());
	}

	private void requestTransfer(PaymentRequestInfo requestInfo, String fromUserId, String toUserId, BigDecimal amount, Currency currency, 
			String gameId, String gameInstanceId, String winningComponentId, String userWinningComponentId, String reason, ClientInfo clientInfo) {
		final String tenantId = clientInfo.getTenantId();
		
		TransferFundsRequestBuilder builder = new TransferFundsRequestBuilder()
				.amount(amount.setScale(2, RoundingMode.DOWN), currency)
				.withPaymentDetails(new PaymentDetailsBuilder()
						.appId(clientInfo.getAppId())
						.gameId(gameId).gameInstanceId(gameInstanceId)
						.winningComponentId(winningComponentId).userWinningComponentId(userWinningComponentId)
						.additionalInfo(reason).build());
		if (fromUserId != null) {
			builder.fromProfileToTenant(fromUserId, tenantId);
		} else {
			builder.fromTenantToProfile(tenantId, toUserId);
		}
		final TransferFundsRequest transactionRequest = builder.buildSingleUserPaymentForGame();
		final JsonMessage<TransferFundsRequest> transactionRequestMessage = jsonUtil.createNewMessage(
				builder.getBuildSingleUserPaymentForGameRequestType(), transactionRequest);

		String transactionLogId = transactionLogAerospikeDao.createTransactionLog(new TransactionLog(transactionRequest, currency, reason, clientInfo.getAppId()));
		requestInfo.setTransactionLogId(transactionLogId);

		try {
			final ServiceConnectionInformation paymentServConnInfo = serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(paymentServConnInfo, transactionRequestMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to initiate [" + amount + " " + currency + "] transfer for winning component [" + winningComponentId + "], "
					+ "userWinningComponent [" + userWinningComponentId + "], game instance [" + gameInstanceId + "] from user [" + fromUserId + "] to user [" + toUserId + "], "
					+ "tenantId [" + tenantId + "]", e);
		}
	}

	public void requestTenantBalance(JsonMessage<? extends JsonMessageContent> sourceMessage, SessionWrapper session, WinningComponentMoneyCheckRequestInfo requestInfo) {
		GetAccountBalanceRequest getAccountBalanceRequest = new GetAccountBalanceRequest();

		final String tenantId = sourceMessage.getClientInfo().getTenantId();
		getAccountBalanceRequest.setTenantId(tenantId);
		getAccountBalanceRequest.setProfileId(sourceMessage.getClientInfo().getUserId());
		getAccountBalanceRequest.setCurrency(Currency.MONEY);

		final JsonMessage<GetAccountBalanceRequest> getAccountBalanceMessage = jsonUtil.createNewMessage(
				PaymentMessageTypes.GET_ACCOUNT_BALANCE, getAccountBalanceRequest);

		requestInfo.setSourceMessage(sourceMessage);
		requestInfo.setSourceSession(session);
		try {
			final ServiceConnectionInformation paymentServiceConnInfo = serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageNoClientInfo(paymentServiceConnInfo, getAccountBalanceMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to initiate MONEY balance inquiry for winning component " +
					"from tenant [" + tenantId + "] for user [" + sourceMessage.getClientInfo().getUserId() + "]", e);
		}
	}

}
