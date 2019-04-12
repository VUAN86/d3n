package de.ascendro.f4m.service.payment.manager.impl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.ascendro.f4m.server.analytics.model.InvoiceEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.F4MPaymentException;
import de.ascendro.f4m.service.payment.callback.Callback;
import de.ascendro.f4m.service.payment.callback.ManagerCallback;
import de.ascendro.f4m.service.payment.client.AdminEmailForwarder;
import de.ascendro.f4m.service.payment.manager.*;
import de.ascendro.f4m.service.payment.model.*;
import de.ascendro.f4m.service.payment.model.config.MergeUsersRequest;
import de.ascendro.f4m.service.payment.model.external.ConvertBetweenCurrenciesUserRequest;
import de.ascendro.f4m.service.payment.model.external.ConvertBetweenCurrenciesUserResponse;
import de.ascendro.f4m.service.payment.model.external.ExchangeRate;
import de.ascendro.f4m.service.payment.model.external.GetExchangeRatesResponse;
import de.ascendro.f4m.service.payment.model.internal.*;
import de.ascendro.f4m.service.payment.payment.system.manager.AccountBalanceManager;
import de.ascendro.f4m.service.payment.payment.system.manager.GameBalanceManager;
import de.ascendro.f4m.service.payment.rest.model.*;
import de.ascendro.f4m.service.payment.rest.wrapper.*;
import de.ascendro.f4m.service.util.F4MBeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

