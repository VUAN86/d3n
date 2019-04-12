package de.ascendro.f4m.service.payment.manager;

import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.payment.callback.Callback;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.config.InsertOrUpdateUserRequest;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceRequest;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceResponse;
import de.ascendro.f4m.service.payment.model.internal.GetUserAccountBalanceResponse;
import de.ascendro.f4m.service.payment.rest.model.AccountRest;
import de.ascendro.f4m.service.payment.rest.model.CurrencyRest;
import de.ascendro.f4m.service.payment.rest.model.UserRest;

import java.util.List;
import java.util.Set;

public interface UserAccountManager {
	void insertOrUpdateUser(InsertOrUpdateUserRequest request, Callback<EmptyJsonMessageContent> originalCallback);
	
	void createUserFromLatestAerospikeData(String tenantId, String profileId);

	UserRest disableUser(String tenantId, String userId);

	GetAccountBalanceResponse getTenantAccountBalance(String tenantId);

	GetAccountBalanceResponse getAccountBalance(PaymentClientInfo paymentClientInfo, GetAccountBalanceRequest request);
	
	GetUserAccountBalanceResponse getAccountBalances(PaymentClientInfo paymentClientInfo);
	
	String findActiveAccountId(String tenantId, String profileId, Currency currency);
	
	String findAccountIdFromAllAccounts(String tenantId, String profileId, Currency currency);
	
	//following methods did not exist in PaymentManager and possibly should be removed or refactored 
    AccountRest findAccountByExternalCurrency(String tenantId, String profileId, String currencyShortName);
	
	AccountRest findActiveAccount(String tenantId, String profileId, Currency currency);

	List<AccountRest> findActiveAccounts(String tenantId, String profileId);

	default AccountRest findActiveGameAccount(String tenantId, String profileId, Currency currency) {
		//"The findActiveGameAccount implemented in UserAccountManagerImpl"
		return null;
	}

	default String findActiveGameAccountId(String tenantId, String profileId, Currency currency) {
		//"The findActiveGameAccount implemented in UserAccountManagerImpl"
		return null;
	}

    String findAccountFromListOrUseTenantEurAccountId(List<AccountRest> userActiveAccounts, String tenantId,
            String profileId, Currency currency);
	
	AccountRest findAccountFromListByExtenalCurrency(List<AccountRest> userActiveAccounts, String tenantId,
			String profileId, CurrencyRest currencyRest);

	List<AccountRest> findAllAccounts(String tenantId, String profileId);

	String[] getUserRoles(String userId, String tenantId);

	Set<String> getProfileTenants(String profileId);
}
