package de.ascendro.f4m.service.payment.manager.impl;

import static de.ascendro.f4m.service.payment.manager.PaymentTestUtil.prepareCurrencyRest;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.server.analytics.AnalyticsDao;
import de.ascendro.f4m.server.analytics.tracker.TrackerImpl;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.callback.CallbackAnswer;
import de.ascendro.f4m.service.payment.callback.TestCallback;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.dao.TenantDao;
import de.ascendro.f4m.service.payment.dao.TenantInfo;
import de.ascendro.f4m.service.payment.manager.AnalyticsEventManager;
import de.ascendro.f4m.service.payment.manager.CurrencyManager;
import de.ascendro.f4m.service.payment.manager.CurrencyManagerTest;
import de.ascendro.f4m.service.payment.manager.ExternalTestCurrency;
import de.ascendro.f4m.service.payment.manager.PaymentTestUtil;
import de.ascendro.f4m.service.payment.manager.PaymentUserIdCalculator;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.PaymentDetailsBuilder;
import de.ascendro.f4m.service.payment.model.TransactionId;
import de.ascendro.f4m.service.payment.model.TransactionLog;
import de.ascendro.f4m.service.payment.model.TransactionStatus;
import de.ascendro.f4m.service.payment.model.config.MergeUsersRequest;
import de.ascendro.f4m.service.payment.model.external.ConvertBetweenCurrenciesUserRequest;
import de.ascendro.f4m.service.payment.model.external.ConvertBetweenCurrenciesUserResponse;
import de.ascendro.f4m.service.payment.model.external.ExchangeRate;
import de.ascendro.f4m.service.payment.model.external.GetExchangeRatesResponse;
import de.ascendro.f4m.service.payment.model.internal.GetAccountHistoryRequest;
import de.ascendro.f4m.service.payment.model.internal.GetAccountHistoryResponse;
import de.ascendro.f4m.service.payment.model.internal.GetTransactionRequest;
import de.ascendro.f4m.service.payment.model.internal.GetTransactionResponse;
import de.ascendro.f4m.service.payment.model.internal.LoadOrWithdrawWithoutCoverageRequest;
import de.ascendro.f4m.service.payment.model.internal.PaymentDetails;
import de.ascendro.f4m.service.payment.model.internal.TransferBetweenAccountsRequest;
import de.ascendro.f4m.service.payment.model.internal.TransferBetweenAccountsRequestBuilder;
import de.ascendro.f4m.service.payment.rest.model.AccountHistoryRest;
import de.ascendro.f4m.service.payment.rest.model.AccountHistoryRestSearchParams;
import de.ascendro.f4m.service.payment.rest.model.AccountRest;
import de.ascendro.f4m.service.payment.rest.model.AccountRestBuilder;
import de.ascendro.f4m.service.payment.rest.model.CurrencyRest;
import de.ascendro.f4m.service.payment.rest.model.TransactionRest;
import de.ascendro.f4m.service.payment.rest.model.TransactionRestInsert;
import de.ascendro.f4m.service.payment.rest.model.TransactionType;
import de.ascendro.f4m.service.payment.rest.wrapper.AccountRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.CurrencyRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.ExchangeRateRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.TransactionRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.UserRestWrapper;
import de.ascendro.f4m.service.payment.session.PaymentClientInfoImpl;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.util.TestGsonProvider;
import io.github.benas.randombeans.api.EnhancedRandom;

public class PaymentManagerImplTest {
	@Mock
	private TransactionRestWrapper transactionRestWrapper;
	@Mock
	private UserRestWrapper userRestWrapper;
	@Mock
	private AccountRestWrapper accountRestWrapper;
	@Mock
	private CurrencyRestWrapper currencyRestWrapper;
	@Mock
	private ExchangeRateRestWrapper exchangeRateRestWrapper;
	@Mock
	private TenantDao tenantAerospikeDao;
	@Mock
	private TransactionLogAerospikeDao transactionLogAerospikeDao;
	@Mock
	private UserAccountManagerImpl userAccountManagerImpl;
	@Mock
	private SessionPool sessionPool;
	@Mock
	private AnalyticsDao analyticsDao;

	private PaymentManagerImpl paymentManager;
	private TenantInfo tenantInfo;
	
