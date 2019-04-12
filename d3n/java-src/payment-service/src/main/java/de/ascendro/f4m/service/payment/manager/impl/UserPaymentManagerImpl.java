package de.ascendro.f4m.service.payment.manager.impl;

import com.google.gson.Gson;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.auth.model.register.SetUserRoleRequest;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.model.user.UserRole;
import de.ascendro.f4m.service.payment.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.dao.PendingIdentification;
import de.ascendro.f4m.service.payment.dao.PendingIdentificationAerospikeDao;
import de.ascendro.f4m.service.payment.manager.*;
import de.ascendro.f4m.service.payment.model.*;
import de.ascendro.f4m.service.payment.model.external.*;
import de.ascendro.f4m.service.payment.model.internal.PaymentDetails;
import de.ascendro.f4m.service.payment.model.internal.TransactionInfo;
import de.ascendro.f4m.service.payment.rest.model.*;
import de.ascendro.f4m.service.payment.rest.wrapper.*;
import de.ascendro.f4m.service.payment.server.PaymentErrorCallback;
import de.ascendro.f4m.service.payment.server.PaymentSuccessCallback;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.F4MBeanUtils;
import de.ascendro.f4m.service.util.auth.UserRoleUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;
import java.math.BigDecimal;
import java.net.URI;
import java.time.ZonedDateTime;

/**
 * For managing Payment API call, where user interaction is necessary after forwarding user to payment system. 
 */
