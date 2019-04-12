package de.ascendro.f4m.service.payment.integration;

import static de.ascendro.f4m.service.payment.manager.PaymentTestUtil.mapToList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URI;
import java.text.ParseException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import javax.inject.Singleton;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPoolImpl;
import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.test.F4MServiceIntegrationTestBase;
import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.PaymentServiceStartup;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.dao.PendingIdentification;
import de.ascendro.f4m.service.payment.dao.PendingIdentificationAerospikeDao;
import de.ascendro.f4m.service.payment.di.GameManagerProvider;
import de.ascendro.f4m.service.payment.di.PaymentDefaultMessageTypeMapper;
import de.ascendro.f4m.service.payment.di.PaymentManagerProvider;
import de.ascendro.f4m.service.payment.di.UserAccountManagerProvider;
import de.ascendro.f4m.service.payment.di.UserPaymentManagerProvider;
import de.ascendro.f4m.service.payment.exception.F4MNoCurrencyRateException;
import de.ascendro.f4m.service.payment.manager.CurrencyManager;
import de.ascendro.f4m.service.payment.manager.ExternalTestCurrency;
import de.ascendro.f4m.service.payment.manager.GameManager;
import de.ascendro.f4m.service.payment.manager.PaymentManager;
import de.ascendro.f4m.service.payment.manager.PaymentTestUtil;
import de.ascendro.f4m.service.payment.manager.PaymentUserIdCalculator;
import de.ascendro.f4m.service.payment.manager.TransactionLogCacheManager;
import de.ascendro.f4m.service.payment.manager.UserAccountManager;
import de.ascendro.f4m.service.payment.manager.UserPaymentManager;
import de.ascendro.f4m.service.payment.manager.impl.GameManagerMockImpl;
import de.ascendro.f4m.service.payment.manager.impl.PaymentManagerMockImpl;
import de.ascendro.f4m.service.payment.manager.impl.UserAccountManagerMockImpl;
import de.ascendro.f4m.service.payment.manager.impl.UserPaymentManagerMockImpl;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.TransactionLog;
import de.ascendro.f4m.service.payment.model.TransactionStatus;
import de.ascendro.f4m.service.payment.model.config.InsertOrUpdateUserRequest;
import de.ascendro.f4m.service.payment.model.config.MergeUsersRequest;
import de.ascendro.f4m.service.payment.model.external.CashoutData;
import de.ascendro.f4m.service.payment.model.external.ConvertBetweenCurrenciesUserRequest;
import de.ascendro.f4m.service.payment.model.external.GetExternalPaymentRequest;
import de.ascendro.f4m.service.payment.model.external.IdentificationMethod;
import de.ascendro.f4m.service.payment.model.external.InitExternalPaymentRequest;
import de.ascendro.f4m.service.payment.model.external.InitIdentificationRequest;
import de.ascendro.f4m.service.payment.model.internal.CloseJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.CreateJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.GetAccountBalanceRequest;
import de.ascendro.f4m.service.payment.model.internal.GetAccountHistoryRequest;
import de.ascendro.f4m.service.payment.model.internal.GetJackpotRequest;
import de.ascendro.f4m.service.payment.model.internal.GetTransactionRequest;
import de.ascendro.f4m.service.payment.model.internal.GetUserAccountHistoryRequest;
import de.ascendro.f4m.service.payment.model.internal.LoadOrWithdrawWithoutCoverageRequest;
import de.ascendro.f4m.service.payment.model.internal.TransferBetweenAccountsRequest;
import de.ascendro.f4m.service.payment.model.internal.TransferJackpotRequest;
import de.ascendro.f4m.service.payment.model.schema.PaymentMessageSchemaMapper;
import de.ascendro.f4m.service.payment.server.IdentificationSuccessCallback;
import de.ascendro.f4m.service.payment.server.PaymentSuccessCallback;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageRequest;
import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;
import de.ascendro.f4m.service.usermessage.translation.Messages;
import de.ascendro.f4m.service.util.register.ServiceRegistryClientImpl;

