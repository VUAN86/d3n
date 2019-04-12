package de.ascendro.f4m.service.promocode.client;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.*;
import de.ascendro.f4m.service.payment.model.internal.LoadOrWithdrawWithoutCoverageRequest;
import de.ascendro.f4m.service.payment.model.internal.PaymentDetails;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;

/**
 * Class for sending user information requests to Profile Service
 */
public class DependencyServicesCommunicator {
	private static final String USING_PROMOCODE_REASON = "Used promocode";

	private ServiceRegistryClient serviceRegistryClient;
	private JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private JsonMessageUtil jsonUtil;
	private TransactionLogAerospikeDao transactionLogAerospikeDao;
	private static final Logger LOGGER = LoggerFactory.getLogger(DependencyServicesCommunicator.class);

	@Inject
	public DependencyServicesCommunicator(ServiceRegistryClient serviceRegistryClient,
										  JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, JsonMessageUtil jsonUtil,
										  TransactionLogAerospikeDao transactionLogAerospikeDao) {
		this.serviceRegistryClient = serviceRegistryClient;
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.jsonUtil = jsonUtil;
		this.transactionLogAerospikeDao = transactionLogAerospikeDao;
	}

	/**
	 * This method starts the payment request for the promocode :
	 */
	public void initiateUserPromocodePayment(JsonMessage<? extends JsonMessageContent> originalMessage,
											 SessionWrapper sessionWrapper,
											 String userId, String promocodeId,
											 BigDecimal amount, Currency currency) {
		final String tenantId = originalMessage.getClientInfo().getTenantId();
		final String appId = originalMessage.getClientInfo().getAppId();

		TransferFundsRequestBuilder builder = new TransferFundsRequestBuilder()
				.fromTenantToProfile(tenantId, userId)
				.amount(amount, currency)
				.withPaymentDetails(new PaymentDetailsBuilder()
						.promocodeId(promocodeId)
						.appId(appId)
						.build());
		TransferFundsRequest transactionRequest = builder.buildSingleUserPaymentForGame();
		final JsonMessage<TransferFundsRequest> transactionRequestMessage = jsonUtil.createNewMessage(
				builder.getBuildSingleUserPaymentForGameRequestType(), transactionRequest);
		final PromocodeRequestInfo requestInfo = new PromocodeRequestInfo();
		requestInfo.setPromocodeId(promocodeId);

		transactionRequest.getPaymentDetails().setAdditionalInfo("promocode");

		String transactionLogId = transactionLogAerospikeDao.createTransactionLog(new TransactionLog(transactionRequest, currency, USING_PROMOCODE_REASON, appId));
		requestInfo.setTransactionLogId(transactionLogId);

		requestInfo.setCurrency(currency);
		requestInfo.setSourceMessage(originalMessage);
		requestInfo.setSourceSession(sessionWrapper);
		try {
			LOGGER.debug("initiateUserPromocodePayment transactionRequestMessage={} requestInfo={}",transactionRequestMessage,requestInfo);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME),
					transactionRequestMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send transferTransaction request to payment service", e);
		}
	}
	/**
	 * This method starts the payment request for the promocode :
	 */
	public void creditUserPromocodePayment(
			String promocode,
			String tenantId,
			String profileId,
			int amount,
			Currency currency
	) {
		LoadOrWithdrawWithoutCoverageRequest request = new LoadOrWithdrawWithoutCoverageRequest();
		request.setTenantId(tenantId);
		request.setProfileId(profileId);
		request.setCurrency(Currency.BONUS);
		request.setAmount(new BigDecimal(amount));
		PaymentDetails details = new PaymentDetails();
		details.setPromocodeId(promocode);
		details.setAdditionalInfo("promocode");
		request.setPaymentDetails(details);
		final JsonMessage<TransferFundsRequest> transactionRequestMessage = jsonUtil.createNewMessage(
				PaymentMessageTypes.LOAD_OR_WITHDRAW_WITHOUT_COVERAGE, request);
		final PromocodeRequestInfo requestInfo = new PromocodeRequestInfo();
		requestInfo.setPromocodeId(promocode);
		String transactionLogId = transactionLogAerospikeDao.createTransactionLog(new TransactionLog(request, currency, USING_PROMOCODE_REASON, ""));
		requestInfo.setTransactionLogId(transactionLogId);
		requestInfo.setCurrency(currency);
		try {
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME),
					transactionRequestMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send transferTransaction request to payment service", e);
		}
	}

}
