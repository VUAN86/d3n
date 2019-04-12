package de.ascendro.f4m.service.payment.manager.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.callback.Callback;
import de.ascendro.f4m.service.payment.manager.PaymentManager;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.PaymentDetailsBuilder;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.config.MergeUsersRequest;
import de.ascendro.f4m.service.payment.model.external.ConvertBetweenCurrenciesUserRequest;
import de.ascendro.f4m.service.payment.model.external.ConvertBetweenCurrenciesUserResponse;
import de.ascendro.f4m.service.payment.model.external.ExchangeRate;
import de.ascendro.f4m.service.payment.model.external.GetExchangeRatesResponse;
import de.ascendro.f4m.service.payment.model.internal.*;
import de.ascendro.f4m.service.payment.rest.model.TransactionType;
import de.ascendro.f4m.service.util.DateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PaymentManagerMockImpl implements PaymentManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentManagerMockImpl.class);
	private static final String RECEIVE_INFO = "{} received call {} to mock from client info {}";
	private static final String RECEIVE_INTERNAL_INFO = "{} received internal mock call {}";
	
	private ExecutorService threadPool = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ExpiredRecordCleaner-task-%d").build());
	
	@Override
	public void getTransaction(GetTransactionRequest request, Callback<GetTransactionResponse> originalCallback) {
		LOGGER.info(RECEIVE_INTERNAL_INFO, "getTransaction", request);
		//make mocked getTransaction slower than ServerMessageHandler, to check if no resources are released prematurely
		threadPool.submit(() -> {
			try {
				Thread.sleep(500);
				originalCallback.completed(prepareGetTransactionResponse());
			} catch (InterruptedException e) {
				LOGGER.error("The biggest pitfall as you make your way through life is impatience - Susan Jeffers", e);
				Thread.currentThread().interrupt();
			}
		});
	}

	@Override
	public GetAccountHistoryResponse getAccountHistory(PaymentClientInfo paymentClientInfo, GetUserAccountHistoryRequest request) {
		LOGGER.info(RECEIVE_INFO, "getAccountHistory", request, paymentClientInfo);
		GetAccountHistoryResponse accountHistoryResponse = new GetAccountHistoryResponse();
		accountHistoryResponse.setItems(Arrays.asList(prepareGetTransactionResponse()));
		accountHistoryResponse.setOffset(0);
		accountHistoryResponse.setLimit(10);
		accountHistoryResponse.setTotal(1);
		return accountHistoryResponse;
	}
	
	public static ZonedDateTime createDate(int year, int month, int dayOfMonth, int hour, int minute, int second) {
		return ZonedDateTime.of(year, month, dayOfMonth, hour, minute, second, 0, DateTimeUtil.TIMEZONE);
	}

	private GetTransactionResponse prepareGetTransactionResponse() {
		PaymentDetails paymentDetails = new PaymentDetailsBuilder().appId("appId").additionalInfo("no").build();
		return new GetTransactionResponseBuilder()
				.id("transactionId")
				.tenantId("tenant")
				.fromProfileId("fromProfile")
				.toProfileId("toProfile")
				.fromCurrency(Currency.MONEY)
				.toCurrency(Currency.BONUS)
				.amount(new BigDecimal("12.34"))
				.creationDate(createDate(2017, 01, 11, 14, 37, 59))
				.paymentDetails(paymentDetails)
				.type(TransactionType.TRANSFER.toString())
				.build();
	}

	@Override
	public void transferBetweenAccounts(TransferBetweenAccountsRequest request, Callback<TransactionId> originalCallback, ClientInfo clientInfo) {
		LOGGER.info(RECEIVE_INTERNAL_INFO, "transferBetweenAccounts", request);
		originalCallback.completed(new TransactionId("transaction_id_value"));
	}

	@Override
	public void convertBetweenCurrencies(PaymentClientInfo paymentClientInfo,
			ConvertBetweenCurrenciesUserRequest request,
			Callback<ConvertBetweenCurrenciesUserResponse> originalCallback) {
		LOGGER.info(RECEIVE_INFO, "convertBetweenCurrencies", request, paymentClientInfo);
		originalCallback.completed(new ConvertBetweenCurrenciesUserResponse("ResultTransactionId"));
	}

	@Override
	public void loadOrWithdrawWithoutCoverage(LoadOrWithdrawWithoutCoverageRequest request, Callback<TransactionId> originalCallback, ClientInfo clientInfo) {
		LOGGER.info(RECEIVE_INTERNAL_INFO, "loadOrWithdrawWithoutCoverage", request);
		originalCallback.completed(new TransactionId("transaction_id_value"));
	}

	@Override
	public GetExchangeRatesResponse getExchangeRates(PaymentClientInfo paymentClientInfo) {
		GetExchangeRatesResponse response = new GetExchangeRatesResponse();
		List<ExchangeRate> exchangeRates = new ArrayList<>();
		exchangeRates.add(new ExchangeRate("EUR", "CREDIT", new BigDecimal("2"), new BigDecimal("10.00")));
		exchangeRates.add(new ExchangeRate("EUR", "CREDIT", new BigDecimal("3"), new BigDecimal("20.00")));
		response.getExchangeRates().addAll(exchangeRates);
		return response;
	}

	@Override
	public void mergeUserAccounts(MergeUsersRequest request) {
		//empty mock method
	}
}
