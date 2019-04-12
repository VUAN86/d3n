package de.ascendro.f4m.service.voucher.client;

import java.math.BigDecimal;

import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.server.game.instance.CommonGameInstanceAerospikeDao;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.model.AppConfig;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentDetailsBuilder;
import de.ascendro.f4m.service.payment.model.TransactionLog;
import de.ascendro.f4m.service.payment.model.TransferFundsRequest;
import de.ascendro.f4m.service.payment.model.TransferFundsRequestBuilder;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.usermessage.translation.Messages;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import de.ascendro.f4m.service.voucher.model.UserVoucher;
import de.ascendro.f4m.service.voucher.model.Voucher;
import de.ascendro.f4m.service.voucher.util.VoucherUtil;

/**
 * Class for sending user information requests to Profile Service
 */
public class DependencyServicesCommunicator {
	private static final Logger LOGGER = LoggerFactory.getLogger(DependencyServicesCommunicator.class);
	
	private static final String CDN_MEDIA_VOUCHER_NAMESPACE = "voucherBig/";
	private static final String IMAGE_RESOLUTION_MEDIUM = "_medium";
	private static final String EMPTY_IMAGE_URL = "#";
	private static final String GAME_PARAMETER_NOT_USED = "none";
	private static final String USING_VOUCHER_REASON = "Used voucher";
	
	private ServiceRegistryClient serviceRegistryClient;
	private JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	private JsonMessageUtil jsonUtil;
	private VoucherUtil voucherUtil;
	private TransactionLogAerospikeDao transactionLogAerospikeDao;
	private CommonGameInstanceAerospikeDao gameInstanceAerospikeDao;
	private ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao;

	@Inject
	public DependencyServicesCommunicator(ServiceRegistryClient serviceRegistryClient,
			JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, JsonMessageUtil jsonUtil,
			VoucherUtil voucherUtil, 
			TransactionLogAerospikeDao transactionLogAerospikeDao,
			CommonGameInstanceAerospikeDao gameInstanceAerospikeDao,
			ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao) {
		this.serviceRegistryClient = serviceRegistryClient;
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.jsonUtil = jsonUtil;
		this.voucherUtil = voucherUtil;
		this.transactionLogAerospikeDao = transactionLogAerospikeDao;
		this.gameInstanceAerospikeDao = gameInstanceAerospikeDao;
		this.applicationConfigurationAerospikeDao = applicationConfigurationAerospikeDao;
	}

	/**
	 * This method will initiate a sendEmail call on the user service.
	 */
	public void sendWonVoucherAssignedEmailToUser(String userId, UserVoucher userVoucher,
			Voucher voucher, String tenantId, String appId) {
		Game game = gameInstanceAerospikeDao.getGameByInstanceId(userVoucher.getGameInstanceId());
		String voucherWonAssignedToUserContentMsg = StringUtils.isNotEmpty(voucher.getRedemptionURL()) ?
				Messages.VOUCHER_WON_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL : Messages.VOUCHER_WON_ASSIGNED_TO_USER_CONTENT;
		sendVoucherAssignedEmailToUser(Messages.VOUCHER_WON_ASSIGNED_TO_USER_SUBJECT,
				voucherWonAssignedToUserContentMsg, userId, userVoucher, voucher, game.getTitle(), tenantId, appId);
	}

	/**
	 * This method will initiate a sendEmail call on the user service.
	 */
	public void sendWonVoucherInTombolaAssignedEmailToUser(String userId, UserVoucher userVoucher, Voucher voucher,
			String tombolaName, String tenantId, String appId) {
		String voucherWonInTombolaAssignedToUserContentMsg = StringUtils.isNotEmpty(voucher.getRedemptionURL()) ?
				Messages.VOUCHER_WON_IN_TOMBOLA_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL :
				Messages.VOUCHER_WON_ASSIGNED_TO_USER_CONTENT;
		sendVoucherAssignedEmailToUser(Messages.VOUCHER_WON_ASSIGNED_TO_USER_SUBJECT,
				voucherWonInTombolaAssignedToUserContentMsg, userId, userVoucher, voucher, tombolaName, tenantId, appId);
	}
	
