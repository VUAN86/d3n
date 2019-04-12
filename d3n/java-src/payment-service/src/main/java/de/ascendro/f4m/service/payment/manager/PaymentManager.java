package de.ascendro.f4m.service.payment.manager;

import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.callback.Callback;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.config.MergeUsersRequest;
import de.ascendro.f4m.service.payment.model.external.ConvertBetweenCurrenciesUserRequest;
import de.ascendro.f4m.service.payment.model.external.ConvertBetweenCurrenciesUserResponse;
import de.ascendro.f4m.service.payment.model.external.GetExchangeRatesResponse;
import de.ascendro.f4m.service.payment.model.internal.GetAccountHistoryResponse;
import de.ascendro.f4m.service.payment.model.internal.GetTransactionRequest;
import de.ascendro.f4m.service.payment.model.internal.GetTransactionResponse;
import de.ascendro.f4m.service.payment.model.internal.GetUserAccountHistoryRequest;
import de.ascendro.f4m.service.payment.model.internal.LoadOrWithdrawWithoutCoverageRequest;
import de.ascendro.f4m.service.payment.model.internal.TransferBetweenAccountsRequest;

public interface PaymentManager {

	void getTransaction(GetTransactionRequest request, Callback<GetTransactionResponse> originalCallback);

	GetAccountHistoryResponse getAccountHistory(PaymentClientInfo paymentClientInfo, GetUserAccountHistoryRequest request);

	void convertBetweenCurrencies(PaymentClientInfo paymentClientInfo, ConvertBetweenCurrenciesUserRequest request,
			Callback<ConvertBetweenCurrenciesUserResponse> originalCallback);

	void transferBetweenAccounts(TransferBetweenAccountsRequest request, Callback<TransactionId> originalCallback, ClientInfo clientInfo);

	void loadOrWithdrawWithoutCoverage(LoadOrWithdrawWithoutCoverageRequest request, Callback<TransactionId> originalCallback, ClientInfo clientInfo);
	
	GetExchangeRatesResponse getExchangeRates(PaymentClientInfo paymentClientInfo);
	
	void mergeUserAccounts(MergeUsersRequest request);
}