public class UserPaymentManagerImpl implements UserPaymentManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserPaymentManagerImpl.class);


	private static final String DEFAULT_USER_COUNTRY = "DE";
	private static final String IDENTIFICATION_FORWARD_URI_PATH = "identification";
	private static final String IDENTIFICATION_FORWARD_URI_SUBPATH = "index";
	private static final String TRANSACTION_FORWARD_URI_PATH = "payment";
	
    private RestWrapperFactory<IdentificationRestWrapper> identificationRestWrapperFactory;
    private TransactionLogAerospikeDao transactionLogAerospikeDao;
    private RestWrapperFactory<PaymentTransactionRestWrapper> paymentTransactionRestWrapperFactory;
    private RestWrapperFactory<UserRestWrapper> userRestWrapperFactory;
	private PaymentConfig paymentConfig;
	private UserAccountManager userAccountManager;
	private TransactionLogCacheManager transactionLogCache;
	private DependencyServicesCommunicator dependencyServicesCommunicator;
	private CurrencyManager currencyManager;
	private Gson gson;
	private PaymentSuccessCallback paymentSuccessCallback;
	private PaymentErrorCallback paymentErrorCallback;
	private PendingIdentificationAerospikeDao pendingIdentificationAerospikeDao;

	@Inject
	public UserPaymentManagerImpl(PaymentConfig paymentConfig,
			RestWrapperFactory<IdentificationRestWrapper> identificationRestWrapperFactory,
			RestWrapperFactory<PaymentTransactionRestWrapper> paymentTransactionRestWrapperFactory,
			RestWrapperFactory<UserRestWrapper> userRestWrapperFactory,
			UserAccountManager userAccountManager,
			TransactionLogAerospikeDao transactionLogAerospikeDao,
			TransactionLogCacheManager transactionLogCache,
			DependencyServicesCommunicator dependencyServicesCommunicator,
			CurrencyManager currencyManager,
			GsonProvider gsonProvider,
			PaymentSuccessCallback paymentSuccessCallback, 
			PaymentErrorCallback paymentErrorCallback,
		    PendingIdentificationAerospikeDao pendingIdentificationAerospikeDao) {
		this.paymentConfig = paymentConfig;
		this.identificationRestWrapperFactory = identificationRestWrapperFactory;
		this.paymentTransactionRestWrapperFactory = paymentTransactionRestWrapperFactory;
		this.userRestWrapperFactory = userRestWrapperFactory;
		this.userAccountManager = userAccountManager;
		this.transactionLogAerospikeDao = transactionLogAerospikeDao;
		this.transactionLogCache = transactionLogCache;
		this.dependencyServicesCommunicator = dependencyServicesCommunicator;
		this.currencyManager = currencyManager;
		this.paymentSuccessCallback = paymentSuccessCallback;
		this.paymentErrorCallback = paymentErrorCallback;
		this.pendingIdentificationAerospikeDao = pendingIdentificationAerospikeDao;
		gson = gsonProvider.get();
	}

    @Override
	public InitIdentificationResponse initiateIdentification(PaymentClientInfo paymentClientInfo, InitIdentificationRequest request) {
        IdentificationInitializationRest initRequest = new IdentificationInitializationRest();
        initRequest.setUserId(PaymentUserIdCalculator.calcPaymentUserId(paymentClientInfo.getTenantId(), paymentClientInfo.getProfileId()));
        initRequest.setMethod(request.getMethod());
		initRequest.setCallbackUrlSuccess(paymentConfig
				.getExternalCallbackForContext(PaymentConfig.IDENTIFICATION_SUCCESS_CONTEXT_PATH).toString());
		initRequest.setCallbackUrlError(paymentConfig
				.getExternalCallbackForContext(PaymentConfig.IDENTIFICATION_ERROR_CONTEXT_PATH).toString());
        IdentificationRestWrapper restWrapper = identificationRestWrapperFactory.create(paymentClientInfo.getTenantId());
		IdentificationResponseRest identificationResponse = restWrapper.startUserIdentificationToGetForwardURL(initRequest);
        InitIdentificationResponse response = new InitIdentificationResponse();
		response.setForwardUrl(buildForwardURI(restWrapper, IDENTIFICATION_FORWARD_URI_PATH,
				IDENTIFICATION_FORWARD_URI_SUBPATH, identificationResponse.getId()).toString());
        response.setIdentificationId(identificationResponse.getId());
		PendingIdentification identification = new PendingIdentification(paymentClientInfo.getTenantId(), paymentClientInfo.getAppId(), paymentClientInfo.getProfileId());
		pendingIdentificationAerospikeDao.createPendingIdentification(identification);
        return response;
    }
    
	protected URI buildForwardURI(RestWrapper restWrapper, String... paths) {
		String webBaseURL = restWrapper.getForwardUriBase();
		UriBuilder builder = UriBuilder.fromUri(webBaseURL);
		for (int i = 0; i < paths.length; i++) {
			builder.path(paths[i]);
		}
		return builder.build();
	}

	@Override
	public InitExternalPaymentResponse initiateExternalPayment(PaymentClientInfo paymentClientInfo, InitExternalPaymentRequest request) {
		PaymentTransactionInitializationRest initializationRequest = new PaymentTransactionInitializationRest();
		initializationRequest.setCallbackUrlSuccess(
				paymentConfig.getExternalCallbackForContext(PaymentConfig.PAYMENT_SUCCESS_CONTEXT_PATH).toString());
		initializationRequest.setCallbackUrlError(
				paymentConfig.getExternalCallbackForContext(PaymentConfig.PAYMENT_ERROR_CONTEXT_PATH).toString());
		initializationRequest.setRedirectUrlSuccess(request.getRedirectUrlSuccess());
		initializationRequest.setRedirectUrlError(request.getRedirectUrlError());
		initializationRequest.setAccountId(userAccountManager.findAccountIdFromAllAccounts(paymentClientInfo.getTenantId(),
				paymentClientInfo.getProfileId(), request.getCurrency()));
		BigDecimal amount = request.getAmount();
		boolean payIn;
		if (amount.signum() > 0) {
			LOGGER.error("initiateExternalPayment 1");
			payIn = true;
			initializationRequest.setType(PaymentTransactionType.CREDIT);
			initializationRequest.setValue(amount);
			if (request.getCashoutData() != null) {
				throw new F4MValidationFailedException("Cashout data can be specified only for pay-out");
			}
		} else if (amount.signum() < 0) {
			LOGGER.error("initiateExternalPayment 2");
			payIn = false;
			initializationRequest.setType(PaymentTransactionType.DEBIT);
			initializationRequest.setValue(amount.negate());
			if (request.getCashoutData() != null) {
				CashoutDataRequest cashoutData = new CashoutDataRequest();
				cashoutData.setBeneficiary(request.getCashoutData().getBeneficiary());
				cashoutData.setIban(request.getCashoutData().getIban());
				cashoutData.setBic(request.getCashoutData().getBic());
				initializationRequest.setCashoutData(cashoutData);
			} else {
				throw new F4MValidationFailedException("Cashout data must be specified for pay-out");
			}
		} else {
			throw new F4MValidationFailedException("Incorrect amount for transaction " + amount.toPlainString());
		}
		
		ExchangeRate exchangeRate = null;
		if (!Currency.MONEY.equals(request.getCurrency())) {
			exchangeRate = currencyManager.getTenantExchangeRateByFromAmount(paymentClientInfo.getTenantId(), amount,
					Currency.MONEY, request.getCurrency());
			initializationRequest.setRate(currencyManager.getRate(exchangeRate));
		}

		PaymentDetails details = new PaymentDetailsBuilder().additionalInfo(request.getDescription()).build();
//		initializationRequest.setReference(PaymentManagerImpl.prepareReferenceFromDetails(details, gson));
		initializationRequest.setReference(getPriceLabel(paymentClientInfo));
		String logId = transactionLogAerospikeDao.createTransactionLog(createTransactionLog(paymentClientInfo, request, exchangeRate));
		PaymentTransactionRestWrapper transactionRestWrapper = paymentTransactionRestWrapperFactory.create(paymentClientInfo.getTenantId());
		LOGGER.error("initiateExternalPayment {} ", logId);
		try {
			PaymentTransactionRest transactionRest = transactionRestWrapper.startPaymentTransactionToGetForwardURL(initializationRequest);
			transactionLogAerospikeDao.updateTransactionLog(logId, transactionRest.getId(), TransactionStatus.PROCESSING);
			transactionLogCache.put(transactionRest.getId(), logId);
			InitExternalPaymentResponse response = new InitExternalPaymentResponse();
			response.setTransactionId(transactionRest.getId());
			response.setPaymentToken(transactionRest.getPaymentToken());
			if (payIn) {
				response.setForwardUrl(buildForwardURI(transactionRestWrapper, TRANSACTION_FORWARD_URI_PATH, transactionRest.getId()).toString());
			} else {
				paymentSuccessCallback.onPaymentSuccess(transactionRest.getId(), false);
			}
			return response;
		} catch (Exception e) {
			if (!payIn) {
				paymentErrorCallback.onPaymentError(logId, null);
			}
			return ExceptionUtils.wrapAndThrow(e);
		}
	}

	private TransactionLog createTransactionLog(PaymentClientInfo paymentClientInfo, InitExternalPaymentRequest request, ExchangeRate exchangeRate) {
		TransactionInfo transactionInfoForLog = new TransactionInfo(paymentClientInfo.getTenantId(), paymentClientInfo.getProfileId(),
				paymentClientInfo.getProfileId(), request.getAmount());
		TransactionLog transactionLog =  new TransactionLog(transactionInfoForLog, Currency.MONEY , request.getDescription(), paymentClientInfo.getAppId());
		transactionLog.setCurrencyTo(request.getCurrency());
		if (exchangeRate!=null) {
			transactionLog.setAmountTo(exchangeRate.getToAmount());
		}
		return transactionLog;
	}

	@Override
	public GetIdentificationResponse getIdentification(PaymentClientInfo paymentClientInfo) {
		UserRestWrapper userRestWrapper = userRestWrapperFactory.create(paymentClientInfo.getTenantId());
		String userId = PaymentUserIdCalculator.calcPaymentUserId(paymentClientInfo.getTenantId(), paymentClientInfo.getProfileId());
		IdentityRest restResponse = userRestWrapper.getUserIdentity(userId);
		GetIdentificationResponse f4mResponse = new GetIdentificationResponse();
		F4MBeanUtils.copyProperties(f4mResponse, restResponse);
		return f4mResponse;
	}

	@Override
	public GetExternalPaymentResponse getExternalPayment(PaymentClientInfo paymentClientInfo, GetExternalPaymentRequest request) {
		PaymentTransactionRestWrapper transactionRestWrapper = paymentTransactionRestWrapperFactory.create(paymentClientInfo.getTenantId());
		PaymentTransactionRest transactionRest = transactionRestWrapper.getPaymentTransaction(request.getTransactionId());
		GetExternalPaymentResponse response = new GetExternalPaymentResponse();
		F4MBeanUtils.copyProperties(response, transactionRest);
		response.setAmount(transactionRest.getValue());
		response.setDescription(transactionRest.getReference());
		response.setTransactionId(transactionRest.getId());
		return response;
	}

	@Override
	public void synchronizeIdentityInformation(String userId) {
		String tenantId = PaymentUserIdCalculator.calcTenantIdFromUserId(userId);
		String profileId = PaymentUserIdCalculator.calcProfileIdFromUserId(userId);
		UserRestWrapper userRestWrapper = userRestWrapperFactory.create(tenantId);
		IdentityRest identityRest = userRestWrapper.getUserIdentity(userId);
		dependencyServicesCommunicator.requestUpdateProfileIdentity(profileId, createProfileUpdateObject(identityRest));
		updateUserRole(profileId, tenantId, identityRest);
	}

	private void updateUserRole(String profileId, String tenantId, IdentityRest identityRest) {
		if (identityRest.getDateOfBirth() != null && isFullyRegisteredBankO18Role(identityRest.getDateOfBirth())) {
			SetUserRoleRequest setUserRoleRequest = UserRoleUtil.createSetUserRoleRequest(profileId,
					userAccountManager.getUserRoles(profileId, tenantId), UserRole.FULLY_REGISTERED_BANK_O18,
					UserRole.ANONYMOUS);
			if (setUserRoleRequest != null) {
				dependencyServicesCommunicator.updateUserRoles(setUserRoleRequest);
			}
		}
	}

	private boolean isFullyRegisteredBankO18Role(ZonedDateTime birthDate) {
		return birthDate.plusYears(18).isBefore(DateTimeUtil.getCurrentDateTime());
	}

	private Profile createProfileUpdateObject(IdentityRest identity) {
		final Profile profile = new Profile();
		ProfileUser profileUser = new ProfileUser();
		profileUser.setFirstName(identity.getFirstName());
		profileUser.setLastName(identity.getName());
		profileUser.setBirthDate(ZonedDateTime.ofInstant(identity.getDateOfBirth().toInstant(), DateTimeUtil.TIMEZONE));
		profile.setPersonWrapper(profileUser);

		ProfileAddress profileAddress = new ProfileAddress();
		profileAddress.setCity(identity.getCity());
		//FIXME: change back to use country from Paydent, when they implement returning value of ISO 3166-1 alpha2
		profileAddress.setCountry(DEFAULT_USER_COUNTRY); //identity.getCountry()); 
		profileAddress.setStreet(identity.getStreet());
		profileAddress.setPostalCode(identity.getZip());
		profile.setAddress(profileAddress);
		return profile;
	}

    private String getPriceLabel(PaymentClientInfo clientInfo){
        switch (clientInfo.getLanguage()) {
            case "de":
                return "Preis";
            case "pt":
                return "Preço";
            case "ru":
                return "Цена";
            default:
                return "Price";
        }
    }
}