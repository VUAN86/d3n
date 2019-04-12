package de.ascendro.f4m.service.payment.exploration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.payment.callback.Callback;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.internal.GetAccountHistoryResponse;
import de.ascendro.f4m.service.payment.model.internal.GetTransactionRequest;
import de.ascendro.f4m.service.payment.model.internal.GetTransactionResponse;
import de.ascendro.f4m.service.payment.model.internal.GetUserAccountHistoryRequest;
import de.ascendro.f4m.service.payment.rest.model.CurrencyRest;
import de.ascendro.f4m.service.payment.session.PaymentClientInfoImpl;

public class PaydentSystemExplorationTest extends PaydentExplorationTestBase {
	private static final Logger LOGGER = LoggerFactory.getLogger(PaydentSystemExplorationTest.class);
	
    @Test
	public void testParallelTransactionGetAsyncCalls() throws Exception {
    	PaymentClientInfo paymentClientInfo = new PaymentClientInfoImpl(tenantId, profileId, appId);
		GetUserAccountHistoryRequest request = new GetUserAccountHistoryRequest();
		request.setCurrency(Currency.CREDIT);
		request.setLimit(5);
		GetAccountHistoryResponse accountHistory = paymentManager.getAccountHistory(paymentClientInfo , request);
		LOGGER.info("Total {}, returned {}", accountHistory.getTotal(), accountHistory.getItems().size());
    	
		AtomicInteger success = new AtomicInteger();
		AtomicInteger failures = new AtomicInteger();
		CountDownLatch finalCountdown = new CountDownLatch(accountHistory.getItems().size());
		Callback<GetTransactionResponse> callback = new Callback<GetTransactionResponse>() {
			@Override
			public void completed(GetTransactionResponse response) {
				success.incrementAndGet();
				finalCountdown.countDown();
			}

			@Override
			public void failed(Throwable throwable) {
				failures.incrementAndGet();
				finalCountdown.countDown();
			}
		};
		for (GetTransactionResponse transaction : accountHistory.getItems()) {
			GetTransactionRequest transactionRequest = new GetTransactionRequest();
			transactionRequest.setTenantId(tenantId);
			transactionRequest.setTransactionId(transaction.getId());
			paymentManager.getTransaction(transactionRequest, callback);
		}
		assertThat(success.get() + failures.get(), lessThan(accountHistory.getItems().size()));
		finalCountdown.await(60, TimeUnit.SECONDS);
		assertThat(failures.get(), equalTo(0));
		assertThat(success.get(), equalTo(accountHistory.getItems().size()));
	}
    
    @Test
	public void renameCurrencies() throws Exception {
		CurrencyRest fakeEur = new CurrencyRest();
		fakeEur.setId("9");
		fakeEur.setShortName("DO_NOT_USE_4");
		fakeEur.setName("Accidentally created currency, which should be deleted (4)");
		//currencyRestWrapper.updateCurrency(fakeEur);
		currencyRestWrapper.getCurrencies();
	}
}
