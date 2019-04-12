package de.ascendro.f4m.service.payment.payment.system.manager;

import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.config.InsertOrUpdateUserRequest;
import de.ascendro.f4m.service.payment.model.external.ConvertBetweenCurrenciesUserRequest;
import de.ascendro.f4m.service.payment.model.external.ExchangeRate;
import de.ascendro.f4m.service.payment.model.external.GetExternalPaymentRequest;
import de.ascendro.f4m.service.payment.model.external.GetExternalPaymentResponse;
import de.ascendro.f4m.service.payment.model.internal.*;
import de.ascendro.f4m.service.payment.payment.system.model.TenantBalance;
import de.ascendro.f4m.service.payment.rest.model.AccountHistoryRestSearchParams;

import java.util.List;

public interface AccountBalanceManager {

    GetUserAccountBalanceResponse getUserAccountBalance(String profileId, String tenantId);

    GetAccountBalanceResponse getAccountBalance(GetAccountBalanceRequest request);

    GetExternalPaymentResponse getMoneyTransaction(GetExternalPaymentRequest request, String tenantId);

    GetTransactionResponse getExternalPayment(GetTransactionRequest request);

    void transferBetweenAccounts(TransferBetweenAccountsRequest request);

    void loadOrWithdrawWithoutCoverage(LoadOrWithdrawWithoutCoverageRequest request);

    GetAccountHistoryResponse getAccountHistory(GetUserAccountHistoryRequest request, String tenantId, String profileId);

    List<String> convertBetweenCurrencies(PaymentClientInfo paymentClientInfo, ConvertBetweenCurrenciesUserRequest request, ExchangeRate rate);

    TenantBalance getTenantBalance(String tenantId);

    void createUser(InsertOrUpdateUserRequest request);
}
