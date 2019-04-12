package de.ascendro.f4m.service.result.engine.client;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import de.ascendro.f4m.service.payment.model.internal.LoadOrWithdrawWithoutCoverageRequest;
import de.ascendro.f4m.service.payment.model.internal.PaymentDetails;
import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.result.RefundReason;
import de.ascendro.f4m.server.result.Results;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.selection.model.game.Game;
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
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotRequest.PayoutItem;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceRequest;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.update.UpdateProfileRequest;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.result.engine.model.notification.GameEndNotification;
import de.ascendro.f4m.service.result.engine.util.UserResultsByHandicapRange;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageRequest;
import de.ascendro.f4m.service.usermessage.translation.Messages;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherAssignRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherReleaseRequest;

public class ServiceCommunicatorImpl implements ServiceCommunicator {

	private static final String JACKPOT_WINNING_REASON = "Jackpot winning";
	private static final String USER_BONUS_REASON = "User bonus";
	private static final String REFUND_REASON_PREFIX = "Refund: ";
	
	private final JsonMessageUtil jsonMessageUtil;
	private final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private final ServiceRegistryClient serviceRegistryClient;
	private final TransactionLogAerospikeDao transactionLogDao;
	private final CommonGameInstanceAerospikeDao commonGameInstanceAerospikeDao;

	@Inject
	public ServiceCommunicatorImpl(JsonMessageUtil jsonMessageUtil,
			JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, ServiceRegistryClient serviceRegistryClient,
			TransactionLogAerospikeDao transactionLogDao,
			CommonGameInstanceAerospikeDao commonGameInstanceAerospikeDao) {
		this.jsonMessageUtil = jsonMessageUtil;
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.serviceRegistryClient = serviceRegistryClient;
		this.transactionLogDao = transactionLogDao;
		this.commonGameInstanceAerospikeDao = commonGameInstanceAerospikeDao;
	}

