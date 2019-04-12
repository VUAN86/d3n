package de.ascendro.f4m.service.tombola.client;

import static de.ascendro.f4m.service.usermessage.translation.Messages.TOMBOLA_OPEN_ANNOUNCEMENT_PUSH;

import de.ascendro.f4m.service.communicator.RabbitClientSender;
import de.ascendro.f4m.service.json.model.MessageSource;
import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.publish.PublishMessageContent;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.PaymentDetailsBuilder;
import de.ascendro.f4m.service.payment.model.TransactionLog;
import de.ascendro.f4m.service.payment.model.TransferFundsRequest;
import de.ascendro.f4m.service.payment.model.TransferFundsRequestBuilder;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.tombola.config.TombolaConfig;
import de.ascendro.f4m.service.tombola.model.Bundle;
import de.ascendro.f4m.service.tombola.model.Tombola;
import de.ascendro.f4m.service.tombola.model.events.TombolaEvents;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.usermessage.model.SendTopicPushRequest;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherAssignForTombolaRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherReleaseRequest;

public class DependencyServicesCommunicator {

	private static final String BUY_TICKETS_REASON = "Bought tickets bundle for tombola";
	private static final String PRIZE_PAYOUT_REASON = "Prize won in tombola";
	private static final String TOMBOLA_OPEN_NOTIFICATION_TOPIC = "tombola.open.announcement.push";

//	private final ServiceRegistryClient serviceRegistryClient;
//	private final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private final JsonMessageUtil jsonUtil;
	private final TransactionLogAerospikeDao transactionLogAerospikeDao;
	private final TombolaConfig config;

	@Inject
	public DependencyServicesCommunicator(
//			ServiceRegistryClient serviceRegistryClient, JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool,
			JsonMessageUtil jsonUtil,
			TransactionLogAerospikeDao transactionLogAerospikeDao, TombolaConfig config) {
//		this.serviceRegistryClient = serviceRegistryClient;
//		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.jsonUtil = jsonUtil;

		this.transactionLogAerospikeDao = transactionLogAerospikeDao;
		this.config = config;
	}