	/**
	 * This method will initiate a sendEmail call on the user service.
	 */
	public void sendBoughtVoucherAssignedEmailToUser(String userId, UserVoucher userVoucher,
			Voucher voucher, String tenantId, String appId) {
		String voucherBoughtAssignedToUserContentMsg = StringUtils.isNotEmpty(voucher.getRedemptionURL()) ?
				Messages.VOUCHER_BOUGHT_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL :
				Messages.VOUCHER_BOUGHT_ASSIGNED_TO_USER_CONTENT;
		sendVoucherAssignedEmailToUser(Messages.VOUCHER_BOUGHT_ASSIGNED_TO_USER_SUBJECT,
				voucherBoughtAssignedToUserContentMsg, userId, userVoucher, voucher, GAME_PARAMETER_NOT_USED,
				tenantId, appId);
	}
	
	private void sendVoucherAssignedEmailToUser(String subject, String body, String userId, UserVoucher userVoucher,
			Voucher voucher, String sourceTitle, String tenantId, String appId) {
		//consider if clientInfo can be used instead of two parameters userId&appId
		String[] parameters = getVoucherEmailParameters(userVoucher, voucher, sourceTitle, tenantId, appId);
		sendEmailToUser(subject, body, userId, voucher, parameters);
	}

	private String[] getVoucherEmailParameters(UserVoucher userVoucher, Voucher voucher, String voucherSourceName,
			String tenantId, String appId) {
		String voucherCompany = voucher.getCompany();
		String expirationDate = DateTimeUtil
				.formatISODate(DateTimeUtil.parseISODateTimeString(voucher.getExpirationDate()).toLocalDate());
		String imageUrl = calculateVoucherImageURL(voucher, tenantId, appId);
		if (StringUtils.isNotEmpty(voucher.getRedemptionURL())) {
			return new String[]{ voucher.getTitle(), voucherSourceName, userVoucher.getCode(), voucherCompany, expirationDate,
					imageUrl, voucher.getRedemptionURL() };
		} else {
			return new String[]{ voucher.getTitle(), voucherSourceName, userVoucher.getCode(), voucherCompany, expirationDate,
					imageUrl};
		}
	}

	private void sendEmailToUser(String subject, String body, String userId, Voucher voucher, String[] parameters) {
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
					requestJson, new VoucherRequestInfo(voucher.getId()));
		} catch (F4MValidationFailedException | F4MIOException e) {
			throw new F4MFatalErrorException("Unable to send sendEmail request", e);
		}
	}

	private String calculateVoucherImageURL(Voucher voucher, String tenantId, String appId) {
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
				String rawImageFilename = voucher.getBigImageId();
				String image = calculateImageFilename(rawImageFilename);
				uriString = String.format("%s%s%s",baseUrl, CDN_MEDIA_VOUCHER_NAMESPACE, image);
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

	/**
	 * This method starts the payment request for the voucher :
	 */
	public void initiateUserVoucherPayment(JsonMessage<? extends JsonMessageContent> originalMessage,
										   SessionWrapper sessionWrapper, String voucherId,
										   BigDecimal amount, Currency currency) {
		final String userId = originalMessage.getClientInfo().getUserId();
		final String tenantId = originalMessage.getClientInfo().getTenantId();
		final String appId = originalMessage.getClientInfo().getAppId();

		TransferFundsRequestBuilder builder = new TransferFundsRequestBuilder()
				.fromProfileToTenant(userId, tenantId)
				.amount(amount, currency)
				.withPaymentDetails(new PaymentDetailsBuilder()
						.voucherId(voucherId)
						.appId(appId)
						.build());
		TransferFundsRequest transactionRequest = builder.buildSingleUserPaymentForGame();

		final JsonMessage<TransferFundsRequest> transactionRequestMessage = jsonUtil.createNewMessage(
				builder.getBuildSingleUserPaymentForGameRequestType(), transactionRequest);

		final VoucherRequestInfo requestInfo = new VoucherRequestInfo(voucherId);
		requestInfo.setSourceMessage(originalMessage);
		requestInfo.setSourceSession(sessionWrapper);

		String transactionLogId = transactionLogAerospikeDao
				.createTransactionLog(new TransactionLog(transactionRequest, currency, USING_VOUCHER_REASON, appId));
		requestInfo.setTransactionLogId(transactionLogId);
		try {
			jsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(
					serviceRegistryClient.getServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME),
					transactionRequestMessage, requestInfo);
		} catch (F4MValidationFailedException | F4MIOException e) {
			voucherUtil.releaseVoucher(requestInfo.getVoucherId());
			throw new F4MFatalErrorException("Unable to send transferTransaction request to payment service", e);
		}
	}

}