	private final String tenantId = "tenant";
	private final String profileId = "profile";
	private final String appId = "appId";
	private final String paymentUserId = PaymentUserIdCalculator.calcPaymentUserId(tenantId, profileId);
	private GetTransactionRequest transactionRequestWithTenantId;
	private AccountRest tenantMoneyAccount;
	private ClientInfo clientInfo;
	
	@Before
	public void setUp() {
		//necessity to inject real currencyManager within test shows too tight coupling between PaymentManagerImpl and CurrencyManager
		MockitoAnnotations.initMocks(this);
		GsonProvider gsonProvider = new TestGsonProvider();
		TrackerImpl tracker = new TrackerImpl(analyticsDao);
		AnalyticsEventManager analyticsEventManager = new AnalyticsEventManager(gsonProvider, tracker);
		tenantInfo = new TenantInfo();
		tenantInfo.setMainCurrency("EUR");
		when(tenantAerospikeDao.getTenantInfo(anyString())).thenReturn(tenantInfo);
		when(currencyRestWrapper.getCurrencies()).thenReturn(PaymentTestUtil.createCurrencyRestsForVirtualCurrencies());
		tenantMoneyAccount = PaymentTestUtil.prepareEurAccount();
		tenantMoneyAccount.setId("tenantMoneyAccountId");
		when(accountRestWrapper.getTenantMoneyAccount()).thenReturn(tenantMoneyAccount);
		CurrencyManager currencyManager = new CurrencyManager((id) -> currencyRestWrapper, (id) -> accountRestWrapper,
				tenantAerospikeDao, (id) -> exchangeRateRestWrapper, new PaymentConfig());
		paymentManager = new PaymentManagerImpl(currencyManager, userAccountManagerImpl, (id) -> userRestWrapper,
				(id) -> accountRestWrapper, (id) -> transactionRestWrapper, transactionLogAerospikeDao, gsonProvider, analyticsEventManager, tracker);
		currencyManager.initSystemCurrencies(tenantId);
		
		transactionRequestWithTenantId = new GetTransactionRequest();
		transactionRequestWithTenantId.setTenantId(tenantId);
		//Creating clientInfo with dummy data
		clientInfo = new ClientInfo();
		clientInfo.setAppId("1");
		clientInfo.setCountryCode(ISOCountry.DE);
	}

	@Test
	public void testTransferBetweenAccountsProfile() throws Exception {
		doAnswer(new CallbackAnswer<>(new TransactionRest())).when(transactionRestWrapper).transferBetweenAccounts(any(), any());
		BigDecimal expectedAmount = new BigDecimal("12.34");
		TransferBetweenAccountsRequest request = prepareTransferBetweenAccountsRequest(expectedAmount, Currency.MONEY);
		String expectedFromAccountId = "fromAccountId";
		String expectedToAccountId = "toAccountId";
		when(userAccountManagerImpl.findActiveAccountId(tenantId, request.getFromProfileId(),
				Currency.MONEY)).thenReturn(expectedFromAccountId);
		when(userAccountManagerImpl.findActiveAccountId(tenantId, request.getToProfileId(),
				Currency.MONEY)).thenReturn(expectedToAccountId);

		try (TestCallback<TransactionId> callback = new TestCallback.WithIgnoringConsumer<>()) {
			paymentManager.transferBetweenAccounts(request, callback, clientInfo);
		}
		ArgumentCaptor<TransactionRestInsert> argument = ArgumentCaptor.forClass(TransactionRestInsert.class);
		verify(transactionRestWrapper).transferBetweenAccounts(argument.capture(), any());
		assertThat(argument.getValue().getDebitorAccountId(), equalTo(expectedFromAccountId));
		assertThat(argument.getValue().getCreditorAccountId(), equalTo(expectedToAccountId));
		assertThat(argument.getValue().getValue(), equalTo(expectedAmount));
		assertThat(argument.getValue().getReference(), equalTo(preparePaymentDetailsWithAppIdAndAdditionalInfo()));
		assertThat(argument.getValue().getType(), equalTo(TransactionType.TRANSFER));
		assertNull(argument.getValue().getUsedExchangeRate());
	}

	private TransferBetweenAccountsRequest prepareTransferBetweenAccountsRequest(BigDecimal expectedAmount, Currency currency) {
		PaymentDetails paymentDetails = new PaymentDetailsBuilder().appId("appId").gameId("1").additionalInfo("none").build();
		return new TransferBetweenAccountsRequestBuilder()
				.tenantId(tenantId)
				.fromProfileId("fromProfileId")
				.toProfileId("toProfileId")
				.amount(expectedAmount)
				.currency(currency)
				.paymentDetails(paymentDetails).build();
	}
	