public class PaymentManagerImpl implements PaymentManager {
	private static final ZoneOffset TIMEZONE = ZoneOffset.UTC;
	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentManagerImpl.class);
	
	private CurrencyManager currencyManager;
	private AnalyticsEventManager analyticsEventManager;
	private UserAccountManager userAccountManager;
	private RestWrapperFactory<UserRestWrapper> userRestWrapperFactory;
	private RestWrapperFactory<AccountRestWrapper> accountRestWrapperFactory;
	private RestWrapperFactory<TransactionRestWrapper> transactionRestWrapperFactory;
	private TransactionLogAerospikeDao transactionLogAerospikeDao;
	private final AdminEmailForwarder adminEmailForwarder;
	private final Tracker tracker;
	private Gson gson;

	/**
	 * Utility method for creating reference field to be sent to payment system from payment details.
	 * @param details
	 * @param gson
	 * @return
	 */
	public static String prepareReferenceFromDetails(PaymentDetails details, Gson gson) {
		return gson.toJson(details);
	}


	@Inject
	private AccountBalanceManager accountBalanceManager;

	@Inject
	private GameBalanceManager gameBalanceManager;

	@Inject
	public PaymentManagerImpl(CurrencyManager currencyManager, UserAccountManager userAccountManager,
							  RestWrapperFactory<UserRestWrapper> userRestWrapperFactory, RestWrapperFactory<AccountRestWrapper> accountRestWrapperFactory,
							  RestWrapperFactory<TransactionRestWrapper> transactionRestWrapperFactory, TransactionLogAerospikeDao transactionLogAerospikeDao,
							  GsonProvider gsonProvider, AnalyticsEventManager analyticsEventManager, AdminEmailForwarder adminEmailForwarder, Tracker tracker)
	{
		this.currencyManager = currencyManager;
		this.userAccountManager = userAccountManager;
		this.userRestWrapperFactory = userRestWrapperFactory;
		this.accountRestWrapperFactory = accountRestWrapperFactory;
		this.transactionRestWrapperFactory = transactionRestWrapperFactory;
		this.transactionLogAerospikeDao = transactionLogAerospikeDao;
		this.analyticsEventManager = analyticsEventManager;
		this.adminEmailForwarder = adminEmailForwarder;
		this.tracker = tracker;
		gson = gsonProvider.get();
	}

	@Override
	public GetTransactionResponse getTransaction(GetTransactionRequest request, Callback<GetTransactionResponse> originalCallback) {
//		ManagerCallback<TransactionRest, ? extends JsonMessageContent> callback = new ManagerCallback<>(
//				originalCallback, transaction -> convertRestToF4MTransaction(transaction, loadOrWithdrawWithoutCoverage.getTenantId()));
//		transactionRestWrapperFactory.create(loadOrWithdrawWithoutCoverage.getTenantId())
//			.getTransaction(loadOrWithdrawWithoutCoverage.getTransactionId(), callback);

		return accountBalanceManager.getExternalPayment(request);
	}

	private GetTransactionResponse convertRestToF4MTransaction(TransactionRest transaction, String originalTenantId) {
		GetTransactionResponse response = new GetTransactionResponse();
		response.setId(transaction.getId());
		response.setAmount(transaction.getValue());
		response.setCreationDate(transaction.getCreated());
		try {
			PaymentDetails details = gson.fromJson(transaction.getReference(), PaymentDetails.class);
			if (details == null) {
				details = new PaymentDetails();
			}
			response.setPaymentDetails(details);
		} catch (JsonSyntaxException e) {
			LOGGER.warn(
					"Could not parse payment details, fallback to return all data in paymentDetails.additionalInfo - error was {}, reference '{}', transation {}",
					e, transaction.getReference(), transaction);
			response.setPaymentDetails(new PaymentDetailsBuilder().additionalInfo(transaction.getReference()).build());
		}
		if (transaction.getType() != null) {
			response.setType(transaction.getType().toString());
		}
		updateFromToInfo(true, transaction.getDebitorAccount(), response, originalTenantId);
		updateFromToInfo(false, transaction.getCreditorAccount(), response, originalTenantId);
		return response;
	}
	
	private void updateFromToInfo(boolean updateFromData, AccountRest account, GetTransactionResponse response,
			String originalTenantId) {
		if (account != null) {
			String userId = account.getUserId();
			if (currencyManager.isTenantMoneyAccount(originalTenantId, account)) {
				LOGGER.trace("Account {} is tenant money account", account);
			} else if (PaymentUserIdCalculator.isPaymentUserIdWithProfileId(userId)
					|| currencyManager.isInitializedTenant(userId)) {
				String tenantId = PaymentUserIdCalculator.calcTenantIdFromUserId(userId);
				LOGGER.debug("updateFromToInfo tenantId={} account={}",tenantId,account);
				response.setTenantId(tenantId);
				String profileId = PaymentUserIdCalculator.calcProfileIdFromUserId(userId);
				Currency currency = currencyManager.getCurrencyEnumByCurrencyRest(tenantId, account.getCurrency());
				if (updateFromData) {
					response.setFromProfileId(profileId);
					response.setFromCurrency(currency);
				} else {
					response.setToProfileId(profileId);
					response.setToCurrency(currency);
				}
			} else {
				if (Optional.ofNullable(response.getPaymentDetails()).map(PaymentDetails::getMultiplayerGameInstanceId)
						.filter(userId::equals).isPresent()) {
					LOGGER.trace("Account {} is game account", account);
				} else {
					LOGGER.warn("Unrecognized '{}' account {}", updateFromData ? "from" : "to", account);
				}
			}
		}
		
	}
	
	@Override
	public GetAccountHistoryResponse getAccountHistory(PaymentClientInfo paymentClientInfo, GetUserAccountHistoryRequest request) {
//		LOGGER.debug("getAccountHistory request {} ", request);
//		AccountHistoryRestSearchParams search = new AccountHistoryRestSearchParams();
//		F4MBeanUtils.copyProperties(search, request);
//		search.setAccountId(paymentClientInfo.getProfileId());
//		LOGGER.debug("getAccountHistory search {} ", search);
//		AccountHistoryRest accountHistory = accountRestWrapperFactory.create(paymentClientInfo.getTenantId()).getAccountHistory(search);
//
//		GetAccountHistoryResponse response = new GetAccountHistoryResponse();
//
//		F4MBeanUtils.copyProperties(response, accountHistory);
//
//		response.setItems(new ArrayList<>(accountHistory.getData().size()));

//		LOGGER.debug("getAccountHistory {} ", accountHistory.getData());
//		LOGGER.debug("getAccountHistory 2  {} ", search);
//		for (TransactionRest transactionRest : accountHistory.getData()) {
//			if (StringUtils.isNumeric(PaymentUserIdCalculator.calcTenantIdFromUserId(transactionRest.getCreditorAccount().getUserId())) &&
//					(transactionRest.getDebitorAccount() == null ||
//                            StringUtils.isNumeric(PaymentUserIdCalculator.calcTenantIdFromUserId(transactionRest.getDebitorAccount().getUserId()))))
//			{
//				response.getItems().add(convertRestToF4MTransaction(transactionRest, paymentClientInfo.getTenantId()));
//			} else {
//				LOGGER.warn("getAccountHistory: CreditorAccount = {} or DebitorAccount = {} dont contain tenant numberic ID! skip"
//						, transactionRest.getCreditorAccount().getUserId(), transactionRest.getDebitorAccount().getUserId());
//			}
//		}
		return accountBalanceManager.getAccountHistory(request, paymentClientInfo.getTenantId(), paymentClientInfo.getProfileId());

	}

	@Override
	public void transferBetweenAccounts(TransferBetweenAccountsRequest request, Callback<TransactionId> originalCallback, ClientInfo clientInfo) {
		analyticsEventManager.savePaymentEvent(request, clientInfo);

//		TransactionRestInsert insert = new TransactionRestInsert();
//		insert.setDebitorAccountId(userAccountManager.
//				findActiveAccountId(request.getTenantId(), request.getFromProfileId(), request.getCurrency()));
//		if (request.getToProfileId() != null && request.getToProfileId().contains("mgi_")) {
//			insert.setCreditorAccountId(userAccountManager.
//					findActiveGameAccountId(request.getTenantId(), request.getToProfileId().replaceFirst("mgi_", ""), request.getCurrency()));
//		} else {
//			insert.setCreditorAccountId(userAccountManager.
//					findActiveAccountId(request.getTenantId(), request.getToProfileId(), request.getCurrency()));
//		}
//
//		LOGGER.debug("transferBetweenAccounts transferBetweenAccounts insert {} ", insert);
//
//		insert.setReference(prepareReferenceFromDetails(request.getPaymentDetails(), gson));
//		insert.setValue(request.getAmount());
//		insert.setType(TransactionType.TRANSFER);
//		ManagerCallback<TransactionRest, ? extends JsonMessageContent> callback = new ManagerCallback<>(
//				originalCallback, transaction -> new TransactionId(transaction.getId()));
//		transactionRestWrapperFactory.create(request.getTenantId())
//				.transferBetweenAccounts(insert, callback);

		accountBalanceManager.transferBetweenAccounts(request);

	}

	@Override
	public ConvertBetweenCurrenciesUserResponse convertBetweenCurrencies(PaymentClientInfo paymentClientInfo, ConvertBetweenCurrenciesUserRequest request,
										 Callback<ConvertBetweenCurrenciesUserResponse> originalCallback)
	{

		ExchangeRate rate = getAmount(paymentClientInfo.getTenantId(), request.getAmount());
		LOGGER.debug("convertBetweenCurrencies rate {} ", rate);
		List<String> transactionId = accountBalanceManager.convertBetweenCurrencies(paymentClientInfo, request, rate);



//
//		if (amount.compareTo(BigDecimal.ZERO) == 0) {
//			adminEmailForwarder.transferFundsToUserAccount(paymentClientInfo.getTenantId(), userId, amount, Currency.CREDIT, appId);
//			transactionId = adminEmailForwarder.withdrawFromThePlayerAccount(appId, tenantId, request.getAmount(), userId);
//		}


//		ExchangeRate exchangeRate = currencyManager.getTenantExchangeRateByFromAmount(tenantId, request.getAmount(), request.getFromCurrency(), request
//				.getToCurrency());
//
//		BigDecimal currencyManagerExchangeRate = currencyManager.getRate(exchangeRate);
//		insert.setUsedExchangeRate(currencyManagerExchangeRate);
//
//		List<AccountRest> userActiveAccounts = userRestWrapperFactory.create(tenantId).getUserActiveAccounts(PaymentUserIdCalculator
//																													 .calcPaymentUserId(tenantId, paymentClientInfo
//																															 .getProfileId()));
//		LOGGER.debug("convertBetweenCurrencies {} ", userActiveAccounts);
//		insert.setDebitorAccountId(userAccountManager
//										   .findAccountFromListOrUseTenantEurAccountId(userActiveAccounts, tenantId, paymentClientInfo.getProfileId(), request
//												   .getFromCurrency()));
//		insert.setCreditorAccountId(userAccountManager
//											.findAccountFromListOrUseTenantEurAccountId(userActiveAccounts, tenantId, paymentClientInfo.getProfileId(), request
//													.getToCurrency()));
//
//		PaymentDetails details = new PaymentDetailsBuilder().additionalInfo(request.getDescription()).build();
//		insert.setReference(prepareReferenceFromDetails(details, gson));
//		insert.setValue(request.getAmount());
//		insert.setType(TransactionType.TRANSFER);

		String logId = transactionLogAerospikeDao.createTransactionLog(createTransactionLog(paymentClientInfo, request, rate.getFromAmount(), rate.getToAmount()));
		for (String s : transactionId) {
			transactionLogAerospikeDao.updateTransactionLog(logId, s, TransactionStatus.COMPLETED);
		}
//		ManagerCallback<TransactionRest, ? extends JsonMessageContent> callback = new ManagerCallback<>(originalCallback, transaction -> {
//			transactionLogAerospikeDao.updateTransactionLog(logId, transaction.getId(), TransactionStatus.COMPLETED);
//			return new ConvertBetweenCurrenciesUserResponse(transaction.getId());
//		});
       // transactionRestWrapperFactory.create(tenantId).transferBetweenAccounts(insert, callback);
		return new ConvertBetweenCurrenciesUserResponse(transactionId.get(0));

	}

	// Get the exchange rates.
	private ExchangeRate getAmount(String tenantId, BigDecimal amountId) {
		List<ExchangeRate> exchangeRateList = currencyManager.getTenantExchangeRateList(tenantId);
		LOGGER.debug("getAmount rate {} ", exchangeRateList);

		return exchangeRateList.stream().filter(rate -> rate.getFromAmount().equals(amountId)).findAny().orElse(null);
	}

	private TransactionLog createTransactionLog(PaymentClientInfo paymentClientInfo, ConvertBetweenCurrenciesUserRequest request,
			BigDecimal exchangeRate, BigDecimal amountTo) {
		TransactionInfo transactionInfoForLog = new TransactionInfo(paymentClientInfo.getTenantId(), paymentClientInfo.getProfileId(),
				paymentClientInfo.getProfileId(), request.getAmount());
		TransactionLog transactionLog = new TransactionLog(transactionInfoForLog, request.getFromCurrency(), request.getDescription(), paymentClientInfo.getAppId());
		transactionLog.setCurrencyTo(request.getToCurrency());
		transactionLog.setAmountTo(amountTo);
		transactionLog.setRate(exchangeRate);

		if (Currency.MONEY.equals(request.getFromCurrency()) &&
				Currency.CREDIT.equals(request.getToCurrency())) {
			ClientInfo clientInfo = new ClientInfo(transactionLog.getTenantId(), paymentClientInfo.getProfileId());
			addInvoiceEvent(clientInfo, request.getAmount(), request.getFromCurrency(),
					request.getToCurrency(), amountTo, exchangeRate);
		}

		return transactionLog;
	}

	private void addInvoiceEvent(ClientInfo clientInfo, BigDecimal amount, Currency currency,
								 Currency currencyTo, BigDecimal amountTo, BigDecimal exchangeRate) {
		InvoiceEvent invoiceEvent = new InvoiceEvent();
		invoiceEvent.setPaymentType(InvoiceEvent.PaymentType.CREDIT_SALES);
		invoiceEvent.setPaymentAmount(amount);
		invoiceEvent.setCurrency(currency);
		invoiceEvent.setCurrencyTo(currencyTo);
		invoiceEvent.setPaymentAmountTo(amountTo);
		invoiceEvent.setExchangeRate(exchangeRate);
		tracker.addEvent(clientInfo, invoiceEvent);
	}

	@Override
	public void loadOrWithdrawWithoutCoverage(LoadOrWithdrawWithoutCoverageRequest request,
			Callback<TransactionId> originalCallback, ClientInfo clientInfo) {
		if (!Currency.BONUS.equals(request.getCurrency()) && !Currency.CREDIT.equals(request.getCurrency())) {
			ErrorInfoRest errInfo = new ErrorInfoRest();
			errInfo.setCode("ERR_NOT_SUPPORTED_CURRENCY"); //don't use PaymentServiceExceptionCodes for Fatal Errors
			errInfo.setMessage(String.format("Not supported payment/loadOrWithdrawWithoutCoverage for %s currency",
					request.getCurrency().toString()));
			throw new F4MPaymentException(errInfo);
		}

        String accountId;
      /*  if (loadOrWithdrawWithoutCoverage.getToProfileId() != null && loadOrWithdrawWithoutCoverage.getToProfileId().contains("mgi_")) {
			// accountId = userAccountManager.findActiveGameAccountId(loadOrWithdrawWithoutCoverage.getTenantId(), loadOrWithdrawWithoutCoverage.getToProfileId().replaceFirst("mgi_", ""), loadOrWithdrawWithoutCoverage.getCurrency());
        } else {
		    accountId = userAccountManager.findActiveAccountId(loadOrWithdrawWithoutCoverage.getTenantId(), loadOrWithdrawWithoutCoverage.getProfileId(), loadOrWithdrawWithoutCoverage.getCurrency());
        }*/

		analyticsEventManager.savePaymentEvent(request, clientInfo);

//		ManagerCallback<TransactionRest, ? extends JsonMessageContent> callback = new ManagerCallback<>(
//				originalCallback, transaction -> new TransactionId(transaction.getId()));
//		transactionRestWrapperFactory.create(loadOrWithdrawWithoutCoverage.getTenantId()).loadOntoOrWithdrawFromAccount(accountId,
//				loadOrWithdrawWithoutCoverage.getAmount(), gson.toJson(loadOrWithdrawWithoutCoverage.getPaymentDetails()), callback);
		if (request.getToProfileId().contains("mgi_")) {
			String mgiId = request.getToProfileId().replaceFirst("mgi_", "");
			gameBalanceManager.transferJackpot(request, mgiId);
		} else accountBalanceManager.loadOrWithdrawWithoutCoverage(request);
	}

	@Override
	public GetExchangeRatesResponse getExchangeRates(PaymentClientInfo paymentClientInfo) {
		List<ExchangeRate> exchangeRateList = currencyManager.getTenantExchangeRateList(paymentClientInfo.getTenantId());
		GetExchangeRatesResponse response = new GetExchangeRatesResponse();
		response.getExchangeRates().addAll(exchangeRateList);
		return response;
	}

	@Override
	public void mergeUserAccounts(MergeUsersRequest request) {
        LOGGER.debug("mergeUserAccounts req {} ", request);
//		String tenantId = request.getTenantId();
//		String fromProfileId = request.getFromProfileId();
//		String toProfileId = request.getToProfileId();
//		currencyManager.findOrInitializeTenant(tenantId);
//
//		MutableBoolean fromProfileFound = new MutableBoolean(true);
//		List<AccountRest> debitorActiveAccounts = PaymentWrapperUtils
//				.executePaymentCall(() -> userAccountManager.findActiveAccounts(tenantId, fromProfileId), e -> {
//					LOGGER.info("Could not find source user during merge of {}", request, e);
//					fromProfileFound.setFalse();
//					return Collections.emptyList();
//				}, PaymentExternalErrorCodes.USER_NOT_FOUND);
//		List<AccountRest> creditorAllAccounts = PaymentWrapperUtils
//				.executePaymentCall(() -> userAccountManager.findAllAccounts(tenantId, toProfileId), e -> {
//					LOGGER.info("Could not find and will create target user during merge of {}", request, e);
//					userAccountManager.createUserFromLatestAerospikeData(tenantId, toProfileId);
//					return userAccountManager.findAllAccounts(tenantId, toProfileId);
//				}, PaymentExternalErrorCodes.USER_NOT_FOUND);
//
//		for (AccountRest debitorAccount : debitorActiveAccounts) {
//			if (BigDecimal.ZERO.compareTo(debitorAccount.getBalance()) < 0) {
//				AccountRest creditorAccount = userAccountManager.findAccountFromListByExtenalCurrency(
//						creditorAllAccounts, tenantId, toProfileId, debitorAccount.getCurrency());
//				transactionRestWrapperFactory.create(request.getTenantId())
//						.transferBetweenAccounts(createTransactionRestInsert(debitorAccount, creditorAccount));
//			}
//		}
//
//		if (fromProfileFound.isTrue()) {
//			userAccountManager.disableUser(tenantId, PaymentUserIdCalculator.calcPaymentUserId(tenantId, fromProfileId));
//		}
	}
	
	private TransactionRestInsert createTransactionRestInsert(AccountRest debitorAccount, AccountRest creditorAccount) {
		TransactionRestInsert insert = new TransactionRestInsert();
		insert.setDebitorAccountId(debitorAccount.getId());
		insert.setCreditorAccountId(creditorAccount.getId());
		PaymentDetails details = new PaymentDetailsBuilder()
				.additionalInfo(String.format("Merge user accounts from user %s to user %s", debitorAccount.getUserId(), creditorAccount.getUserId()))
				.build();
		insert.setReference(prepareReferenceFromDetails(details, gson));
		insert.setValue(debitorAccount.getBalance());
		insert.setType(TransactionType.TRANSFER);
		return insert;
	}
}