	@Override
	public void requestUpdateProfileHandicap(Results results, JsonMessage<? extends JsonMessageContent> sourceMessage, SessionWrapper session) {
		final UpdateProfileRequest request = new UpdateProfileRequest(results.getUserId());
		final Profile profile = new Profile();
		profile.setHandicap(results.getNewUserHandicap());
		request.setProfile(profile.getJsonObject());
		request.setService("resultEngineService");

		final JsonMessage<UpdateProfileRequest> message = jsonMessageUtil.createNewMessage(ProfileMessageTypes.UPDATE_PROFILE, request);
		final RequestInfo requestInfo = new ServiceRequestInfo(results.getGameInstanceId());
		requestInfo.setSourceMessage(sourceMessage);
		requestInfo.setSourceSession(session);
		try {
			final ServiceConnectionInformation connInfo = serviceRegistryClient.getServiceConnectionInformation(ProfileMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(connInfo, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send update profile request to profile service", e);
		}
	}

    @Override
    public void pushMessageToUser(String userId, GameEndNotification gameEndNotification, ClientInfo clientInfo) {
        SendWebsocketMessageRequest requestContent = new SendWebsocketMessageRequest(true);
        requestContent.setUserId(userId);
        requestContent.setMessage(Messages.GAME_ENDED_PUSH);
        requestContent.setType(gameEndNotification.getType());
        requestContent.setPayload(jsonMessageUtil.toJsonElement(gameEndNotification));
        Game gameInstance = commonGameInstanceAerospikeDao.getGameByInstanceId(gameEndNotification.getGameInstanceId());
        String gameTitle = "";
        if (gameInstance != null) {
            gameTitle = gameInstance.getTitle();
        }
        requestContent.setParameters(new String[]{gameTitle});

        final JsonMessage<SendWebsocketMessageRequest> message = jsonMessageUtil
                .createNewMessage(UserMessageMessageTypes.SEND_WEBSOCKET_MESSAGE, requestContent);
        message.setClientInfo(clientInfo);

        try {
            ServiceConnectionInformation userMessageConnInfo = serviceRegistryClient.getServiceConnectionInformation(UserMessageMessageTypes.SERVICE_NAME);
            jsonWebSocketClientSessionPool.sendAsyncMessage(userMessageConnInfo, message);
        } catch (F4MValidationFailedException | F4MIOException e) {
            throw new F4MFatalErrorException("Unable to send SendMobilePushRequest to User Message Service", e);
        }
    }

	@Override
	public void requestUserVoucherAssign(Results results, JsonMessage<? extends JsonMessageContent> sourceMessage, SessionWrapper session) {
		final UserVoucherAssignRequest request = new UserVoucherAssignRequest(results.getSpecialPrizeVoucherId(), 
				results.getUserId(),results.getGameInstanceId(), results.getTenantId(), results.getAppId(), "superPrize");
		
		final JsonMessage<UserVoucherAssignRequest> message = jsonMessageUtil.createNewMessage(VoucherMessageTypes.USER_VOUCHER_ASSIGN, request);
		final RequestInfo requestInfo = new ServiceRequestInfo(results.getGameInstanceId());
		requestInfo.setSourceMessage(sourceMessage);
		requestInfo.setSourceSession(session);
		try {
			final ServiceConnectionInformation connInfo = serviceRegistryClient.getServiceConnectionInformation(VoucherMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(connInfo, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send add voucher to profile request to voucher service", e);
		}
	}

	@Override
	public void requestUserVoucherRelease(GameInstance gameInstance, JsonMessage<? extends JsonMessageContent> sourceMessage, SessionWrapper session) {
		final UserVoucherReleaseRequest request = new UserVoucherReleaseRequest(gameInstance.getGame()
													.getResultConfiguration().getSpecialPrizeVoucherId());

		final JsonMessage<UserVoucherReleaseRequest> message = jsonMessageUtil.createNewMessage(VoucherMessageTypes.USER_VOUCHER_RELEASE, request);
		final RequestInfo requestInfo = new ServiceRequestInfo(gameInstance.getId());
		requestInfo.setSourceMessage(sourceMessage);
		requestInfo.setSourceSession(session);
		try {
			final ServiceConnectionInformation connInfo = serviceRegistryClient.getServiceConnectionInformation(VoucherMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(connInfo, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send release voucher request to voucher service", e);
		}
	}

	@Override
	public void requestTotalBonusPoints(Results results, JsonMessage<? extends JsonMessageContent> sourceMessage, SessionWrapper session) {
		GetAccountBalanceRequest getAccountBalanceRequest = new GetAccountBalanceRequest();
		
		final String tenantId = sourceMessage.getClientInfo().getTenantId();
		getAccountBalanceRequest.setTenantId(tenantId);
		getAccountBalanceRequest.setProfileId(results.getUserId());
		getAccountBalanceRequest.setCurrency(Currency.BONUS);
		
		final JsonMessage<GetAccountBalanceRequest> getAccountBalanceMessage = jsonMessageUtil.createNewMessage(
				PaymentMessageTypes.GET_ACCOUNT_BALANCE, getAccountBalanceRequest);

		final ServiceRequestInfo requestInfo = new ServiceRequestInfo(results.getGameInstanceId());
		requestInfo.setSourceMessage(sourceMessage);
		requestInfo.setSourceSession(session);
		try {
			final ServiceConnectionInformation paymentServiceConnInfo = serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageNoClientInfo(paymentServiceConnInfo, getAccountBalanceMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to initiate BONUS balance inquiry for game instance [" + results.getGameInstanceId()
					+ "] from tenant [" + tenantId + "] for user [" + results.getUserId() + "]", e);
		}
	}
	
	@Override
	public void requestSinglePlayerGamePaymentTransfer(String userId, String gameId, String multiplayerGameInstanceId, String gameInstanceId,
	            BigDecimal amount, Currency currency, JsonMessage<? extends JsonMessageContent> sourceMessage, SessionWrapper session) {
		final String tenantId = sourceMessage.getClientInfo().getTenantId();
		final String appId = sourceMessage.getClientInfo().getAppId();
		final String reason = USER_BONUS_REASON;
		
		final TransferFundsRequestBuilder requestBuilder = new TransferFundsRequestBuilder()		
				.fromTenantToProfile(tenantId, userId)
				.amount(amount, currency)
				.withPaymentDetails(new PaymentDetailsBuilder()
						.appId(appId).additionalInfo(reason)
						.gameId(gameId).gameInstanceId(gameInstanceId).multiplayerGameInstanceId(multiplayerGameInstanceId)
						.build());
		final TransferFundsRequest request = requestBuilder.buildSingleUserPaymentForGame();
		
		final JsonMessage<TransferFundsRequest> transactionRequestMessage = jsonMessageUtil.createNewMessage(
				requestBuilder.getBuildSingleUserPaymentForGameRequestType(), request);

		String transactionLogId = transactionLogDao.createTransactionLog(new TransactionLog(null, tenantId, userId,
				gameId, multiplayerGameInstanceId, gameInstanceId, amount, currency, reason, appId));
		
		final ServiceRequestInfo requestInfo = new ServiceRequestInfo(gameInstanceId, Collections.singletonList(transactionLogId));
		requestInfo.setSourceMessage(sourceMessage);
		requestInfo.setSourceSession(session);
		try {
			final ServiceConnectionInformation paymentServiceConnInfo = serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(paymentServiceConnInfo, transactionRequestMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to initiate [" + amount + " " + currency + "] transfer for game [" + gameId + "], multiplayerGameInstanceId [" + multiplayerGameInstanceId + "], "
					+ "game instance [" + gameInstanceId + "] from tenant [" + tenantId + "] to user [" + userId + "]", e);
		}
	}

	@Override
	public void requestMultiplayerGamePaymentTransfer(UserResultsByHandicapRange results, boolean isTournament) {
		final CloseJackpotRequest request = new CloseJackpotRequest();
		request.setMultiplayerGameInstanceId(results.getMultiplayerGameInstanceId());
		request.setPaymentDetails(new PaymentDetailsBuilder()
				.gameId(results.getGame().getGameId())
				.gameInstanceId(null) //multiple values userResult.getGameInstanceId(), so don't use any
				.multiplayerGameInstanceId(results.getMultiplayerGameInstanceId())
				.build());
		RefundReason refundReason = results.getRefundReason();
		if (refundReason != null) {
			request.getPaymentDetails().setAdditionalInfo(REFUND_REASON_PREFIX + refundReason);
		} else {
			request.getPaymentDetails().setAdditionalInfo(JACKPOT_WINNING_REASON);
		}
		request.setPayouts(new ArrayList<>());
		results.forEach(userResult -> {
			request.setTenantId(userResult.getTenantId());
			request.getPaymentDetails().setAppId(userResult.getAppId());

			if (userResult.getJackpotWinning() != null || refundReason != null) {
				PayoutItem payoutItem = new PayoutItem();
				payoutItem.setProfileId(userResult.getUserId());
				if (userResult.getJackpotWinning() != null) {
					if (isTournament && userResult.isAdditionalPaymentForWinning()
							&& !commonGameInstanceAerospikeDao.isRecordInTheDatabaseOfPaymentOfAdditionalWinnings(results.getMultiplayerGameInstanceId(), userResult.getUserId())) {
						commonGameInstanceAerospikeDao.createRecordInTheDatabaseOfPaymentOfAdditionalWinnings(results.getMultiplayerGameInstanceId(), userResult.getUserId());
						winSurcharge(userResult.getAppId(), userResult.getTenantId(), userResult.getUserId(), BigDecimal.valueOf(1), userResult.getJackpotWinningCurrency(), userResult.getGameInstanceId());
						payoutItem.setAmount(userResult.getJackpotWinning().subtract(BigDecimal.valueOf(1)));
					} else {
						payoutItem.setAmount(userResult.getJackpotWinning());
					}


				} else {
					assert refundReason != null;
					payoutItem.setAmount(userResult.getEntryFeePaid());
				}
				request.getPayouts().add(payoutItem);
			}
		});

		if (!request.getPayouts().isEmpty()) {
			final JsonMessage<CloseJackpotRequest> transactionRequestMessage = jsonMessageUtil.createNewMessage(
					PaymentMessageTypes.CLOSE_JACKPOT, request);
			sendCloseJackpotMessage(transactionRequestMessage, results.getEntryFeeCurrency());
		}
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
		String transactionLogId = transactionLogDao.createTransactionLog(new TransactionLog(request, currency, "Supplement win the tournament", appId));
		winRequestInfo.setTransactionLogId(transactionLogId);
		try {
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME),
					transactionRequestMessage, winRequestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send transferTransaction request to payment service", e);
		}
	}

	private class WinSurchargeRequestInfo extends ServiceRequestInfo {
		private Currency currency;
		private String transactionLogId;

		public WinSurchargeRequestInfo(String gameInstanceId, Currency currency) {
			super(gameInstanceId);
			this.currency = currency;
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


	private void sendCloseJackpotMessage(final JsonMessage<CloseJackpotRequest> transactionRequestMessage, Currency currency) {
		final CloseJackpotRequest request = transactionRequestMessage.getContent();
		List<String> transactionIds = new ArrayList<>(request.getPayouts().size());
		for (PayoutItem payoutItem : request.getPayouts()) {
			transactionIds.add(transactionLogDao.createTransactionLog(
					new TransactionLog(null, request.getTenantId(), payoutItem.getProfileId(),
							request.getPaymentDetails().getGameId(),
							request.getPaymentDetails().getMultiplayerGameInstanceId(), request.getPaymentDetails().getGameInstanceId(), payoutItem.getAmount(),
							currency, request.getPaymentDetails().getAdditionalInfo(), request.getPaymentDetails().getAppId())));
		}
		
		final ServiceRequestInfo requestInfo = new ServiceRequestInfo(request.getPaymentDetails().getGameInstanceId(), transactionIds);
		try {
			final ServiceConnectionInformation paymentServiceConnInfo = serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(paymentServiceConnInfo, transactionRequestMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to initiate transfer for game [" + request.getPaymentDetails().getGameId()
					+ "], multiplayerGameInstanceId [" + request.getPaymentDetails().getMultiplayerGameInstanceId()
					+ "], game instance [" + request.getPaymentDetails().getGameInstanceId()
					+ "] from tenant [" + request.getTenantId()
					+ "] to [" + StringUtils.join(request.getPayouts(), ", ") + "]", e);
		}
	}
}