	@Test
	public void testConvertBetweenCurrencies() throws Exception {
		String logId = "logId";
		when(transactionLogAerospikeDao.createTransactionLog(any())).thenReturn(logId);
		PaymentClientInfo paymentClientInfo = new PaymentClientInfoImpl(tenantId, profileId, appId);
		
		String expectedFromAccountId = "fromAccountId";
		String expectedToAccountId = "toAccountId";
		when(userAccountManagerImpl.findAccountFromListOrUseTenantEurAccountId(any(), eq(tenantId), eq(profileId),
				eq(Currency.BONUS))).thenReturn(expectedFromAccountId);
		when(userAccountManagerImpl.findAccountFromListOrUseTenantEurAccountId(any(), eq(tenantId), eq(profileId),
				eq(Currency.CREDIT))).thenReturn(expectedToAccountId);
		
		TransactionRest transactionRest = prepareTransactionRest();
		doAnswer(new CallbackAnswer<>(transactionRest)).when(transactionRestWrapper).transferBetweenAccounts(any(), any());
		
		ExchangeRate rate = new ExchangeRate();
		rate.setFromCurrency(ExternalTestCurrency.BONUS.toString());
		tenantInfo.setExchangeRates(Arrays.asList(CurrencyManagerTest.createRate("10.00", ExternalTestCurrency.BONUS,
				"200", ExternalTestCurrency.CREDIT)));
		
		ConvertBetweenCurrenciesUserRequest request = new ConvertBetweenCurrenciesUserRequest();
		request.setFromCurrency(Currency.BONUS);
		request.setToCurrency(Currency.CREDIT);
		request.setDescription("Simple unnecessary description");
		BigDecimal expectedAmount = new BigDecimal("10");
		request.setAmount(new BigDecimal("10"));
		try (TestCallback<ConvertBetweenCurrenciesUserResponse> callback = new TestCallback.WithIgnoringConsumer<>()) {
			paymentManager.convertBetweenCurrencies(paymentClientInfo, request, callback);
		}

		ArgumentCaptor<TransactionRestInsert> argument = ArgumentCaptor.forClass(TransactionRestInsert.class);
		verify(transactionRestWrapper).transferBetweenAccounts(argument.capture(), any());
		assertThat(argument.getValue().getDebitorAccountId(), equalTo(expectedFromAccountId));
		assertThat(argument.getValue().getCreditorAccountId(), equalTo(expectedToAccountId));
		assertThat(argument.getValue().getValue(), comparesEqualTo(expectedAmount));
		assertThat(argument.getValue().getReference(), equalTo("{\"additionalInfo\":\"" + request.getDescription() + "\"}"));
		assertThat(argument.getValue().getType(), equalTo(TransactionType.TRANSFER));
		assertThat(argument.getValue().getUsedExchangeRate(), comparesEqualTo(new BigDecimal("20")));
		
		ArgumentCaptor<TransactionLog> logArg = ArgumentCaptor.forClass(TransactionLog.class);
		verify(transactionLogAerospikeDao).createTransactionLog(logArg.capture());
		assertThat(logArg.getValue().getStatus(), equalTo(TransactionStatus.INITIATED));
		assertThat(logArg.getValue().getCurrency(), equalTo(Currency.BONUS));
		assertThat(logArg.getValue().getCurrencyTo(), equalTo(Currency.CREDIT));
		assertThat(logArg.getValue().getRate(), comparesEqualTo(new BigDecimal("200").divide(new BigDecimal("10.00"))));
		verify(transactionLogAerospikeDao).updateTransactionLog(logId, transactionRest.getId(), TransactionStatus.COMPLETED);
	}

