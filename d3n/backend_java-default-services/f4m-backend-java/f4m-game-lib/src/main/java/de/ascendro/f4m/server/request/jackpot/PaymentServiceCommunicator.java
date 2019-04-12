package de.ascendro.f4m.server.request.jackpot;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.*;
import de.ascendro.f4m.service.payment.model.internal.*;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;


public class PaymentServiceCommunicator {
	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentServiceCommunicator.class);

	private ServiceRegistryClient serviceRegistryClient;
	private JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private JsonMessageUtil jsonMessageUtil;

	@Inject
	public TransactionLogAerospikeDao transactionLogAerospikeDao;

	@Inject
	public PaymentServiceCommunicator(ServiceRegistryClient serviceRegistryClient,
			JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, JsonMessageUtil jsonMessageUtil) {
        this.serviceRegistryClient = serviceRegistryClient;
        this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
        this.jsonMessageUtil = jsonMessageUtil;
	}

	// creating a game account in paydent
	public void sendCreateJackpotRequest(String instanceId, CustomGameConfig customGameConfig, Game game,
										 JsonMessage<?> sourceMessage, SessionWrapper sourceSession) {
		final CreateJackpotRequest createJackpotRequest = new CreateJackpotRequest();
		createJackpotRequest.setMultiplayerGameInstanceId(instanceId);
		createJackpotRequest.setTenantId(customGameConfig.getTenantId());

		final Currency entryFeeCurrency = getCurrency(customGameConfig, game);
		createJackpotRequest.setCurrency(entryFeeCurrency);

		final RequestInfoImpl requestInfo = new PaymentCreateJackpotRequestInfo(instanceId, sourceMessage, sourceSession);

		JsonMessage<CreateJackpotRequest> message = jsonMessageUtil.createNewMessage(PaymentMessageTypes.CREATE_JACKPOT,
				createJackpotRequest);
		try {
			ServiceConnectionInformation paymentConnInfo = serviceRegistryClient
					.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(paymentConnInfo, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send CreateJackpotRequest to Payment Service", e);
		}

	}

	// getting the balance of the game
	public void sendGetJackpotReq(String mgiId, CustomGameConfig config, SessionWrapper sessionWrapper,  JsonMessage message) {
		PaymentGetJackpotRequestInformation paymentGetJackpotRequestInfo =
				new PaymentGetJackpotRequestInformation(config.getGameId(), mgiId, config.getEntryFeeAmount(), config.getEntryFeeCurrency(),
														message, sessionWrapper, new GameIdCancelGameTournamentResponse(config.getGameId()));
		sendGetJackpotRequest(mgiId, config.getTenantId(), paymentGetJackpotRequestInfo);
	}

	// getting the balance of the game
	public void sendGetJackpotRequest(String multiplayerGameInstanceId, String tenantId,
									  RequestInfoImpl requestInfo) {

		GetJackpotRequest jackpotRequest = new GetJackpotRequest();
		jackpotRequest.setMultiplayerGameInstanceId(multiplayerGameInstanceId);
		jackpotRequest.setTenantId(tenantId);
		JsonMessage<GetJackpotRequest> message = jsonMessageUtil
				.createNewMessage(PaymentMessageTypes.GET_JACKPOT, jackpotRequest);
		try {
			final ServiceConnectionInformation connInfo = serviceRegistryClient
					.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(connInfo, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send GetJackpotRequest to Payment Service", e);
		}
	}

	// when creating a tournament, the minimum guaranteed reward is listed on the jackpot, which is debited from the tenant(only money)
	public void sendTransferMinimumJackpotGuarantee(
			CustomGameConfig customGameConfig,
			Game game,
			JsonMessage<?> sourceMessage,
			SessionWrapper sourceSession,
			final String mgiId
	) {

		final String appId = customGameConfig.getAppId() != null ? customGameConfig.getAppId() : "6";
		final String toTenantId = customGameConfig.getTenantId();

		final String gameId = game.getGameId();
		final Currency currency = Currency.MONEY;
		final BigDecimal minimumJackpotGarantie = BigDecimal.valueOf(customGameConfig.getMinimumJackpotGarantie());

		TransferFundsRequestBuilder requestBuilder = new TransferFundsRequestBuilder()
				.fromTenantToProfile(toTenantId, "mgi_" + mgiId)
				.withMultiplayerGameInstanceId(mgiId)
				.amount(minimumJackpotGarantie)
				.currency(currency)
				.withPaymentDetails(new PaymentDetailsBuilder().gameId(gameId).multiplayerGameInstanceId(mgiId).appId(appId).build());

        TransferFundsRequest request = requestBuilder.buildSingleUserPaymentForGame();


        final JsonMessage<TransferFundsRequest> transactionRequestMessage = jsonMessageUtil.createNewMessage(
				requestBuilder.getBuildSingleUserPaymentForGameRequestType(), request);


		String transactionLogId = transactionLogAerospikeDao.createTransactionLog(
				new TransactionLog(request, currency, "Transfer minimum jackpot guarantee", appId));

		final PaymentRequestInformation requestInfo = new PaymentRequestInformation(
			gameId,
			mgiId,
			transactionLogId,
			minimumJackpotGarantie,
			currency,
			game.getType());

		requestInfo.setSourceSession(sourceSession);
		requestInfo.setSourceMessage(sourceMessage);

		try {
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME),
					transactionRequestMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send TransferMinimumJackpotGuarantee to Payment Service", e);
		}
	}

	public String withdrawFromThePlayerAccount(String appId, String toTenantId, BigDecimal amount, String userFromId) {
		final Currency currency = Currency.MONEY;
		LOGGER.debug("withdrawFromThePlayerAccount amount {}  ", amount);
		TransferFundsRequestBuilder requestBuilder = new TransferFundsRequestBuilder().fromProfileToTenant(userFromId, toTenantId)
																					  .amount(amount).currency(currency)
																					  .withPaymentDetails(new PaymentDetailsBuilder().appId(appId).build());
		TransferFundsRequest request = requestBuilder.buildPaymentBetweenUsers();
		final JsonMessage<TransferFundsRequest> transactionRequestMessage = jsonMessageUtil.createNewMessage(requestBuilder
																													 .getBuildSingleUserPaymentForGameRequestType(), request);
        String transactionLogId = transactionLogAerospikeDao
                .createTransactionLog(new TransactionLog(request, currency, "Withdraw from the player's account", appId));
		LOGGER.debug("withdrawFromThePlayerAccount transactionLogId {} ", transactionLogId);

		final PaymentRequestInformation requestInfo = new PaymentRequestInformation(null, null, transactionLogId, amount, currency, null);
		try {
            jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(serviceRegistryClient
                                                                                  .getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME), transactionRequestMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send TransferMinimumJackpotGuarantee to Payment Service", e);
		}
		return transactionLogId;
	}

    public void transferFundsToUserAccount(String tenantId, String profileId, BigDecimal amount, Currency currency, String appId) {
        LoadOrWithdrawWithoutCoverageRequest request = new LoadOrWithdrawWithoutCoverageRequest();
        PaymentDetails paymentDetails = new PaymentDetails();
        request.setTenantId(tenantId);
        request.setProfileId(profileId);
        request.setCurrency(currency);
        request.setAmount(amount);
        request.setPaymentDetails(paymentDetails);
        String transactionLogId = transactionLogAerospikeDao.createTransactionLog(new TransactionLog(request, currency, "Transfer funds to user account.", appId));
        PaymentService paymentService = new PaymentService();
        paymentService.setTransactionLogId(transactionLogId);
        paymentService.setCurrency(currency);
        final JsonMessage<TransferFundsRequest> transactionRequestMessage = jsonMessageUtil.createNewMessage(PaymentMessageTypes.LOAD_OR_WITHDRAW_WITHOUT_COVERAGE, request);
        try {
            jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(
                    serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME), transactionRequestMessage, paymentService);
        } catch (F4MValidationFailedException | F4MIOException e) {
            LOGGER.error("transferFundsToUserAccount message{}", e.getMessage());
            throw new F4MFatalErrorException("Unable to send transferTransaction request to payment service", e);
        }
    }

	// Receiving currency for the jackpot.
    public Currency getCurrency(CustomGameConfig customGameConfig, Game game) {
        final Currency currency = firstNonNull(customGameConfig.getEntryFeeCurrency(), game.getEntryFeeCurrency());
		if (game.isTournament() && currency != Currency.MONEY && !game.getResultConfiguration().isJackpotCalculateByEntryFee()) {
			return Currency.MONEY;
		}
        return currency;
    }


    // Return of funds for the game if after the end of the duel time, no one played or refused.
    public void refundPayment(String mgiId, CustomGameConfig customGameConfig) {
            RefundReason refundReason = RefundReason.NO_OPPONENT;

            final CloseJackpotRequest request = new CloseJackpotRequest();
            request.setMultiplayerGameInstanceId(mgiId);
            request.setPaymentDetails(new PaymentDetailsBuilder()
                    .gameId(customGameConfig.getGameId())
                    .gameInstanceId(null) //multiple values userResult.getGameInstanceId(), so don't use any
                    .multiplayerGameInstanceId(mgiId)
                    .build());
            request.getPaymentDetails().setAdditionalInfo("Refund: " + refundReason);
            request.setPayouts(new ArrayList<>());
            request.setTenantId(customGameConfig.getTenantId());
            request.getPaymentDetails().setAppId(customGameConfig.getAppId());

		if (!customGameConfig.getGameType().isTournament()) {
			CloseJackpotRequest.PayoutItem payoutItem = new CloseJackpotRequest.PayoutItem();
			payoutItem.setProfileId(customGameConfig.getGameCreatorId());
			payoutItem.setAmount(customGameConfig.getEntryFeeAmount());

			request.getPayouts().add(payoutItem);
		}
            final JsonMessage<CloseJackpotRequest> transactionRequestMessage = jsonMessageUtil.createNewMessage(
                    PaymentMessageTypes.CLOSE_JACKPOT, request);
            sendCloseJackpotMessage(transactionRequestMessage, customGameConfig.getEntryFeeCurrency());
    }





	private void sendCloseJackpotMessage(final JsonMessage<CloseJackpotRequest> transactionRequestMessage, Currency currency) {
		final CloseJackpotRequest request = transactionRequestMessage.getContent();
		List<String> transactionIds = new ArrayList<>(request.getPayouts().size());
		for (CloseJackpotRequest.PayoutItem payoutItem : request.getPayouts()) {
			transactionIds.add(transactionLogAerospikeDao.createTransactionLog(
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
