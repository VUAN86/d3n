package de.ascendro.f4m.service.payment.rest.wrapper;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.callback.Callback;
import de.ascendro.f4m.service.payment.callback.WaitingCallback;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.di.RestClientProvider;
import de.ascendro.f4m.service.payment.exception.F4MPaymentClientException;
import de.ascendro.f4m.service.payment.rest.model.TransactionRest;
import de.ascendro.f4m.service.payment.rest.model.TransactionRestInsert;
import de.ascendro.f4m.service.payment.rest.model.TransactionType;

public class TransactionRestWrapperTest {
	@Mock
	private PaymentConfig config;
	@Mock
	private RestClientProvider restClientProvider;
	@Mock
	private LoggingUtil loggingUtil;
	@InjectMocks
	private PaymentWrapperUtils utils;
	private TransactionRestWrapper transactionRestWrapper;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		transactionRestWrapper = spy(
				new TransactionRestWrapper("tenantId", restClientProvider, config, loggingUtil, utils));
		when(config.getPropertyAsInteger(PaymentConfig.TRANSACTION_RETRY_TIMES)).thenReturn(5);
	}

	@Test(timeout = 1_000)
	@SuppressWarnings("unchecked")
	public void transferBetweenAccountsRetryTest() {
		AtomicInteger retryCountdown = new AtomicInteger(2);
		TransactionRest response = new TransactionRest();
		WaitingCallback<TransactionRest> waitingCallback = new WaitingCallback<>();
		executeTransaction(waitingCallback, invocation -> {
			int attemptNr = retryCountdown.getAndDecrement();
			Callback<TransactionRest> callback = (Callback<TransactionRest>) invocation.getArgument(2);
			if (attemptNr > 0) {
				callback.failed(new F4MPaymentClientException(
						PaymentExternalErrorCodes.ACCOUNTTRANSACTION_FAILED.getF4MCode(), "any"));
			} else {
				callback.completed(response);
			}
			return null;
		});
		assertSame(response, waitingCallback.getResult());
	}

	@Test(timeout = 1_000)
	@SuppressWarnings("unchecked")
	public void transferBetweenAccountsRetryAlwaysFailing() {
		List<String> testResults = new ArrayList<>();
		Callback<TransactionRest> waitingCallback = new Callback<TransactionRest>() {
			@Override
			public void completed(TransactionRest response) {
				testResults.add("success");
			}

			@Override
			public void failed(Throwable throwable) {
				testResults.add("failure");
			}
		};
		executeTransaction(waitingCallback, invocation -> {
			Callback<TransactionRest> callback = (Callback<TransactionRest>) invocation.getArgument(2);
			callback.failed(new F4MPaymentClientException(
					PaymentExternalErrorCodes.ACCOUNTTRANSACTION_FAILED.getF4MCode(), "any"));
			return null;
		});
		assertThat(testResults, contains("failure"));
	}

	private void executeTransaction(Callback<TransactionRest> callback, Answer<Void> answer) {
		doAnswer(answer).when(transactionRestWrapper).callPostAsync(any(), any(), any());
		TransactionRestInsert insert = new TransactionRestInsert();
		insert.setType(TransactionType.CREDIT);
		transactionRestWrapper.transferBetweenAccounts(insert, callback);
	}
}