	@Test
	public void testConvertBetweenCurrenciesSmallExchangeRate() throws Exception {
		PaymentClientInfo paymentClientInfo = new PaymentClientInfoImpl(tenantId, profileId, appId);
		TransactionRest transactionRest = prepareTransactionRest();
		doAnswer(new CallbackAnswer<>(transactionRest)).when(transactionRestWrapper).transferBetweenAccounts(any(), any());
		ExchangeRate rate = new ExchangeRate();
		rate.setFromCurrency(ExternalTestCurrency.BONUS.toString());
		tenantInfo.setExchangeRates(Arrays.asList(CurrencyManagerTest.createRate("1000000000.00", ExternalTestCurrency.BONUS,
				"1", ExternalTestCurrency.CREDIT)));
		
		ConvertBetweenCurrenciesUserRequest request = new ConvertBetweenCurrenciesUserRequest();
		request.setFromCurrency(Currency.BONUS);
		request.setToCurrency(Currency.CREDIT);
		request.setDescription("Simple unnecessary description");
		request.setAmount(new BigDecimal("1000000000.00"));
		try (TestCallback<ConvertBetweenCurrenciesUserResponse> callback = new TestCallback.WithIgnoringConsumer<>()) {
			paymentManager.convertBetweenCurrencies(paymentClientInfo, request, callback);
		}
		ArgumentCaptor<TransactionLog> logArg = ArgumentCaptor.forClass(TransactionLog.class);
		verify(transactionLogAerospikeDao).createTransactionLog(logArg.capture());

		assertThat(logArg.getValue().getRate(), comparesEqualTo(CurrencyManager.SMALLEST_EXCHANGE_RATE));
	}
	
	@Test
	public void testGetTransaction() throws Exception {
		TransactionRest transactionRest = prepareTransactionRest();
		doAnswer(new CallbackAnswer<>(transactionRest)).when(transactionRestWrapper).getTransaction(any(), any());
		try (TestCallback<GetTransactionResponse> callback = new TestCallback<>(
				(response) -> assertExpectedTransactionResponse(transactionRest, response))) {
			paymentManager.getTransaction(transactionRequestWithTenantId, callback);
		}
	}
	
	@Test
	public void testGetTransactionWithTenantMoneyAccount() throws Exception {
		TransactionRest transactionRest = prepareTransactionRest();
		transactionRest.getDebitorAccount().setId(tenantMoneyAccount.getId());
		transactionRest.getDebitorAccount().setUserId("anyValue_AsTenantUser");
		transactionRest.getCreditorAccount().setUserId("tenant_toProfile");
		doAnswer(new CallbackAnswer<>(transactionRest)).when(transactionRestWrapper).getTransaction(any(), any());

		try (TestCallback<GetTransactionResponse> callback = new TestCallback<>((response) -> {
			assertThat(response.getFromProfileId(), nullValue());
			assertThat(response.getToProfileId(), equalTo("toProfile"));
			assertThat(response.getTenantId(), equalTo("tenant"));
		})) {
			paymentManager.getTransaction(transactionRequestWithTenantId, callback);
		}
	}
	
	@Test
	public void testGetTransactionWithIncorrectReference() throws Exception {
		TransactionRest transactionRest = prepareTransactionRest();
		transactionRest.setReference("Not a JSON data");
		doAnswer(new CallbackAnswer<>(transactionRest)).when(transactionRestWrapper).getTransaction(any(), any());

		try (TestCallback<GetTransactionResponse> callback = new TestCallback<>(
				(response) -> assertEquals("Not a JSON data", response.getPaymentDetails().getAdditionalInfo()))) {
			paymentManager.getTransaction(transactionRequestWithTenantId, callback);
		}
	}
	
	@Test
	public void testGetTransactionWithEmptyReference() throws Exception {
		TransactionRest transactionRest = prepareTransactionRest();
		transactionRest.setReference("");
		doAnswer(new CallbackAnswer<>(transactionRest)).when(transactionRestWrapper).getTransaction(any(), any());

		try (TestCallback<GetTransactionResponse> callback = new TestCallback<>(
				(response) -> assertNotNull(response.getPaymentDetails()))) {
			paymentManager.getTransaction(transactionRequestWithTenantId, callback);
		}
	}
	
	private static ZonedDateTime prepareTransactionDate() {
		return PaymentManagerMockImpl.createDate(2017, 01, 11, 14, 37, 59);
	}

