package de.ascendro.f4m.service.payment.server;

import com.google.api.services.androidpublisher.model.ProductPurchase;
import com.google.gson.Gson;
import de.ascendro.f4m.server.market.Market;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.handler.JsonAuthenticationMessageMQHandler;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.MessageSource;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.payment.cache.AccountBalanceCache;
import de.ascendro.f4m.service.payment.callback.JsonMessageHandlerCallback;
import de.ascendro.f4m.service.payment.client.AdminEmailForwarder;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.exception.F4MPaymentClientException;
import de.ascendro.f4m.service.payment.manager.*;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.Ios;
import de.ascendro.f4m.service.payment.model.IosInApp;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.config.InsertOrUpdateUserRequest;
import de.ascendro.f4m.service.payment.model.config.MergeUsersRequest;
import de.ascendro.f4m.service.payment.model.external.*;
import de.ascendro.f4m.service.payment.model.internal.*;
import de.ascendro.f4m.service.payment.session.PaymentClientInfoImpl;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
public class PaymentServiceServerMessageHandler extends JsonAuthenticationMessageMQHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentServiceServerMessageHandler.class);

	private PaymentManager paymentManager;
	private UserPaymentManager userPaymentManager;
	private UserAccountManager userAccountManager;
	private GameManager gameManager;
	private AccountBalanceCache accountBalanceCache;
	private AdminEmailForwarder adminEmailForwarder;
	private final SimpleDateFormat sdfToDate = new SimpleDateFormat("yyyyMMdd");
	private final SimpleDateFormat sdfToDateTime = new SimpleDateFormat("yyyyMMdd HH:mm:ss");


	public PaymentServiceServerMessageHandler(PaymentManager paymentManager, UserPaymentManager userPaymentManager,
											  UserAccountManager userAccountManager, GameManager gameManager, AccountBalanceCache accountBalanceCache,
											  AdminEmailForwarder adminEmailForwarder) {
		this.paymentManager = paymentManager;
		this.userPaymentManager = userPaymentManager;
		this.userAccountManager = userAccountManager;
		this.gameManager = gameManager;
		this.accountBalanceCache = accountBalanceCache;
		this.adminEmailForwarder = adminEmailForwarder;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JsonMessageContent onUserMessage(RequestContext ctx) {
		JsonMessage<? extends JsonMessageContent> message = ctx.getMessage();

		final JsonMessageContent resultContent;
		final PaymentMessageTypes type = message.getType(PaymentMessageTypes.class);
		if (type != null) {
			switch (type) {
			case INIT_EXTERNAL_PAYMENT:
				resultContent = userPaymentManager.initiateExternalPayment(getClientInfo(message),
						((JsonMessage<InitExternalPaymentRequest>) message).getContent());
				break;
			case GET_EXTERNAL_PAYMENT:
				resultContent = userPaymentManager.getExternalPayment(getClientInfo(message),
						((JsonMessage<GetExternalPaymentRequest>) message).getContent());
				break;
			case INIT_IDENTIFICATION:
				resultContent = userPaymentManager.initiateIdentification(getClientInfo(message),
						((JsonMessage<InitIdentificationRequest>) message).getContent());
				break;
			case INSERT_OR_UPDATE_USER:
				resultContent = null;//
				userAccountManager.insertOrUpdateUser(((JsonMessage<InsertOrUpdateUserRequest>) message).getContent(),
						new JsonMessageHandlerCallback<EmptyJsonMessageContent>(this, ctx));
				break;
			case MERGE_USERS:
				paymentManager.mergeUserAccounts(((JsonMessage<MergeUsersRequest>) message).getContent());
				resultContent = new EmptyJsonMessageContent();
				break;
			case CONVERT_BETWEEN_CURRENCIES:
				resultContent = null; 
				paymentManager.convertBetweenCurrencies(getClientInfo(message),
						((JsonMessage<ConvertBetweenCurrenciesUserRequest>) message).getContent(),
						new JsonMessageHandlerCallback<ConvertBetweenCurrenciesUserResponse>(this, ctx));
				break;
			case GET_ACCOUNT_BALANCE:
				resultContent = getAccountBalance(message, message.getClientInfo());
				break;
			case GET_USER_ACCOUNT_BALANCES:
				resultContent = accountBalanceCache.getAccountBalances(getClientInfo(message));
				break;
			case GET_ACCOUNT_HISTORY:
				GetAccountHistoryRequest historyRequest = ((JsonMessage<GetAccountHistoryRequest>) message).getContent();
				PaymentClientInfoImpl clientInfo = getClientInfo(message);
				clientInfo.setProfileId(historyRequest.getProfileId());
				resultContent = paymentManager.getAccountHistory(clientInfo, historyRequest);
				break;
			case GET_USER_ACCOUNT_HISTORY:
				resultContent = paymentManager.getAccountHistory(getClientInfo(message),
						((JsonMessage<GetUserAccountHistoryRequest>) message).getContent());
				break;
			case GET_TRANSACTION:
				resultContent = null;//
				paymentManager.getTransaction(((JsonMessage<GetTransactionRequest>) message).getContent(),
						new JsonMessageHandlerCallback<GetTransactionResponse>(this, ctx));
				break;
			case TRANSFER_BETWEEN_ACCOUNTS:
				resultContent = null; 
				paymentManager.transferBetweenAccounts(
						((JsonMessage<TransferBetweenAccountsRequest>) message).getContent(),
						new JsonMessageHandlerCallback<TransactionId>(this, ctx), message.getClientInfo());
				break;
			case GET_IDENTIFICATION:
				resultContent = userPaymentManager.getIdentification(getClientInfo(message));
				break;
			case LOAD_OR_WITHDRAW_WITHOUT_COVERAGE:
				resultContent = null; 
				paymentManager.loadOrWithdrawWithoutCoverage(
						((JsonMessage<LoadOrWithdrawWithoutCoverageRequest>) message).getContent(),
						new JsonMessageHandlerCallback<TransactionId>(this, ctx), message.getClientInfo());
				break;
			case GET_EXCHANGE_RATES:
				resultContent = paymentManager.getExchangeRates(getClientInfo(message));
				break;
			case TRANSFER_JACKPOT:
				resultContent = gameManager
						.transferJackpot(((JsonMessage<TransferJackpotRequest>) message).getContent(), message.getClientInfo());
				break;
			case CREATE_JACKPOT:
				gameManager.createJackpot(((JsonMessage<CreateJackpotRequest>) message).getContent());
				resultContent = new EmptyJsonMessageContent();
				break;
			case GET_JACKPOT:
				resultContent = gameManager
						.getJackpot(((JsonMessage<GetJackpotRequest>) message).getContent());
				break;
			case CLOSE_JACKPOT:
				resultContent = gameManager.closeJackpot(this.isAdmin(message.getClientInfo()), ((JsonMessage<CloseJackpotRequest>) message).getContent());
				break;
			case MOBILE_PURCHASE:
				resultContent = checkPurchase(message, message.getClientInfo());
				break;
			default:
				throw new F4MValidationFailedException(
						"Payment Service message type[" + message.getName() + "] not recognized");
			}
		} else {
			throw new F4MValidationFailedException("Unrecognized message " + message.getName());
		}
		return resultContent;
	}

	private JsonMessageContent checkPurchase(JsonMessage<? extends JsonMessageContent> message, ClientInfo clientInfo) {
		@SuppressWarnings("unchecked")
		MobilePurchaseRequest request = ((JsonMessage<MobilePurchaseRequest>) message).getContent();
		Gson gson = new Gson();
		MobilePurchaseResponse response;
		String date = sdfToDate.format(Calendar.getInstance().getTime());
		PaymentConfig paymentConfig=adminEmailForwarder.getConfig();
		String status = null;
		switch (request.getDevice()) {
			case IOS:
				try {
					IosPurchase iosPurchase = gson.fromJson(request.getReceipt(), IosPurchase.class);
					Ios ios =IosPurchaseValidate.getReceiptInfo(iosPurchase,paymentConfig);
					status = String.valueOf(ios.getStatus());
					List<IosInApp> iosInAppList = ios.getReceipt().getIn_app();
					String transactionId = null;
					transactionId = iosInAppList.get(0).getTransaction_id();
					if (adminEmailForwarder.isPurchaseCreditInstance(request.getDevice().getFullName(), transactionId)) {
						adminEmailForwarder.createPurchaseCreditInstanceIos(request.getDevice().getFullName(), transactionId, clientInfo.getUserId(),
								clientInfo.getTenantId(), clientInfo.getAppId(), date, iosInAppList.get(0).getProduct_id(),
								sdfToDateTime.format(Calendar.getInstance().getTime()), request.getReceipt());
						adminEmailForwarder.transferFundsToUserAccount(clientInfo.getTenantId(), clientInfo.getUserId(),
								Market.valueOf(iosInAppList.get(0).getProduct_id().toUpperCase()).getValue(), Currency.CREDIT, clientInfo.getAppId());
						response = new MobilePurchaseResponse("SUCCESS");
					} else {
						adminEmailForwarder.createPurchaseCreditInstanceIos(request.getDevice().getFullName() + ":" + message.getTimestamp(), transactionId, clientInfo.getUserId(),
								clientInfo.getTenantId(), clientInfo.getAppId(), date, iosInAppList.get(0).getProduct_id(),
								sdfToDateTime.format(Calendar.getInstance().getTime()), request.getReceipt());
						response = new MobilePurchaseResponse("PURCHASED");
					}
				} catch (Exception e) {
                    LOGGER.error("PaymentServiceServerMessageHandler IOS: {} message {}", e.getMessage(), message);
					adminEmailForwarder.createPurchaseCreditInstanceError(request.getDevice().getFullName() + ":" + message.getTimestamp(),
							clientInfo.getUserId(), clientInfo.getTenantId(), clientInfo.getAppId(), request.getReceipt(), date, message.getTimestamp(), e.getMessage());
					throw new F4MPaymentClientException("ERROR_INVALID_IOS_RECEIPT : "+e.getMessage(), e.getMessage());
				}
				break;
			case ANDROID:
				try {
					AndroidPurchaseValidate androidPurchaseValidate = new AndroidPurchaseValidate();
					AndroidPurchase androidPurchase = gson.fromJson(request.getReceipt(), AndroidPurchase.class);
					ProductPurchase productPurchase = androidPurchaseValidate.getReceiptInfo(androidPurchase,paymentConfig);
					if (adminEmailForwarder.isPurchaseCreditInstance(request.getDevice().getFullName(), productPurchase.getOrderId())) {
						adminEmailForwarder.createPurchaseCreditInstanceAndroid(request.getDevice().getFullName(), productPurchase.getOrderId(), clientInfo.getUserId(),
								clientInfo.getTenantId(), clientInfo.getAppId(),
								date, androidPurchase.getPackageName(), androidPurchase.getProductId(), androidPurchase.getPurchaseTime(),
								androidPurchase.getPurchaseState(), androidPurchase.getPurchaseToken());
						adminEmailForwarder.transferFundsToUserAccount(clientInfo.getTenantId(), clientInfo.getUserId(),
								Market.valueOf(androidPurchase.getProductId().toUpperCase()).getValue(), Currency.CREDIT, clientInfo.getAppId());
						response = new MobilePurchaseResponse("SUCCESS");
					} else {
						adminEmailForwarder.createPurchaseCreditInstanceAndroid(request.getDevice().getFullName() + ":" + message.getTimestamp(), productPurchase.getOrderId(),
								clientInfo.getUserId(), clientInfo.getTenantId(), clientInfo.getAppId(),
								date, androidPurchase.getPackageName(), androidPurchase.getProductId(), androidPurchase.getPurchaseTime(),
								androidPurchase.getPurchaseState(), androidPurchase.getPurchaseToken());
						response = new MobilePurchaseResponse("PURCHASED");
					}
				} catch (Exception e) {
					LOGGER.error("PaymentServiceServerMessageHandler ANDROID: {} message {}", e.getMessage(), message);
					adminEmailForwarder.createPurchaseCreditInstanceError(request.getDevice().getFullName() + ":" + message.getTimestamp(), clientInfo.getUserId(),
							clientInfo.getTenantId(), clientInfo.getAppId(), request.getReceipt(), date, message.getTimestamp(), e.getMessage());
					throw new F4MPaymentClientException("ERROR_INVALID_ANDROID_RECEIPT : "+e.getMessage(), e.getMessage());
				}
					break;
			default:
				response = new MobilePurchaseResponse("NOT_FOUND_DEVICE");
				break;
		}
		return response;
	}

	private JsonMessageContent getAccountBalance(JsonMessage<? extends JsonMessageContent> message, ClientInfo clientInfo) {
		final JsonMessageContent resultContent;
		@SuppressWarnings("unchecked")
		GetAccountBalanceRequest balanceRequest = ((JsonMessage<GetAccountBalanceRequest>) message).getContent();
		if (StringUtils.isNotEmpty(balanceRequest.getProfileId())) {
			String appId = (clientInfo != null) ? clientInfo.getAppId() : null;
			resultContent = accountBalanceCache
					.getAccountBalance(calculateTenantForPaymentClientInfo(message, balanceRequest, appId), balanceRequest);
		} else {
			if (message.getClientInfo() == null || isAdmin(clientInfo)) {//tenant balance available only for internal services and admins of this tenant
				resultContent = userAccountManager.getTenantAccountBalance(balanceRequest.getTenantId());
			} else {
				throw new F4MInsufficientRightsException("ProfileId is mandatory");
			}
		}
		return resultContent;
	}

	private PaymentClientInfoImpl getClientInfo(JsonMessage<?> message) {
		ClientInfo clientInfo = message.getClientInfo();
		if (clientInfo != null) {
			return new PaymentClientInfoImpl(clientInfo.getTenantId(), clientInfo.getUserId(), clientInfo.getAppId(), clientInfo.getLanguage());
		} else {
			throw new F4MInsufficientRightsException("No user information");
		}
	}

	private PaymentClientInfoImpl calculateTenantForPaymentClientInfo(JsonMessage<?> message, GetAccountBalanceRequest balanceRequest, String appId) {
		String tenantId;
		ClientInfo clientInfo = message.getClientInfo();
		if (clientInfo != null) {
			//for users always use tenantId from clientInfo. Same for internal service calls in users context call
			tenantId = clientInfo.getTenantId();
		} else {
			//for service internal calls use tenantId from the message
			tenantId = balanceRequest.getTenantId();
		}
		return new PaymentClientInfoImpl(tenantId, balanceRequest.getProfileId(), appId);
	}

	@Override
	public void onFailure(String originalMessageEncoded, RequestContext requestContext, MessageSource messageSource, Throwable e) {
		super.onFailure(originalMessageEncoded, requestContext, messageSource, e);
		if (adminEmailForwarder.shouldForwardErrorToAdmin(e)) {
			adminEmailForwarder.forwardErrorToAdmin(requestContext.getMessage(), e, null);
		}
	}
}