public class PaymentServiceStartupTest extends F4MServiceIntegrationTestBase {
	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentServiceStartupTest.class);
	
	private static final String INIT_PROFILE_ID = "initProfileId";
	private static final String INIT_TENANT_ID = "initTenantId";
	private static final String INIT_APP_ID = "initAppId";

	private PaymentManager paymentManager;
	private GameManager gameManager;
	private UserPaymentManager userPaymentManager;
	private UserAccountManager userAccountManager;
	private TransactionLogAerospikeDao transactionLogAerospikeDao;
	private PendingIdentificationAerospikeDao pendingIdentificationAerospikeDao;
	private AnalyticsDaoImpl analyticsDaoImpl;
	private TransactionLogCacheManager transactionLogCache;
	private ServiceRegistryClientImpl serviceRegistryClient;
	private JsonWebSocketClientSessionPoolImpl jsonWebSocketClientSessionPool;
	private CurrencyManager currencyManager;
	private ClientInfo clientInfo;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		config.setProperty(PaymentConfig.MOCK_MODE, false);
		transactionLogCache = jettyServerRule.getServerStartup().getInjector()
				.getInstance(TransactionLogCacheManager.class);
		//Creating clientInfo with dummy data
		clientInfo = new ClientInfo();
		clientInfo.setAppId("1");
		clientInfo.setCountryCode(ISOCountry.DE);
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new PaymentServiceStartup(DEFAULT_TEST_STAGE) {
			@Override
			protected Module getModule() {
				return Modules.override(super.getModule()).with(new AbstractModule() {
					@Override
					protected void configure() {
						assertNull(paymentManager); //check if not initialised twice
						paymentManager = spy(PaymentManagerMockImpl.class); //prepares test data
						bind(PaymentManagerProvider.class).toInstance(new PaymentManagerProvider() {
							@Override
							public PaymentManager get() {
								return paymentManager;
							}
						});
						
						assertNull(gameManager); //check if not initialised twice
						gameManager = spy(GameManagerMockImpl.class); //prepares test data
						bind(GameManagerProvider.class).toInstance(new GameManagerProvider() {
							@Override
							public GameManager get() {
								return gameManager;
							}
						});

						assertNull(userPaymentManager); //check if not initialised twice
						userPaymentManager = spy(UserPaymentManagerMockImpl.class); //prepares test data
						bind(UserPaymentManagerProvider.class).toInstance(new UserPaymentManagerProvider() {
							@Override
							public UserPaymentManager get() {
								return userPaymentManager;
							}
						});
						
						assertNull(userAccountManager); //check if not initialised twice
						userAccountManager = spy(UserAccountManagerMockImpl.class); //prepares test data
						bind(UserAccountManagerProvider.class).toInstance(new UserAccountManagerProvider() {
							@Override
							public UserAccountManager get() {
								return userAccountManager;
							}
						});
						
						transactionLogAerospikeDao = mock(TransactionLogAerospikeDao.class);
						bind(TransactionLogAerospikeDao.class).toInstance(transactionLogAerospikeDao);

						pendingIdentificationAerospikeDao = mock(PendingIdentificationAerospikeDao.class);
						bind(PendingIdentificationAerospikeDao.class).toInstance(pendingIdentificationAerospikeDao);

						analyticsDaoImpl = mock(AnalyticsDaoImpl.class);
						bind(AnalyticsDaoImpl.class).toInstance(analyticsDaoImpl);

						
						serviceRegistryClient = mock(ServiceRegistryClientImpl.class);
						bind(ServiceRegistryClientImpl.class).toInstance(serviceRegistryClient);
						
						jsonWebSocketClientSessionPool = mock(JsonWebSocketClientSessionPoolImpl.class);
						bind(JsonWebSocketClientSessionPool.class).toInstance(jsonWebSocketClientSessionPool);
						
						currencyManager = mock(CurrencyManager.class);
						bind(CurrencyManager.class).toInstance(currencyManager);
						bind(AerospikeClientProvider.class).to(AerospikeClientMockProvider.class).in(Singleton.class);
					}
				});
			}
		};
	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(PaymentDefaultMessageTypeMapper.class, PaymentMessageSchemaMapper.class);
	}

	@Test
	public void testTransferBetweenAccounts() throws Exception {
		verifyResponseOnRequest("TransferBetweenAccountsRequest.json", "TransferBetweenAccountsResponse.json");
		ArgumentCaptor<TransferBetweenAccountsRequest> argument = ArgumentCaptor
				.forClass(TransferBetweenAccountsRequest.class);
		verify(paymentManager).transferBetweenAccounts(argument.capture(), any(), any());
		assertThat(argument.getValue().getAmount(), comparesEqualTo(new BigDecimal("12.34")));
		assertThat(argument.getValue().getCurrency(), equalTo(Currency.MONEY));
		assertThat(argument.getValue().getPaymentDetails(), notNullValue());
		assertThat(argument.getValue().getPaymentDetails().getAppId(), equalTo("appId"));
		assertThat(argument.getValue().getPaymentDetails().getGameId(), nullValue());
		assertThat(argument.getValue().getPaymentDetails().getAdditionalInfo(), equalTo("no"));
	}

	@Test
	public void testInsertOrUpdateUser() throws Exception {
		verifyResponseOnRequest("InsertOrUpdateUserRequest.json", "InsertOrUpdateUserResponse.json");
		ArgumentCaptor<InsertOrUpdateUserRequest> argument = ArgumentCaptor.forClass(InsertOrUpdateUserRequest.class);
		verify(userAccountManager).insertOrUpdateUser(argument.capture(), any());
		assertThat(argument.getValue().getTenantId(), equalTo("Tenant"));
		assertThat(argument.getValue().getProfileId(), equalTo("Profile"));
		assertThat(argument.getValue().getProfile().isJsonObject(), equalTo(true));
		Profile profile = new Profile(argument.getValue().getProfile().getAsJsonObject());
		assertThat(profile.getPersonWrapper().getFirstName(), equalTo("Name"));
		assertThat(profile.getPersonWrapper().getLastName(), equalTo("Surname"));
	}

	@Test
	public void testMergeUsers() throws Exception {
		verifyResponseOnRequest("MergeUsersRequest.json", "MergeUsersResponse.json");
		ArgumentCaptor<MergeUsersRequest> argument = ArgumentCaptor.forClass(MergeUsersRequest.class);
		verify(paymentManager).mergeUserAccounts(argument.capture());
		assertThat(argument.getValue().getTenantId(), equalTo("Tenant"));
		assertThat(argument.getValue().getFromProfileId(), equalTo("Profile"));
		assertThat(argument.getValue().getToProfileId(), equalTo("Profile2"));
	}

	@Test
	public void testGetTransaction() throws Exception {
		verifyResponseOnRequest("GetTransactionRequest.json", "GetTransactionResponse.json");
		ArgumentCaptor<GetTransactionRequest> argument = ArgumentCaptor.forClass(GetTransactionRequest.class);
		verify(paymentManager).getTransaction(argument.capture(), any());
		assertThat(argument.getValue().getTenantId(), equalTo("expectedTenantId"));
		assertThat(argument.getValue().getTransactionId(), equalTo("expectedTransactionId"));
	}
	
	@Test
	public void testAccountHistory() throws Exception {
		verifyResponseOnRequest("GetAccountHistoryRequest.json", "GetAccountHistoryResponse.json");
		GetUserAccountHistoryRequest request = verifyAccountHistoryCallParams();
		assertThat(request, instanceOf(GetAccountHistoryRequest.class));
	}
	
	@Test
	public void testUserAccountHistory() throws Exception {
		verifyResponseOnRequest("GetUserAccountHistoryRequest.json", "GetUserAccountHistoryResponse.json");
		verifyAccountHistoryCallParams();
	}

	private GetUserAccountHistoryRequest verifyAccountHistoryCallParams() throws ParseException {
		ArgumentCaptor<PaymentClientInfo> clientInfoArg = ArgumentCaptor.forClass(PaymentClientInfo.class);
		ArgumentCaptor<GetUserAccountHistoryRequest> argument = ArgumentCaptor.forClass(GetUserAccountHistoryRequest.class);
		verify(paymentManager).getAccountHistory(clientInfoArg.capture(), argument.capture());
		assertThat(clientInfoArg.getValue().getTenantId(), equalTo("searchTenant"));
		assertThat(clientInfoArg.getValue().getProfileId(), equalTo("searchProfile"));
		GetUserAccountHistoryRequest request = argument.getValue();
		assertThat(request.getCurrency(), equalTo(Currency.MONEY));
		assertThat(request.getOffset(), nullValue());
		assertThat(request.getLimit(), equalTo(6));
		assertThat(request.getStartDate(), comparesEqualTo(ZonedDateTime.of(2016, 11, 24, 0, 0, 0, 0, ZoneOffset.UTC)));
		assertThat(request.getEndDate(), comparesEqualTo(ZonedDateTime.of(2016, 12, 01, 0, 0, 0, 0, ZoneOffset.UTC)));
		return request;
	}
	
	@Test
	public void testUserAccountHistoryWithIncorrectDate() throws Exception {
		//verifies, if validation error is returned despite the fact that 
		// everit is not able to validate optional ("type": [ "string", "null" ]) attributes with "format": "date-time"
		verifyResponseOnRequest("exceptions/GetUserAccountHistoryRequestIncorrectDate.json",
				"exceptions/GetUserAccountHistoryResponseValidationError.json", false);
	}
	
	@Test
	public void testUserAccountHistoryWithIncorrectSchema() throws Exception {
		verifyResponseOnRequest("exceptions/GetUserAccountHistoryRequestIncorrectSchema.json",
				"exceptions/GetUserAccountHistoryResponseValidationError.json", false);
	}
	
	@Test
	public void testReceivingNonJsonData() throws Exception {
		verifyResponseOnRequest("exceptions/non-json-message.txt", "exceptions/GeneralValidationErrorResponse.json", false);
	}
	
	@Test
	public void testUserAccountBalances() throws Exception {
		verifyResponseOnRequest("GetUserAccountBalancesRequest.json", "GetUserAccountBalancesResponse.json");

		ArgumentCaptor<PaymentClientInfo> clientArg = ArgumentCaptor.forClass(PaymentClientInfo.class);
		verify(userAccountManager).getAccountBalances(clientArg.capture());
		assertThat(clientArg.getValue().getTenantId(), equalTo("searchTenant"));
		assertThat(clientArg.getValue().getProfileId(), equalTo("searchProfile"));
		
	}

	@Test
	public void testAccountBalance() throws Exception {
		verifyResponseOnRequest("GetAccountBalanceRequest.json", "GetAccountBalanceResponse.json");
		
		ArgumentCaptor<PaymentClientInfo> clientArg = ArgumentCaptor.forClass(PaymentClientInfo.class);
		ArgumentCaptor<GetAccountBalanceRequest> balanceArg = ArgumentCaptor.forClass(GetAccountBalanceRequest.class);
		verify(userAccountManager).getAccountBalance(clientArg.capture(), balanceArg.capture());
		assertThat(balanceArg.getValue().getCurrency(), equalTo(Currency.MONEY));
		assertThat(clientArg.getValue().getTenantId(), equalTo("searchTenant"));
		assertThat(clientArg.getValue().getProfileId(), equalTo("searchProfile"));
	}
	
	@Test
	public void testGetExchangeRates() throws Exception {
		verifyResponseOnRequest("GetExchangeRatesRequest.json", "GetExchangeRatesResponse.json");
		ArgumentCaptor<PaymentClientInfo> clientArg = ArgumentCaptor.forClass(PaymentClientInfo.class);
		verify(paymentManager).getExchangeRates(clientArg.capture());
		assertThat(clientArg.getValue().getTenantId(), equalTo("searchTenant"));
	}

	@Test
	public void testGetIdentification() throws Exception {
		verifyResponseOnRequest("GetIdentificationRequest.json", "GetIdentificationResponse.json");
		ArgumentCaptor<PaymentClientInfo> clientArg = ArgumentCaptor.forClass(PaymentClientInfo.class);
		verify(userPaymentManager).getIdentification(clientArg.capture());
		assertThat(clientArg.getValue().getTenantId(), equalTo(INIT_TENANT_ID));
		assertThat(clientArg.getValue().getProfileId(), equalTo(INIT_PROFILE_ID));
	}
	
	@Test
	public void testConvertBetweenCurrenciesUser() throws Exception {
		verifyResponseOnRequest("ConvertBetweenCurrenciesUserRequest.json", "ConvertBetweenCurrenciesUserResponse.json");
		ArgumentCaptor<PaymentClientInfo> clientArg = ArgumentCaptor.forClass(PaymentClientInfo.class);
		ArgumentCaptor<ConvertBetweenCurrenciesUserRequest> argument = ArgumentCaptor.forClass(ConvertBetweenCurrenciesUserRequest.class);
		verify(paymentManager).convertBetweenCurrencies(clientArg.capture(), argument.capture(), any());
		assertThat(clientArg.getValue().getTenantId(), equalTo(INIT_TENANT_ID));
		assertThat(clientArg.getValue().getProfileId(), equalTo(INIT_PROFILE_ID));
		assertThat(argument.getValue().getFromCurrency(), equalTo(Currency.BONUS));
		assertThat(argument.getValue().getToCurrency(), equalTo(Currency.CREDIT));
		assertThat(argument.getValue().getAmount(), comparesEqualTo(new BigDecimal("54.32")));
		assertThat(argument.getValue().getDescription(), equalTo("Short and very optional description"));
	}
	
	@Test
	public void testConvertBetweenCurrenciesUserWhenMissingExchangeRate() throws Exception {
		doThrow(new F4MNoCurrencyRateException("No exchange rate from 10 EUR to BONUS")).when(paymentManager)
				.convertBetweenCurrencies(any(), any(), any());
		verifyResponseOnRequest("ConvertBetweenCurrenciesUserRequest.json", "ConvertBetweenCurrenciesUserError.json", false);
	}

	@Test
	public void testInitIdentification() throws Exception {
		verifyResponseOnRequest("InitIdentificationRequest.json", "InitIdentificationResponse.json");
		ArgumentCaptor<PaymentClientInfo> clientArg = ArgumentCaptor.forClass(PaymentClientInfo.class);
		ArgumentCaptor<InitIdentificationRequest> argument = ArgumentCaptor.forClass(InitIdentificationRequest.class);
		verify(userPaymentManager).initiateIdentification(clientArg.capture(), argument.capture());
		assertThat(clientArg.getValue().getTenantId(), equalTo(INIT_TENANT_ID));
		assertThat(clientArg.getValue().getProfileId(), equalTo(INIT_PROFILE_ID));
		assertThat(argument.getValue().getMethod(), equalTo(IdentificationMethod.POSTIDENT));
	}
	
	@Test
	public void testInitExternalPayment() throws Exception {
		verifyResponseOnRequest("InitExternalPaymentRequest.json", "InitExternalPaymentResponse.json");
		ArgumentCaptor<PaymentClientInfo> clientArg = ArgumentCaptor.forClass(PaymentClientInfo.class);
		ArgumentCaptor<InitExternalPaymentRequest> paymentArg = ArgumentCaptor.forClass(InitExternalPaymentRequest.class);
		verify(userPaymentManager).initiateExternalPayment(clientArg.capture(), paymentArg.capture());
		assertThat(clientArg.getValue().getTenantId(), equalTo("expectedTenantId"));
		assertThat(clientArg.getValue().getProfileId(), equalTo(INIT_PROFILE_ID));
		InitExternalPaymentRequest paymentRequest = paymentArg.getValue();
		assertThat(paymentRequest.getAmount(), comparesEqualTo(new BigDecimal("-12.34")));
		assertThat(paymentRequest.getCurrency(), equalTo(Currency.MONEY));
		assertThat(paymentRequest.getRedirectUrlError(), equalTo("pay.ment/error"));
		assertThat(paymentRequest.getRedirectUrlSuccess(), equalTo("pay.ment/success"));
		CashoutData cashoutData = paymentRequest.getCashoutData();
		assertThat(cashoutData.getBeneficiary(), equalTo("Identified Name"));
		assertThat(cashoutData.getIban(), equalTo("DE02120300000000202051"));
		assertThat(cashoutData.getBic(), equalTo("BYLADEM1001"));
	}

	@Test
	public void testGetExternalPayment() throws Exception {
		verifyResponseOnRequest("GetExternalPaymentRequest.json", "GetExternalPaymentResponse.json");
		ArgumentCaptor<PaymentClientInfo> clientArg = ArgumentCaptor.forClass(PaymentClientInfo.class);
		ArgumentCaptor<GetExternalPaymentRequest> paymentArg = ArgumentCaptor.forClass(GetExternalPaymentRequest.class);
		verify(userPaymentManager).getExternalPayment(clientArg.capture(), paymentArg.capture());
		assertThat(clientArg.getValue().getTenantId(), equalTo(INIT_TENANT_ID));
		assertThat(clientArg.getValue().getProfileId(), equalTo(INIT_PROFILE_ID));
		assertThat(paymentArg.getValue().getTransactionId(), equalTo("transactionIdValue"));
	}

	@Test
	public void testCallbacks() throws Exception {
		config.setProperty(PaymentConfig.IDENTIFICATION_SCHEDULE_TIMEOUT, 0);
		
		Client client = ClientBuilder.newBuilder()
                .hostnameVerifier((hostname, session) -> true) //ignore all certificate errors
                .build();

		when(analyticsDaoImpl.createAnalyticsEvent(any())).thenReturn("");
		
		//mock/spy should be created for all servlet, if we want to check if id's are really received
		ServiceConnectionInformation connectionInformation = new ServiceConnectionInformation();
		ExternalTestCurrency externalTestCurrency = ExternalTestCurrency.EUR;
		when(currencyManager.getCurrencyRestByCurrencyEnum("tenantId", Currency.MONEY))
				.thenReturn(PaymentTestUtil.prepareCurrencyRest(externalTestCurrency));
		when(serviceRegistryClient.getServiceConnectionInformation(UserMessageMessageTypes.SERVICE_NAME))
				.thenReturn(connectionInformation);
		when(transactionLogAerospikeDao.getTransactionLog(any()))
				.thenReturn(new TransactionLog("", "tenantId", INIT_PROFILE_ID,
						"gameId", "multiplayerGameInstanceId", "gameInstanceId",
						BigDecimal.ONE, Currency.MONEY, "reason", INIT_APP_ID));

		when(pendingIdentificationAerospikeDao.getPendingIdentification(INIT_TENANT_ID, INIT_PROFILE_ID))
				.thenReturn(new PendingIdentification(INIT_TENANT_ID, INIT_APP_ID, INIT_PROFILE_ID));
		
		String paydentUserId = PaymentUserIdCalculator.calcPaymentUserId(INIT_TENANT_ID, INIT_PROFILE_ID);
		SendWebsocketMessageRequest identificationSuccessPush = verifyGetCallback(client,
				PaymentConfig.IDENTIFICATION_SUCCESS_CONTEXT_PATH, IdentificationSuccessCallback.USER_ID, paydentUserId,
				Messages.PAYMENT_SYSTEM_IDENTIFICATION_SUCCESS_PUSH);
		assertNotNull(identificationSuccessPush.getPayload());
		assertEquals(WebsocketMessageType.IDENTIFICATION_SUCCESS, identificationSuccessPush.getType());
		assertEmptyJsonObject(identificationSuccessPush);

		SendWebsocketMessageRequest identificationFailurePush = verifyGetCallback(client,
				PaymentConfig.IDENTIFICATION_ERROR_CONTEXT_PATH, IdentificationSuccessCallback.USER_ID, paydentUserId,
				Messages.PAYMENT_SYSTEM_IDENTIFICATION_ERROR_PUSH);
		assertEquals(WebsocketMessageType.IDENTIFICATION_FAILURE, identificationFailurePush.getType());
		assertEmptyJsonObject(identificationFailurePush);

		String transactionId = "transactionIdValue";
		String faultyTransactionId = "transactionIdError";
		transactionLogCache.put(transactionId, "logId");
		transactionLogCache.put(faultyTransactionId, "logIdError");

		SendWebsocketMessageRequest paymentSuccessPush = verifyGetCallback(client,
				PaymentConfig.PAYMENT_SUCCESS_CONTEXT_PATH, PaymentSuccessCallback.TRANSACTION_ID, transactionId,
				Messages.PAYMENT_SYSTEM_PAYMENT_SUCCESS_PUSH);
		verify(transactionLogAerospikeDao).updateTransactionLog("logId", transactionId, TransactionStatus.COMPLETED);
		assertEquals(WebsocketMessageType.PAYMENT_SUCCESS, paymentSuccessPush.getType());
		verifyPaymentPushPayload(externalTestCurrency, transactionId, paymentSuccessPush);

		SendWebsocketMessageRequest paymentFailurePush = verifyGetCallback(client,
				PaymentConfig.PAYMENT_ERROR_CONTEXT_PATH, PaymentSuccessCallback.TRANSACTION_ID, faultyTransactionId,
				Messages.PAYMENT_SYSTEM_PAYMENT_ERROR_PUSH);
		verify(transactionLogAerospikeDao).updateTransactionLog("logIdError", faultyTransactionId,
				TransactionStatus.ERROR);
		assertEquals(WebsocketMessageType.PAYMENT_FAILURE, paymentFailurePush.getType());
		verifyPaymentPushPayload(externalTestCurrency, transactionId, paymentSuccessPush);
	}

    private SendWebsocketMessageRequest verifyGetCallback(Client client, String path, String param, String paramValue, String message) {
		reset(jsonWebSocketClientSessionPool);
		PaymentConfig paymentConfig = (PaymentConfig) config;
		URI uri = paymentConfig.getExternalCallbackForContext(path);
		LOGGER.info("Checking connection to callback {}", uri);
		WebTarget target = client.target(uri);
		target = target.queryParam(param, paramValue);
		Response response = target.request().get();
		assertEquals(Response.Status.OK.getStatusCode(), response.getStatusInfo().getStatusCode());
		response.close();
		
		ArgumentCaptor<ServiceConnectionInformation> connectionCaptor = ArgumentCaptor.forClass(ServiceConnectionInformation.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<JsonMessage<? extends JsonMessageContent>> messageCaptor = ArgumentCaptor.forClass(JsonMessage.class);
		RetriedAssert
				.assertWithWait(() -> verify(jsonWebSocketClientSessionPool)
						.sendAsyncMessage(connectionCaptor.capture(), messageCaptor.capture()));
		SendWebsocketMessageRequest wsRequest = (SendWebsocketMessageRequest) messageCaptor.getValue().getContent();
		assertEquals(message, wsRequest.getMessage());
		assertEquals(INIT_PROFILE_ID, wsRequest.getUserId());
		assertTrue(wsRequest.isLanguageAuto());
		return wsRequest;
	}
	
	private void verifyPaymentPushPayload(ExternalTestCurrency externalTestCurrency, String transactionId,
			SendWebsocketMessageRequest paymentSuccessPush) {
		assertEquals(transactionId, getPayloadAttribute(paymentSuccessPush, "transactionId"));
		assertEquals(BigDecimal.ONE.toString(), getPayloadAttribute(paymentSuccessPush, "amount"));
		assertEquals(externalTestCurrency.toString(), getPayloadAttribute(paymentSuccessPush, "currency"));
	}

	private void assertEmptyJsonObject(SendWebsocketMessageRequest identificationFailurePush) {
		assertTrue(identificationFailurePush.getPayload().isJsonObject());
		assertTrue(((JsonObject)identificationFailurePush.getPayload()).entrySet().isEmpty());
		assertTrue(identificationFailurePush.isLanguageAuto());
	}

    private String getPayloadAttribute(SendWebsocketMessageRequest directWebsocketPush, String memberName) {
        return ((JsonObject) directWebsocketPush.getPayload()).get(memberName).getAsString();
    }

	@Test
	public void testLoadOrWithdrawWithoutCoverage() throws Exception {
		verifyResponseOnRequest("LoadOrWithdrawWithoutCoverageRequest.json", "LoadOrWithdrawWithoutCoverageResponse.json");
		ArgumentCaptor<LoadOrWithdrawWithoutCoverageRequest> argument = ArgumentCaptor
				.forClass(LoadOrWithdrawWithoutCoverageRequest.class);
		verify(paymentManager).loadOrWithdrawWithoutCoverage(argument.capture(), any(), any());
		assertThat(argument.getValue().getAmount(), comparesEqualTo(new BigDecimal("1.01")));
		assertThat(argument.getValue().getCurrency(), equalTo(Currency.BONUS));
		assertThat(argument.getValue().getTenantId(), equalTo("tenant"));
		assertThat(argument.getValue().getProfileId(), equalTo("toProfile"));
		assertThat(argument.getValue().getPaymentDetails().getAdditionalInfo(), equalTo("The description"));
	}
	
	@Test
	public void testTransferJackpot() throws Exception {
		verifyResponseOnRequest("TransferJackpotRequest.json", "TransferJackpotResponse.json");
		ArgumentCaptor<TransferJackpotRequest> argument = ArgumentCaptor.forClass(TransferJackpotRequest.class);
		verify(gameManager).transferJackpot(argument.capture(),any());
		assertThat(argument.getValue().getTenantId(), equalTo("tenant"));
		assertThat(argument.getValue().getMultiplayerGameInstanceId(), equalTo("multiplayerGameInstanceIdentifier"));
		assertThat(argument.getValue().getFromProfileId(), equalTo("fromProfile"));
		assertThat(argument.getValue().getAmount(), comparesEqualTo(new BigDecimal("12.34")));
		assertThat(argument.getValue().getPaymentDetails(), notNullValue());
		assertThat(argument.getValue().getPaymentDetails().getAppId(), equalTo("appId"));
		assertThat(argument.getValue().getPaymentDetails().getGameId(), nullValue());
		assertThat(argument.getValue().getPaymentDetails().getAdditionalInfo(), equalTo("no"));
	}

	@Test
	public void testCreateJackpot() throws Exception {
		verifyResponseOnRequest("CreateJackpotRequest.json", "CreateJackpotResponse.json");
		ArgumentCaptor<CreateJackpotRequest> jackpotArgument = ArgumentCaptor.forClass(CreateJackpotRequest.class);
		verify(gameManager).createJackpot(jackpotArgument.capture());
		assertThat(jackpotArgument.getValue().getMultiplayerGameInstanceId(), equalTo("multiplayer_game_instance_id"));
		assertThat(jackpotArgument.getValue().getCurrency(), equalTo(Currency.MONEY));
	}

	@Test
	public void testGetJackpot() throws Exception {
		verifyResponseOnRequest("GetJackpotRequest.json", "GetJackpotResponse.json");
		ArgumentCaptor<GetJackpotRequest> argument = ArgumentCaptor.forClass(GetJackpotRequest.class);
		verify(gameManager).getJackpot(argument.capture());
		assertThat(argument.getValue().getMultiplayerGameInstanceId(), equalTo("multiplayer_game_instance_id"));
	}
	
	@Test
	public void testCloseJackpot() throws Exception {
		verifyResponseOnRequest("CloseJackpotRequest.json", "CloseJackpotResponse.json");
		ArgumentCaptor<CloseJackpotRequest> argument = ArgumentCaptor.forClass(CloseJackpotRequest.class);
		verify(gameManager).closeJackpot(argument.capture());
		assertThat(argument.getValue().getMultiplayerGameInstanceId(), equalTo("multiplayer_game_instance_id"));
		assertThat(argument.getValue().getPaymentDetails().getAdditionalInfo(),
				equalTo("Short and very optional description"));
		assertThat(mapToList(argument.getValue().getPayouts(), p -> p.getProfileId()),
				contains("profileId1", "profileId2"));
		assertThat(mapToList(argument.getValue().getPayouts(), p -> p.getAmount()),
				contains(new BigDecimal("12.34"), new BigDecimal("43.21")));
	}

	@Test
	public void testErrorForwardingToAdmin() throws Exception {
		String errorMessage = "Expected error occurred";
		doThrow(new F4MFatalErrorException(errorMessage)).when(paymentManager).getTransaction(any(), any());
		verifyResponseOnRequest("GetTransactionRequest.json", "GetTransactionError.json", false);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<JsonMessage<? extends JsonMessageContent>> messageCaptor = ArgumentCaptor.forClass(JsonMessage.class);
		verify(jsonWebSocketClientSessionPool).sendAsyncMessage(any(), messageCaptor.capture());
		SendEmailWrapperRequest emailToAdmin = (SendEmailWrapperRequest) messageCaptor.getValue().getContent();
		assertThat(emailToAdmin.getMessage(), containsString(errorMessage));
	}

}