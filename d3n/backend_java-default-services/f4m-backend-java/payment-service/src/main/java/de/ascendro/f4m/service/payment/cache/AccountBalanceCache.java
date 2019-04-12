package de.ascendro.f4m.service.payment.cache;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.client.AdminEmailForwarder;
import de.ascendro.f4m.service.payment.exception.F4MPaymentClientException;
import de.ascendro.f4m.service.payment.exception.F4MPaymentSystemIOException;
import de.ascendro.f4m.service.payment.manager.UserAccountManager;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceRequest;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceResponse;
import de.ascendro.f4m.service.payment.model.internal.GetUserAccountBalanceResponse;
import de.ascendro.f4m.service.payment.payment.system.manager.AccountBalanceManager;
import de.ascendro.f4m.service.payment.rest.wrapper.PaymentExternalErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class AccountBalanceCache {
	private static final Logger LOGGER = LoggerFactory.getLogger(AccountBalanceCache.class);

	public static final String PAYMENT_ACCOUNT_BALANCE_SET_NAME = "paymentBalance"; 
	public static final String PAYMENT_ACCOUNT_BALANCE_BIN_NAME = "value";
	private static final String PAYMENT_ACCOUNT_BALANCE_KEY_PREFIX = "balance" +  PrimaryKeyUtil.KEY_ITEM_SEPARATOR;

	private UserAccountManager userAccountManager;
	private AerospikeDao aerospikeDao;
	private AdminEmailForwarder emailForwarder;

	@Inject
	private AccountBalanceManager accountBalanceManager;

	@Inject
	public AccountBalanceCache(UserAccountManager userAccountManager, AerospikeDao aerospikeDao,
			AdminEmailForwarder emailForwarder) {
		this.userAccountManager = userAccountManager;
		this.aerospikeDao = aerospikeDao;
		this.emailForwarder = emailForwarder;
	}

	public GetUserAccountBalanceResponse getAccountBalances(PaymentClientInfo paymentClientInfo) {
//		String messageDescription = "getAccountBalance for tenant " + paymentClientInfo.getTenantId() + ", profile "
//				+ paymentClientInfo.getProfileId();
//		LOGGER.debug("getAccountBalances profileId {} ", paymentClientInfo.getProfileId());
//		LOGGER.debug("getAccountBalances tenantId {} ", paymentClientInfo.getTenantId());
//
//		List<AccountBalance> accountBalanceList =
//				accountBalanceService.getUserAccountBalance(paymentClientInfo.getProfileId(), paymentClientInfo.getTenantId());
//		LOGGER.debug("accountBalance {} ", accountBalanceList);
//        List<GetAccountBalanceResponse> accountBalanceResponseList =
//                accountBalanceList
//                        .stream()
//                        .map(a -> new GetAccountBalanceResponse(a.getBalance(), a.getCurrencies().getCurrency()))
//                        .collect(Collectors.toList());

//        return new GetUserAccountBalanceResponse(accountBalanceResponseList);

//		return execute(messageDescription,
//		() -> {
//			GetUserAccountBalanceResponse balances = userAccountManager.getAccountBalances(paymentClientInfo);
//			for (GetAccountBalanceResponse balance : balances.getBalances()) {
//				String primaryKey = createPrimaryKey(paymentClientInfo.getTenantId(), paymentClientInfo.getProfileId(),
//						balance.getCurrency());
//				writeAccountBalanceToAerospikeCache(primaryKey, balance.getAmount());
//			}
//			return balances;
//		}, e -> {
//			GetUserAccountBalanceResponse response = new GetUserAccountBalanceResponse();
//			response.setBalances(new ArrayList<>(Currency.values().length));
//			for (Currency currency : Currency.values()) {
//				String primaryKey = createPrimaryKey(paymentClientInfo.getTenantId(), paymentClientInfo.getProfileId(),
//						currency);
//				response.getBalances().add(findBalanceInCache(currency, primaryKey, e));
//			}
//			return response;
//		});
		return accountBalanceManager.getUserAccountBalance(paymentClientInfo.getProfileId(), paymentClientInfo.getTenantId());
	}

	public GetAccountBalanceResponse getAccountBalance(ClientInfo clientInfo, GetAccountBalanceRequest request) {
		String tenantId;
		String profileId;
		if (clientInfo != null) {
			tenantId = clientInfo.getTenantId();
			profileId = clientInfo.getUserId();
		} else
		{
			tenantId = request.getTenantId();
			profileId = request.getProfileId();
		}
//		AccountBalance accountBalance = accountBalanceService.get(profileId, tenantId, loadOrWithdrawWithoutCoverage.getCurrency());
//		return new GetAccountBalanceResponse(accountBalance.getBalance(), accountBalance.getCurrencies().getCurrency());
		return accountBalanceManager.getAccountBalance(request);
//		String primaryKey = createPrimaryKey(paymentClientInfo.getTenantId(), paymentClientInfo.getProfileId(),
//				loadOrWithdrawWithoutCoverage.getCurrency());
//
//		String messageDescription = "getAccountBalance for tenantId=" + paymentClientInfo.getTenantId() + ", profileId="
//				+ paymentClientInfo.getProfileId() + ", currency=" + loadOrWithdrawWithoutCoverage.getCurrency();
//		String tenantId;
//		ClientInfo clientInfo = message.getClientInfo();
//		if (clientInfo != null) {
//			//for users always use tenantId from clientInfo. Same for internal service calls in users context call
//			tenantId = clientInfo.getTenantId();
//		} else {
//			//for service internal calls use tenantId from the message
//			tenantId = balanceRequest.getTenantId();
//		}
//		return new PaymentClientInfoImpl(tenantId, balanceRequest.getProfileId(), appId);
//
//		return execute(messageDescription,
//		() -> {
//			GetAccountBalanceResponse accountBalance = userAccountManager.getAccountBalance(paymentClientInfo, loadOrWithdrawWithoutCoverage);
//			writeAccountBalanceToAerospikeCache(primaryKey, accountBalance.getAmount());
//			return accountBalance;
//		}, e -> findBalanceInCache(loadOrWithdrawWithoutCoverage.getCurrency(), primaryKey, e));
	}

	private GetAccountBalanceResponse findBalanceInCache(Currency currency, String primaryKey,
			RuntimeException e) {
		Optional<BigDecimal> balance = readAccountBalanceFromAerospikeCache(primaryKey);
		if (balance.isPresent()) {
			GetAccountBalanceResponse response = new GetAccountBalanceResponse();
			response.setAmount(balance.get());
			response.setCurrency(currency);
			return response;
		} else {
			LOGGER.warn("Account balance for {} could not be found in cache, rethrowing exception", primaryKey);
			throw e;
		}
	}
	
	private <T> T execute(String messageDescription, Supplier<T> call,
			Function<RuntimeException, T> paymentFailureHandler) {
		try {
			return call.get();
		} catch (F4MPaymentSystemIOException e) {
			return processFailure(messageDescription, paymentFailureHandler, e);
		} catch (F4MPaymentClientException e) {
			if (!PaymentExternalErrorCodes.UNDEFINED_ERROR.getCode().equals(e.getCode())) {
				throw e; //all other exception codes mean a business error on our side
			}
			return processFailure(messageDescription, paymentFailureHandler, e);
		}
	}

	private <T> T processFailure(String messageDescription, Function<RuntimeException, T> paymentFailureHandler,
			F4MException e) {
		LOGGER.warn("Payment system failed, returning balance from cache after error", e);
		if (emailForwarder.shouldForwardErrorToAdmin(e)) {
			emailForwarder.forwardWarningToAdmin(messageDescription, e);
		}
		return paymentFailureHandler.apply(e);
	}
	
	private Optional<BigDecimal> readAccountBalanceFromAerospikeCache(String key) {
		String balance = aerospikeDao.readString(PAYMENT_ACCOUNT_BALANCE_SET_NAME, key, PAYMENT_ACCOUNT_BALANCE_BIN_NAME);
		return Optional.ofNullable(balance).map(BigDecimal::new); 
	}
	
	private void writeAccountBalanceToAerospikeCache(String key, BigDecimal value) {
		aerospikeDao.createOrUpdateString(PAYMENT_ACCOUNT_BALANCE_SET_NAME, key, PAYMENT_ACCOUNT_BALANCE_BIN_NAME,
				(v, wp) -> value.toString());
	}
	
	private String createPrimaryKey(String tenantId, String profileId, Currency currency) {
		return PAYMENT_ACCOUNT_BALANCE_KEY_PREFIX + tenantId + PrimaryKeyUtil.KEY_ITEM_SEPARATOR + profileId
				+ PrimaryKeyUtil.KEY_ITEM_SEPARATOR + currency;
	}

}