	/**
	 * This method starts the payment request for the promocode :
	 */
	public void initiateTombolaBuyTicketsPayment(JsonMessage<? extends JsonMessageContent> originalMessage,
												 String userId, Bundle bundle, String tombolaId,
												 String appId, String tenantId) {

		final TransferFundsRequestBuilder builder = new TransferFundsRequestBuilder()
				.fromProfileToTenant(userId, tenantId)
				.amount(bundle.getPrice(), bundle.getCurrency())
				.withPaymentDetails(new PaymentDetailsBuilder()
						.appId(appId).additionalInfo(BUY_TICKETS_REASON)
						.tombolaId(tombolaId)
						.tombolaTicketsAmount(bundle.getAmount())
						.build());

		TransferFundsRequest transactionRequest = builder.buildSingleUserPaymentForGame();
		final JsonMessage<TransferFundsRequest> transactionRequestMessage = jsonUtil.createNewMessage(
				builder.getBuildSingleUserPaymentForGameRequestType(), transactionRequest);

		final TicketPurchaseRequestInfo requestInfo = new TicketPurchaseRequestInfo(tombolaId);
		requestInfo.setNumberOfTicketsBought(bundle.getAmount());
		requestInfo.setPrice(bundle.getPrice());
		requestInfo.setCurrency(bundle.getCurrency());
		requestInfo.setBundleImageId(bundle.getImageId());
		requestInfo.setAppId(appId);
		requestInfo.setTenantId(tenantId);

		String transactionLogId = transactionLogAerospikeDao.createTransactionLog(new TransactionLog(transactionRequest,
				bundle.getCurrency(), BUY_TICKETS_REASON, appId));
		requestInfo.setTransactionLogId(transactionLogId);

		requestInfo.setSourceMessage(originalMessage);
		requestInfo.setSourceMessageSource(originalMessage.getMessageSource());
		try {
			RabbitClientSender.sendAsyncMessageWithClientInfo(PaymentMessageTypes.SERVICE_NAME, transactionRequestMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send transferTransaction request to payment service", e);
		}
	}

	public void initiatePushNotificationForNewTombola(Tombola tombola) {
		SendTopicPushRequest push = new SendTopicPushRequest();
		push.setTopic(TOMBOLA_OPEN_NOTIFICATION_TOPIC);
		push.setMessage(TOMBOLA_OPEN_ANNOUNCEMENT_PUSH);
		push.setAppIds(tombola.getApplicationsIds().stream().toArray(String[]::new));

		JsonMessage<SendTopicPushRequest> message = jsonUtil.createNewMessage(UserMessageMessageTypes.SEND_TOPIC_PUSH,
				push);

		try {
			RabbitClientSender.sendAsyncMessage(UserMessageMessageTypes.SERVICE_NAME,message);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send sendTopicPushRequest to userMessage service", e);
		}
	}

	public void initiatePrizePayment(PrizePayoutRequestInfo requestInfo) {
		final TransferFundsRequestBuilder builder = new TransferFundsRequestBuilder()
				.fromTenantToProfile(requestInfo.getTenantId(), requestInfo.getUserId())
				.amount(requestInfo.getAmount(), requestInfo.getCurrency())
				.withPaymentDetails(new PaymentDetailsBuilder()
						.additionalInfo(PRIZE_PAYOUT_REASON)
						.tombolaId(requestInfo.getTombolaId())
						.appId(requestInfo.getAppId())
						.build());

		TransferFundsRequest transactionRequest = builder.buildSingleUserPaymentForGame();
		final JsonMessage<TransferFundsRequest> transactionRequestMessage = jsonUtil.createNewMessage(
				builder.getBuildSingleUserPaymentForGameRequestType(), transactionRequest);

		String transactionLogId = transactionLogAerospikeDao.createTransactionLog(new TransactionLog(transactionRequest,
				requestInfo.getCurrency(), PRIZE_PAYOUT_REASON, requestInfo.getAppId()));
		requestInfo.setTransactionLogId(transactionLogId);

		try {
			RabbitClientSender.sendAsyncMessageWithClientInfo(PaymentMessageTypes.SERVICE_NAME,transactionRequestMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send transferFunds request to payment service", e);
		}
	}

	public void requestUserVoucherAssign(String tombolaName, PrizePayoutRequestInfo requestInfo) {
		final UserVoucherAssignForTombolaRequest request = new UserVoucherAssignForTombolaRequest(
				requestInfo.getVoucherId(), requestInfo.getUserId(),
				requestInfo.getTombolaId(), tombolaName, requestInfo.getTenantId(), requestInfo.getAppId());
		final JsonMessage<UserVoucherAssignForTombolaRequest> message = jsonUtil.createNewMessage(
				VoucherMessageTypes.USER_VOUCHER_ASSIGN_FOR_TOMBOLA, request);

		try {
			RabbitClientSender.sendAsyncMessageWithClientInfo(VoucherMessageTypes.SERVICE_NAME, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send userVoucherAssignForTombola request to voucher service",
					e);
		}
	}

	public void sendEmailToAdmin(String subject, String[] subjectParameters, String body, String[] bodyParameters) {
		String emailAddress = config.getProperty(TombolaConfig.ADMIN_EMAIL);
		if (StringUtils.isNotBlank(emailAddress)) {
			SendEmailWrapperRequest request = new SendEmailWrapperRequest();
			request.setAddress(emailAddress);
			request.setSubject(subject);
			request.setSubjectParameters(subjectParameters);
			request.setMessage(body);
			request.setParameters(bodyParameters);
			request.setISOLanguage(ISOLanguage.EN);
			JsonMessage<SendEmailWrapperRequest> requestJson = jsonUtil
					.createNewMessage(UserMessageMessageTypes.SEND_EMAIL, request);
			try {
				RabbitClientSender.sendAsyncMessage(UserMessageMessageTypes.SERVICE_NAME,requestJson);
			} catch (F4MValidationFailedException | F4MIOException e) {
				throw new F4MFatalErrorException("Unable to send sendEmail request to user message service", e);
			}
		}
	}

	public void requestUserVoucherRelease(JsonMessage<? extends JsonMessageContent> sourceMessage,
										  String voucherId) {
		final UserVoucherReleaseRequest request = new UserVoucherReleaseRequest(voucherId);
		final JsonMessage<UserVoucherReleaseRequest> message = jsonUtil.createNewMessage(
				VoucherMessageTypes.USER_VOUCHER_RELEASE, request);
		final RequestInfo requestInfo = new RequestInfoImpl();
		requestInfo.setSourceMessage(sourceMessage);
		requestInfo.setSourceMessageSource(message.getMessageSource());
		try {
			RabbitClientSender.sendAsyncMessageWithClientInfo(VoucherMessageTypes.SERVICE_NAME, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send userVoucherRelease request to voucher service", e);
		}
	}

	public void requestTombolaDraw(JsonMessage<? extends JsonMessageContent> sourceMessage,
			String tombolaId, String tenantId) {
		final TombolaEvents drawEvent = new TombolaEvents();
		drawEvent.setTombolaId(tombolaId);
		drawEvent.setTenantId(tenantId);
		final PublishMessageContent request = new PublishMessageContent();
		request.setNotificationContent(drawEvent.getJsonObject());
		request.setTopic(TombolaEvents.getDrawEventTopic(tombolaId));
		request.setVirtual(true);

		final JsonMessage<PublishMessageContent> message = jsonUtil.createNewMessage(
				EventMessageTypes.PUBLISH, request);
		final RequestInfo requestInfo = new RequestInfoImpl();
		requestInfo.setSourceMessage(sourceMessage);
		requestInfo.setSourceMessageSource(sourceMessage.getMessageSource());
		try {
			RabbitClientSender.sendAsyncMessageWithClientInfo(EventMessageTypes.SERVICE_NAME, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send tombola draw event notification to event service", e);
		}
	}
}
