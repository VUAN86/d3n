package de.ascendro.f4m.service.tombola.client;

import static de.ascendro.f4m.service.usermessage.translation.Messages.TOMBOLA_OPEN_ANNOUNCEMENT_PUSH;

import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.model.AppConfig;
import de.ascendro.f4m.service.usermessage.translation.Messages;
import org.apache.commons.io.FilenameUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyServicesCommunicator {
	private static final Logger LOGGER = LoggerFactory.getLogger(DependencyServicesCommunicator.class);

	private static final String BUY_TICKETS_REASON = "Bought tickets bundle for tombola";
	private static final String PRIZE_PAYOUT_REASON = "Prize won in tombola";
	private static final String TOMBOLA_OPEN_NOTIFICATION_TOPIC = "tombola.open.announcement.push";
	private static final String GAME_PARAMETER_NOT_USED = "none";
	private static final String CDN_MEDIA_TOMBOLA_NAMESPACE = "tombola/";
	private static final String EMPTY_IMAGE_URL = "#";
	private static final String IMAGE_RESOLUTION_MEDIUM = "_medium";

	private final ServiceRegistryClient serviceRegistryClient;
	private final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private final JsonMessageUtil jsonUtil;
	private final TransactionLogAerospikeDao transactionLogAerospikeDao;
	private final TombolaConfig config;
	private final ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao;

	@Inject
	public DependencyServicesCommunicator(ServiceRegistryClient serviceRegistryClient,
			JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, JsonMessageUtil jsonUtil,
			TransactionLogAerospikeDao transactionLogAerospikeDao, TombolaConfig config,
			ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao) {
		this.serviceRegistryClient = serviceRegistryClient;
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.jsonUtil = jsonUtil;
		this.transactionLogAerospikeDao = transactionLogAerospikeDao;
		this.config = config;
		this.applicationConfigurationAerospikeDao = applicationConfigurationAerospikeDao;
	}

	/**
	 * This method starts the payment request for the promocode :
	 */
	public void initiateTombolaBuyTicketsPayment(JsonMessage<? extends JsonMessageContent> originalMessage,
			SessionWrapper sessionWrapper,String userId, Bundle bundle, String tombolaId, String appId,
			String tenantId) {

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
		requestInfo.setSourceSession(sessionWrapper);
		try {
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(
					serviceRegistryClient.getServiceConnectionInformation(
							PaymentMessageTypes.SERVICE_NAME), transactionRequestMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send transferTransaction request to payment service", e);
		}
	}

	/**
	 * This method will initiate a sendEmail call on the user service.
	 */
	public void sendTombolaEndToUser(String userId,
									 Tombola tombola, String tenantId,
									 String appId, boolean winner) {
		String voucherBoughtAssignedToUserContentMsg = winner ?
				Messages.TOMBOLA_DRAW_WIN_EMAIL_CONTENT : //TOMBOLA_DRAW_CONSOLATION_EMAIL_CONTENT
				Messages.TOMBOLA_DRAW_LOSE_EMAIL_CONTENT;
		sendTombolaEmailEndToUser(Messages.TOMBOLA_DRAW_EMAIL_SUBJECT,
				voucherBoughtAssignedToUserContentMsg, userId, tombola, GAME_PARAMETER_NOT_USED,
				tenantId, appId);
	}

	private void sendTombolaEmailEndToUser(String subject, String body, String userId,
										   Tombola tombola, String sourceTitle, String tenantId, String appId) {
		//consider if clientInfo can be used instead of two parameters userId&appId
		String[] parameters = getTombolaEmailParameters(tombola, tenantId, appId);
		sendEmailToUser(subject, body, userId, tombola, parameters);
	}

	private String[] getTombolaEmailParameters(Tombola tombola, String tenantId, String appId) {
		String imageUrl = calculateVoucherImageURL(tombola, tenantId, appId);
//		if (StringUtils.isNotEmpty(tombola.getRedemptionURL())) {
			return new String[]{ tombola.getName(), imageUrl};
//					imageUrl, tombola.getRedemptionURL() };
//		} else {
//			return new String[]{ tombola.getName(), imageUrl, userVoucher.getCode(), voucherCompany, expirationDate,
//					imageUrl};
//		}
	}

	private void sendEmailToUser(String subject, String body, String userId, Tombola tombola, String[] parameters) {
		for (int i=0; i<parameters.length; i++){
			if (parameters[i]==null){
				parameters[i]="NULL";
				LOGGER.error("sendEmailToUser {} parameter in NULL : [{}]",i,parameters);
			}
		}

		SendEmailWrapperRequest request = new SendEmailWrapperRequest();
		request.setUserId(userId);
		request.setSubject(subject);
		request.setMessage(body);
		request.setParameters(parameters);
		request.setLanguageAuto();
		JsonMessage<SendEmailWrapperRequest> requestJson = jsonUtil.createNewMessage(
				UserMessageMessageTypes.SEND_EMAIL, request);
		try {
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(
					serviceRegistryClient.getServiceConnectionInformation(UserMessageMessageTypes.SERVICE_NAME),
					requestJson, new TombolaRequestInfo(tombola.getId()));
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send sendEmail request", e);
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
			jsonWebSocketClientSessionPool.sendAsyncMessage(
					serviceRegistryClient.getServiceConnectionInformation(UserMessageMessageTypes.SERVICE_NAME),
					message);
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
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(
					serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME),
					transactionRequestMessage, requestInfo);
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
			final ServiceConnectionInformation connInfo = serviceRegistryClient.getServiceConnectionInformation(
					VoucherMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(connInfo, message, requestInfo);
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
				jsonWebSocketClientSessionPool.sendAsyncMessage(
						serviceRegistryClient.getServiceConnectionInformation(UserMessageMessageTypes.SERVICE_NAME),
						requestJson);
			} catch (F4MValidationFailedException | F4MIOException e) {
				throw new F4MFatalErrorException("Unable to send sendEmail request to user message service", e);
			}
		}
	}

	private String calculateVoucherImageURL(Tombola tombola, String tenantId, String appId) {
		String uriString = EMPTY_IMAGE_URL;
		if (tenantId == null) {
			LOGGER.warn("tenantId is null.");
		} else if (appId == null) {
			LOGGER.warn("appId is null.");
		} else {
			AppConfig appConfig = applicationConfigurationAerospikeDao.getAppConfiguration(tenantId, appId);
			if(appConfig == null) {
				LOGGER.warn("AppConfig is null.");
			} else if(appConfig.getApplication() == null) {
				LOGGER.warn("AppConfig.application is null.");
			} else if(appConfig.getApplication().getConfiguration() == null) {
				LOGGER.warn("AppConfig.application.configuration is null.");
			} else if(appConfig.getApplication().getConfiguration().getCdnMedia() == null) {
				LOGGER.warn("AppConfig.application.configuration.cdnMedia is null.");
			} else {
				String baseUrl = appConfig.getApplication().getConfiguration().getCdnMedia();
				String rawImageFilename = tombola.getImageId();
				String image = calculateImageFilename(rawImageFilename);
				uriString = String.format("%s%s%s",baseUrl, CDN_MEDIA_TOMBOLA_NAMESPACE, image);
			}
		}
		return uriString;
	}

	private String calculateImageFilename(String rawImage) {
		String originaleBasename = FilenameUtils.getBaseName(rawImage);
		String originalExtension = FilenameUtils.getExtension(rawImage);
		String newBasename = String.format("%s%s", originaleBasename, IMAGE_RESOLUTION_MEDIUM);
		return String.format("%s%s%s",newBasename, FilenameUtils.EXTENSION_SEPARATOR_STR, originalExtension);
	}

	public void requestUserVoucherRelease(JsonMessage<? extends JsonMessageContent> sourceMessage,
			SessionWrapper session, String voucherId) {
		final UserVoucherReleaseRequest request = new UserVoucherReleaseRequest(voucherId);
		final JsonMessage<UserVoucherReleaseRequest> message = jsonUtil.createNewMessage(
				VoucherMessageTypes.USER_VOUCHER_RELEASE, request);
		final RequestInfo requestInfo = new RequestInfoImpl();
		requestInfo.setSourceMessage(sourceMessage);
		requestInfo.setSourceSession(session);
		try {
			final ServiceConnectionInformation connInfo = serviceRegistryClient.getServiceConnectionInformation(
					VoucherMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(connInfo, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send userVoucherRelease request to voucher service", e);
		}
	}

	public void requestTombolaDraw(JsonMessage<? extends JsonMessageContent> sourceMessage, SessionWrapper session,
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
		requestInfo.setSourceSession(session);
		try {
			final ServiceConnectionInformation connInfo = serviceRegistryClient.getServiceConnectionInformation(
					EventMessageTypes.SERVICE_NAME);
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(connInfo, message, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send tombola draw event notification to event service", e);
		}
	}
}
