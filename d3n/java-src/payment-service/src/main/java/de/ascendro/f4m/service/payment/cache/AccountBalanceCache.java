package de.ascendro.f4m.service.payment.cache;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.payment.client.AdminEmailForwarder;
import de.ascendro.f4m.service.payment.exception.F4MPaymentClientException;
import de.ascendro.f4m.service.payment.exception.F4MPaymentSystemIOException;
import de.ascendro.f4m.service.payment.manager.UserAccountManager;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceRequest;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceResponse;
import de.ascendro.f4m.service.payment.model.internal.GetUserAccountBalanceResponse;
import de.ascendro.f4m.service.payment.rest.wrapper.PaymentExternalErrorCodes;

public class AccountBalanceCache {
	private static final Logger LOGGER = LoggerFactory.getLogger(AccountBalanceCache.class);
	
	public static final String PAYMENT_ACCOUNT_BALANCE_SET_NAME = "paymentBalance"; 
	public static final String PAYMENT_ACCOUNT_BALANCE_BIN_NAME = "value";
	private static final String PAYMENT_ACCOUNT_BALANCE_KEY_PREFIX = "balance" +  PrimaryKeyUtil.KEY_ITEM_SEPARATOR;

	private UserAccountManager userAccountManager;
	private AerospikeDao aerospikeDao;
	private AdminEmailForwarder emailForwarder;

	@Inject
	public AccountBalanceCache(UserAccountManager userAccountManager, AerospikeDao aerospikeDao,
			AdminEmailForwarder emailForwarder) {
		this.userAccountManager = userAccountManager;
		this.aerospikeDao = aerospikeDao;
		this.emailForwarder = emailForwarder;
	}
	
	public GetUserAccountBalanceResponse getAccountBalances(PaymentClientInfo paymentClientInfo) {
		String messageDescription = "getAccountBalance for tenant " + paymentClientInfo.getTenantId() + ", profile "
				+ paymentClientInfo.getProfileId();
		return execute(messageDescription, 
		() -> {
			GetUserAccountBalanceResponse balances = userAccountManager.getAccountBalances(paymentClientInfo);
			for (GetAccountBalanceResponse balance : balances.getBalances()) {
				String primaryKey = createPrimaryKey(paymentClientInfo.getTenantId(), paymentClientInfo.getProfileId(),
						balance.getCurrency());
				writeAccountBalanceToAerospikeCache(primaryKey, balance.getAmount());
			}
			return balances;
		}, e -> {
			GetUserAccountBalanceResponse response = new GetUserAccountBalanceResponse();
			response.setBalances(new ArrayList<>(Currency.values().length));
			for (Currency currency : Currency.values()) {
				String primaryKey = createPrimaryKey(paymentClientInfo.getTenantId(), paymentClientInfo.getProfileId(),
						currency);
				response.getBalances().add(findBalanceInCache(currency, primaryKey, e));
			}
			return response;
		});
	}
	
	public GetAccountBalanceResponse getAccountBalance(PaymentClientInfo paymentClientInfo, GetAccountBalanceRequest request) {
		String primaryKey = createPrimaryKey(paymentClientInfo.getTenantId(), paymentClientInfo.getProfileId(),
				request.getCurrency());
		
		String messageDescription = "getAccountBalance for tenantId=" + paymentClientInfo.getTenantId() + ", profileId="
				+ paymentClientInfo.getProfileId() + ", currency=" + request.getCurrency();
		return execute(messageDescription, 
		() -> {
			GetAccountBalanceResponse accountBalance = userAccountManager.getAccountBalance(paymentClientInfo, request);
			writeAccountBalanceToAerospikeCache(primaryKey, accountBalance.getAmount());
			return accountBalance;
		}, e -> findBalanceInCache(request.getCurrency(), primaryKey, e));
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