	private void assertExpectedTransactionResponse(TransactionRest transactionRest, GetTransactionResponse response) {
		assertThat(response.getId(), equalTo(transactionRest.getId()));
		assertThat(response.getTenantId(), equalTo(tenantId));
		assertThat(response.getFromProfileId(), equalTo("fromProfile"));
		assertThat(response.getToProfileId(), nullValue());
		assertThat(response.getFromCurrency(), equalTo(Currency.MONEY));
		assertThat(response.getToCurrency(), equalTo(Currency.BONUS));
		assertThat(response.getCreationDate(), equalTo(prepareTransactionDate()));
		assertThat(response.getAmount(), comparesEqualTo(transactionRest.getValue()));
		assertThat(response.getType(), equalTo("TRANSFER"));
		assertThat(response.getPaymentDetails().getAppId(), equalTo("appId"));
		assertThat(response.getPaymentDetails().getGameId(), equalTo("1"));
		assertThat(response.getPaymentDetails().getWinningComponentId(), nullValue());
		assertThat(response.getPaymentDetails().getAdditionalInfo(), equalTo("none"));
	}

	private TransactionRest prepareTransactionRest() {
		TransactionRest transactionRest = new TransactionRest();
		transactionRest.setId("expectedId");
		transactionRest.setDebitorAccount(new AccountRestBuilder()
				.currency(prepareCurrencyRest(ExternalTestCurrency.EUR))
				.userId("fromTenant_fromProfile").build());
		transactionRest.setCreditorAccount(new AccountRestBuilder()
				.currency(prepareCurrencyRest(ExternalTestCurrency.BONUS))
				.userId(tenantId).build());
		transactionRest.setValue(new BigDecimal("12.34"));
		transactionRest.setCreated(prepareTransactionDate());
		transactionRest.setType(TransactionType.TRANSFER);
		transactionRest.setReference(preparePaymentDetailsWithAppIdAndAdditionalInfo());
		return transactionRest;
	}

	@Test
	public void testGetAccountHistory() throws Exception {
		GetAccountHistoryRequest request = EnhancedRandom.random(GetAccountHistoryRequest.class);
		request.setCurrency(Currency.MONEY);
		request.setOffset(7);
		PaymentClientInfo paymentClientInfo = new PaymentClientInfoImpl(tenantId, profileId, appId);

		AccountHistoryRest restResponse = EnhancedRandom.random(AccountHistoryRest.class);
		TransactionRest transactionRest = prepareTransactionRest();
		restResponse.setData(Arrays.asList(transactionRest));
		when(accountRestWrapper.getAccountHistory(any())).thenReturn(restResponse);

		AccountRest account = createAccount(ExternalTestCurrency.EUR, "-1", "-1");
		when(userRestWrapper.getUserActiveAccounts(paymentUserId)).thenReturn(Arrays.asList(account));

		GetAccountHistoryResponse response = paymentManager.getAccountHistory(paymentClientInfo, request);

		ArgumentCaptor<AccountHistoryRestSearchParams> argument = ArgumentCaptor.forClass(AccountHistoryRestSearchParams.class);
		verify(accountRestWrapper).getAccountHistory(argument.capture());
		assertThat(argument.getValue().getOffset(), equalTo(request.getOffset()));
		
		assertThat(response.getLimit(), equalTo(restResponse.getLimit()));
		assertThat(response.getOffset(), equalTo(restResponse.getOffset()));
		assertThat(response.getTotal(), equalTo(restResponse.getTotal()));
		assertThat(response.getItems().size(), equalTo(restResponse.getData().size()));
		assertExpectedTransactionResponse(transactionRest, response.getItems().get(0));
	}

	private AccountRest createAccount(ExternalTestCurrency currency, String id, String currencyId) {
		//This is a copy from PaymentManagerImplTest. Move this method to some kind of test util?
		return new AccountRestBuilder().currency(prepareCurrencyRest(currency)).id(id).currencyId(currencyId).build();
	}
	
