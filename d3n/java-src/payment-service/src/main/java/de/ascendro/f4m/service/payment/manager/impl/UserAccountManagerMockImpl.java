package de.ascendro.f4m.service.payment.manager.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.user.UserRole;
import de.ascendro.f4m.service.payment.callback.Callback;
import de.ascendro.f4m.service.payment.manager.UserAccountManager;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.config.InsertOrUpdateUserRequest;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceRequest;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceResponse;
import de.ascendro.f4m.service.payment.model.internal.GetUserAccountBalanceResponse;
import de.ascendro.f4m.service.payment.rest.model.AccountRest;
import de.ascendro.f4m.service.payment.rest.model.CurrencyRest;
import de.ascendro.f4m.service.payment.rest.model.UserRest;

public class UserAccountManagerMockImpl implements UserAccountManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserAccountManagerMockImpl.class);
	private static final String RECEIVE_INFO = "{} received call {} to mock from client info {}";
	private static final String RECEIVE_INTERNAL_INFO = "{} received internal mock call {}";
	
	@Override
	public void insertOrUpdateUser(InsertOrUpdateUserRequest request, Callback<EmptyJsonMessageContent> originalCallback) {
		LOGGER.info(RECEIVE_INTERNAL_INFO, "insertOrUpdateUser", request);
		originalCallback.completed(new EmptyJsonMessageContent());
	}

	@Override
	public void createUserFromLatestAerospikeData(String tenantId, String profileId) {
		//method not used in test and is not exposed as API
	}

	@Override
	public GetAccountBalanceResponse getTenantAccountBalance(String tenantId) {
		return createBalance(new BigDecimal("43.21"), Currency.MONEY);
	}

	@Override
	public GetAccountBalanceResponse getAccountBalance(PaymentClientInfo paymentClientInfo, GetAccountBalanceRequest request) {
		LOGGER.info(RECEIVE_INFO, "getAccountBalance", request, paymentClientInfo);
		return createBalance(new BigDecimal("12.34"), Currency.MONEY);
	}

	private GetAccountBalanceResponse createBalance(BigDecimal amount, Currency money) {
		GetAccountBalanceResponse accountBalanceResponse = new GetAccountBalanceResponse();
		accountBalanceResponse.setAmount(amount);
		accountBalanceResponse.setCurrency(money);
		return accountBalanceResponse;
	}

	@Override
	public GetUserAccountBalanceResponse getAccountBalances(PaymentClientInfo paymentClientInfo) {
		GetUserAccountBalanceResponse result = new GetUserAccountBalanceResponse();
		result.setBalances(Arrays.asList(createBalance(new BigDecimal("12.34"), Currency.MONEY),
				createBalance(new BigDecimal("200"), Currency.BONUS),
				createBalance(new BigDecimal("0"), Currency.CREDIT)));
		return result;
	}

	@Override
	public String findActiveAccountId(String tenantId, String profileId, Currency currency) {
		return null;
	}
	
	@Override
	public String findAccountIdFromAllAccounts(String tenantId, String profileId, Currency currency) {
		return null;
	}

	@Override
	public AccountRest findAccountByExternalCurrency(String tenantId, String profileId, String currencyShortName) {
		return null;
	}

	@Override
	public AccountRest findActiveAccount(String tenantId, String profileId, Currency currency) {
		return null;
	}

	@Override
	public String findAccountFromListOrUseTenantEurAccountId(List<AccountRest> userActiveAccounts, String tenantId,
			String profileId, Currency currency) {
		return null;
	}

	@Override
	public UserRest disableUser(String tenantId, String userId) {
		return null;
	}

	@Override
	public List<AccountRest> findActiveAccounts(String tenantId, String profileId) {
		return Collections.emptyList();
	}

	@Override
	public AccountRest findAccountFromListByExtenalCurrency(List<AccountRest> userActiveAccounts, String tenantId,
			String profileId, CurrencyRest currencyRest) {
		return null;
	}

	@Override
	public List<AccountRest> findAllAccounts(String tenantId, String profileId) {
		return Collections.emptyList();
	}

	@Override
	public String[] getUserRoles(String userId, String tenantId) {
		return Arrays.asList(UserRole.ANONYMOUS.toString()).toArray(new String[1]);
	}

	@Override
	public Set<String> getProfileTenants(String profileId) {
		return Collections.emptySet();
	}
}
