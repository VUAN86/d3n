package de.ascendro.f4m.service.promocode.client;

import java.math.BigDecimal;

import javax.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.communicator.RabbitClientSender;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.handler.MessageHandler;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.handler.JsonAuthenticationMessageMQHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.MessageSource;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentDetailsBuilder;
import de.ascendro.f4m.service.payment.model.TransactionLog;
import de.ascendro.f4m.service.payment.model.TransferFundsRequest;
import de.ascendro.f4m.service.payment.model.TransferFundsRequestBuilder;
import de.ascendro.f4m.service.payment.model.internal.LoadOrWithdrawWithoutCoverageRequest;
import de.ascendro.f4m.service.payment.model.internal.PaymentDetails;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

/**
 * Class for sending user information requests to Profile Service
 */
public class DependencyServicesCommunicator {
	private static final String USING_PROMOCODE_REASON = "Used promocode";

//	private ServiceRegistryClient serviceRegistryClient;
//	private JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private JsonMessageUtil jsonUtil;
//	private RabbitClientSender rabbitClientSender;
	private TransactionLogAerospikeDao transactionLogAerospikeDao;

	@Inject
	public DependencyServicesCommunicator(
//			ServiceRegistryClient serviceRegistryClient,
//										  JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool,
											JsonMessageUtil jsonUtil,
//											RabbitClientSender rabbitClientSender,
											TransactionLogAerospikeDao transactionLogAerospikeDao) {
//		this.serviceRegistryClient = serviceRegistryClient;
//		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
//		this.rabbitClientSender = rabbitClientSender;
		this.jsonUtil = jsonUtil;
		this.transactionLogAerospikeDao = transactionLogAerospikeDao;
	}

	/**
	 * This method starts the payment request for the promocode :
	 */
	public void initiateUserPromocodePayment(JsonMessage<? extends JsonMessageContent> originalMessage,
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

		String transactionLogId = transactionLogAerospikeDao.createTransactionLog(new TransactionLog(transactionRequest, currency, USING_PROMOCODE_REASON, appId));
		requestInfo.setTransactionLogId(transactionLogId);

		requestInfo.setCurrency(currency);
		requestInfo.setSourceMessage(originalMessage);
		System.out.println("setSourceMessageSource="+originalMessage.getMessageSource());
		requestInfo.setSourceMessageSource(originalMessage.getMessageSource());
		try {
			RabbitClientSender.sendAsyncMessageWithClientInfo(PaymentMessageTypes.SERVICE_NAME,transactionRequestMessage, requestInfo);
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
		request.setPaymentDetails(details);
		final JsonMessage<TransferFundsRequest> transactionRequestMessage = jsonUtil.createNewMessage(
				PaymentMessageTypes.LOAD_OR_WITHDRAW_WITHOUT_COVERAGE, request);
		final PromocodeRequestInfo requestInfo = new PromocodeRequestInfo();
		requestInfo.setPromocodeId(promocode);
		String transactionLogId = transactionLogAerospikeDao.createTransactionLog(new TransactionLog(request, currency, USING_PROMOCODE_REASON, ""));
		requestInfo.setTransactionLogId(transactionLogId);
		requestInfo.setCurrency(currency);
		try {
			RabbitClientSender.sendAsyncMessageWithClientInfo(PaymentMessageTypes.SERVICE_NAME,transactionRequestMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send transferTransaction request to payment service", e);
		}
	}

}