	@Test
	public void testLoadOrWithdrawWithoutCoverage() throws Exception {
		CurrencyRest currencyBonus = prepareCurrencyRest(ExternalTestCurrency.BONUS);
		
		String expectedFromAccountId = "fromAccountId";
		AccountRest fromAccount = new AccountRestBuilder().id(expectedFromAccountId).currency(currencyBonus).build();
		when(userRestWrapper.getUserActiveAccounts("tenantId_profileId")).thenReturn(Arrays.asList(fromAccount));
		
		String trId = "transactionResultId";
		TransactionRest transactionRest = new TransactionRest();
		transactionRest.setId(trId);
		doAnswer(new CallbackAnswer<>(transactionRest)).when(transactionRestWrapper)
				.loadOntoOrWithdrawFromAccount(any(), any(), any(), any());
				
		LoadOrWithdrawWithoutCoverageRequest request = new LoadOrWithdrawWithoutCoverageRequest();
		request.setTenantId("tenantId");
		request.setProfileId("profileId");
		request.setCurrency(Currency.BONUS);
		request.setAmount(new BigDecimal("1.01"));
		request.setPaymentDetails(new PaymentDetailsBuilder().additionalInfo("Withdraw").gameId("1").appId("appId").build());
		try (TestCallback<TransactionId> callback = new TestCallback<>(
				(response) -> assertEquals(trId, response.getTransactionId()))) {
			paymentManager.loadOrWithdrawWithoutCoverage(request, callback, clientInfo);
		}
	}

	@Test
	public void testMergeUsers() throws Exception {
		MergeUsersRequest request = new MergeUsersRequest();
		request.setTenantId(tenantId);
		request.setFromProfileId(UUID.randomUUID().toString());
		request.setToProfileId(UUID.randomUUID().toString());
		
		CurrencyRest currencyBonus = prepareCurrencyRest(ExternalTestCurrency.BONUS);
		String expectedFromAccountId = "accountId";
		AccountRest account = new AccountRestBuilder().id(expectedFromAccountId).currency(currencyBonus).balance(BigDecimal.ONE).build();
		when(userAccountManagerImpl.findActiveAccounts(tenantId, request.getFromProfileId())).thenReturn(Arrays.asList(account));
		when(userAccountManagerImpl.findAllAccounts(tenantId, request.getToProfileId())).thenReturn(Arrays.asList(account));
		when(userAccountManagerImpl.findAccountFromListByExtenalCurrency(Arrays.asList(account), tenantId,
				request.getToProfileId(), currencyBonus)).thenReturn(account);
		when(userRestWrapper.disableUser("tenantId_"+request.getFromProfileId())).thenReturn(null);
		
		paymentManager.mergeUserAccounts(request);
		
		ArgumentCaptor<TransactionRestInsert> argument = ArgumentCaptor.forClass(TransactionRestInsert.class);
		verify(transactionRestWrapper).transferBetweenAccounts(argument.capture());
		assertThat(argument.getValue().getDebitorAccountId(), equalTo("accountId"));
		assertThat(argument.getValue().getCreditorAccountId(), equalTo("accountId"));
		assertThat(argument.getValue().getValue(), comparesEqualTo(BigDecimal.ONE));
		assertThat(argument.getValue().getType(), equalTo(TransactionType.TRANSFER));
	}
	
	@Test
	public void testGetExchangeRates() throws Exception {
		TenantInfo tenantInfo = new TenantInfo();
		tenantInfo.setTenantId("1");
		List<ExchangeRate> exchangeRates = new ArrayList<ExchangeRate>();
		ExchangeRate exchangeRate = new ExchangeRate();
		exchangeRate.setFromCurrency("EUR");
		exchangeRate.setToCurrency("CREDIT");
		exchangeRate.setFromAmount(new BigDecimal("1"));
		exchangeRate.setToAmount(new BigDecimal("10"));
		exchangeRates.add(exchangeRate);
		tenantInfo.setExchangeRates(exchangeRates);
		when(tenantAerospikeDao.getTenantInfo("1")).thenReturn(tenantInfo);
		GetExchangeRatesResponse response = paymentManager.getExchangeRates(new PaymentClientInfoImpl("1", null, appId));

		assertThat(response.getExchangeRates().get(0).getFromCurrency(), equalTo(tenantInfo.getExchangeRates().get(0).getFromCurrency()));
		assertThat(response.getExchangeRates().get(0).getToCurrency(), equalTo(tenantInfo.getExchangeRates().get(0).getToCurrency()));
		assertThat(response.getExchangeRates().get(0).getFromAmount(), equalTo(tenantInfo.getExchangeRates().get(0).getFromAmount()));
		assertThat(response.getExchangeRates().get(0).getToAmount(), equalTo(tenantInfo.getExchangeRates().get(0).getToAmount()));
	}

	private String preparePaymentDetailsWithAppIdAndAdditionalInfo() {
		return "{\"appId\":\"appId\",\"gameId\":\"1\",\"additionalInfo\":\"none\"}";
	}
}