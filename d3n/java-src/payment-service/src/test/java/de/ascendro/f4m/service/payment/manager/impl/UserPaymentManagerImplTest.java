package de.ascendro.f4m.service.payment.manager.impl;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.auth.model.register.SetUserRoleRequest;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.json.model.user.UserRole;
import de.ascendro.f4m.service.payment.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.dao.PendingIdentificationAerospikeDao;
import de.ascendro.f4m.service.payment.manager.CurrencyManager;
import de.ascendro.f4m.service.payment.manager.PaymentUserIdCalculator;
import de.ascendro.f4m.service.payment.manager.TransactionLogCacheManager;
import de.ascendro.f4m.service.payment.manager.UserAccountManager;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.PaymentClientInfo;
import de.ascendro.f4m.service.payment.model.TransactionLog;
import de.ascendro.f4m.service.payment.model.TransactionStatus;
import de.ascendro.f4m.service.payment.model.external.ExchangeRate;
import de.ascendro.f4m.service.payment.model.external.GetExternalPaymentRequest;
import de.ascendro.f4m.service.payment.model.external.GetExternalPaymentResponse;
import de.ascendro.f4m.service.payment.model.external.GetIdentificationResponse;
import de.ascendro.f4m.service.payment.model.external.IdentificationMethod;
import de.ascendro.f4m.service.payment.model.external.InitExternalPaymentRequest;
import de.ascendro.f4m.service.payment.model.external.InitExternalPaymentResponse;
import de.ascendro.f4m.service.payment.model.external.InitIdentificationRequest;
import de.ascendro.f4m.service.payment.model.external.InitIdentificationResponse;
import de.ascendro.f4m.service.payment.model.external.PaymentTransactionType;
import de.ascendro.f4m.service.payment.rest.model.IdentificationInitializationRest;
import de.ascendro.f4m.service.payment.rest.model.IdentificationResponseRest;
import de.ascendro.f4m.service.payment.rest.model.IdentityRest;
import de.ascendro.f4m.service.payment.rest.model.PaymentTransactionInitializationRest;
import de.ascendro.f4m.service.payment.rest.model.PaymentTransactionRest;
import de.ascendro.f4m.service.payment.rest.wrapper.IdentificationRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.PaymentTransactionRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.UserRestWrapper;
import de.ascendro.f4m.service.payment.server.PaymentErrorCallback;
import de.ascendro.f4m.service.payment.server.PaymentSuccessCallback;
import de.ascendro.f4m.service.payment.session.PaymentClientInfoImpl;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.util.DateTimeUtil;
import io.github.benas.randombeans.api.EnhancedRandom;

public class UserPaymentManagerImplTest {
	@Spy
	private PaymentConfig paymentConfig = new PaymentConfig();
	@Mock
	private UserAccountManager userAccountManager;
	@Mock
	private IdentificationRestWrapper identificationRestWrapper;
	@Mock
	private PaymentTransactionRestWrapper paymentTransactionRestWrapper;
	@Mock
	private UserRestWrapper userRestWrapper;
	@Mock
	private TransactionLogAerospikeDao transactionLogAerospikeDao;
	@Mock
	private TransactionLogCacheManager transactionLogCacheManager;
	@Mock
	private DependencyServicesCommunicator dependencyServicesCommunicator;
	@Mock
	private CurrencyManager currencyManager;
	@Mock
	private PaymentSuccessCallback paymentSuccessCallback;
	@Mock
	private PaymentErrorCallback paymentErrorCallback;
	@Mock
	private ExchangeRate exchangeRate;
	@Mock
	PendingIdentificationAerospikeDao pendingIdentificationAerospikeDao;
	
	private GsonProvider gsonProvider;
	private UserPaymentManagerImpl userPaymentManager;
	
	private final String tenantId = "tenant";
	private final String profileId = "profile";
	private final String paymentUserId = PaymentUserIdCalculator.calcPaymentUserId(tenantId, profileId);

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		gsonProvider = new GsonProvider();
		userPaymentManager = new UserPaymentManagerImpl(paymentConfig, (id) -> identificationRestWrapper,
				(id) -> paymentTransactionRestWrapper, (id) -> userRestWrapper, userAccountManager,
				transactionLogAerospikeDao, transactionLogCacheManager, dependencyServicesCommunicator, currencyManager,
				gsonProvider, paymentSuccessCallback, paymentErrorCallback, pendingIdentificationAerospikeDao);
	}
	
	@Test
	public void testInitiateIdentification() throws Exception {
		when(identificationRestWrapper.getForwardUriBase()).thenReturn("www.payments.com");
		when(paymentConfig.getProperty(PaymentConfig.EXTERNAL_HOSTNAME)).thenReturn("www.f4m.rulez");
		IdentificationResponseRest identificationMockResponse = new IdentificationResponseRest();
		identificationMockResponse.setId("123456");
		when(identificationRestWrapper.startUserIdentificationToGetForwardURL(any())).thenReturn(identificationMockResponse);
		
		PaymentClientInfo paymentClientInfo = new PaymentClientInfoImpl("tenant", "profile", "appId");
		
		InitIdentificationRequest request = EnhancedRandom.random(InitIdentificationRequest.class);
		request.setMethod(IdentificationMethod.ID);
		InitIdentificationResponse response = userPaymentManager.initiateIdentification(paymentClientInfo, request);
		assertThat(response.getIdentificationId(), equalTo(identificationMockResponse.getId()));
		assertThat(response.getForwardUrl(), equalTo("www.payments.com/identification/index/123456"));
		
		//verify input to REST service
		ArgumentCaptor<IdentificationInitializationRest> argument = ArgumentCaptor.forClass(IdentificationInitializationRest.class);
		verify(identificationRestWrapper).startUserIdentificationToGetForwardURL(argument.capture());
		assertThat(argument.getValue().getMethod(), equalTo(request.getMethod()));
		assertThat(argument.getValue().getUserId(), equalTo(paymentClientInfo.getTenantId() + "_" + paymentClientInfo.getProfileId()));
		assertThat(argument.getValue().getCallbackUrlSuccess(),
				equalTo("https://www.f4m.rulez:8443/callback/identificationSuccess"));
		assertThat(argument.getValue().getCallbackUrlError(),
				equalTo("https://www.f4m.rulez:8443/callback/identificationError"));
	}
	
	@Test
	public void testForwardUriBuilding() throws Exception {
		when(paymentTransactionRestWrapper.getForwardUriBase()).thenReturn("http://super.host");
		assertEquals("http://super.host/someId",
				userPaymentManager.buildForwardURI(paymentTransactionRestWrapper, "someId").toString());
		when(paymentTransactionRestWrapper.getForwardUriBase()).thenReturn("http://super.host/");
		assertEquals("http://super.host/someId",
				userPaymentManager.buildForwardURI(paymentTransactionRestWrapper, "someId").toString());
		assertEquals("http://super.host/identification/index/someId",
				userPaymentManager.buildForwardURI(paymentTransactionRestWrapper, "identification", "/index", "someId").toString());
	}
	
	@Test
	public void testInitiateExternalPaymentToPayout() throws Exception {
		String logId = "logId";
		when(transactionLogAerospikeDao.createTransactionLog(any())).thenReturn(logId);
		when(paymentTransactionRestWrapper.getForwardUriBase()).thenReturn("www.payments.com");
		when(paymentConfig.getProperty(PaymentConfig.EXTERNAL_HOSTNAME)).thenReturn("www.f4m.rulez");
		String accountId = "AccountIdEUR";
		PaymentClientInfo paymentClientInfo = new PaymentClientInfoImpl("tenant", "profile", "appId");
		when(userAccountManager.findAccountIdFromAllAccounts(eq(paymentClientInfo.getTenantId()), eq(paymentClientInfo.getProfileId()), any())).thenReturn(accountId);
		
		PaymentTransactionRest transactionFromPayment = new PaymentTransactionRest();
		String transactionId = "654321";
		transactionFromPayment.setId(transactionId);
		String paymentToken = "ncvoeiwnfowivnoewrovewn";
		transactionFromPayment.setPaymentToken(paymentToken);
		when(paymentTransactionRestWrapper.startPaymentTransactionToGetForwardURL(any())).thenReturn(transactionFromPayment);
		when(currencyManager.getTenantExchangeRateByFromAmount(any(), any(), any(), any())).thenReturn(exchangeRate);
		when(currencyManager.getRate(exchangeRate)).thenReturn(new BigDecimal("10.00"));
		
		InitExternalPaymentRequest request = EnhancedRandom.random(InitExternalPaymentRequest.class);
		request.setAmount(new BigDecimal("-12.34"));
		request.setCurrency(Currency.CREDIT);
		request.setDescription("Short description");
		InitExternalPaymentResponse response = userPaymentManager.initiateExternalPayment(paymentClientInfo, request);
		//verify output
		assertThat(response.getTransactionId(), equalTo(transactionId));
		assertThat(response.getPaymentToken(), equalTo(paymentToken));
		assertNull(response.getForwardUrl());
		
		//verify input to REST service
		ArgumentCaptor<PaymentTransactionInitializationRest> argument = ArgumentCaptor.forClass(PaymentTransactionInitializationRest.class);
		verify(paymentTransactionRestWrapper).startPaymentTransactionToGetForwardURL(argument.capture());
		assertThat(argument.getValue().getAccountId(), equalTo(accountId));
		assertThat(argument.getValue().getReference(), equalTo("{\"additionalInfo\":\"" + request.getDescription() + "\"}"));
		assertThat(argument.getValue().getType(), equalTo(PaymentTransactionType.DEBIT));
		assertThat(argument.getValue().getValue(), equalTo(new BigDecimal("+12.34")));
		assertThat(argument.getValue().getCallbackUrlSuccess(), equalTo("https://www.f4m.rulez:8443/callback/paymentSuccess"));
		assertThat(argument.getValue().getCallbackUrlError(), equalTo("https://www.f4m.rulez:8443/callback/paymentError"));
		assertThat(argument.getValue().getRedirectUrlSuccess(), equalTo(request.getRedirectUrlSuccess()));
		assertThat(argument.getValue().getRedirectUrlError(), equalTo(request.getRedirectUrlError()));
		assertThat(argument.getValue().getRate(), equalTo(new BigDecimal("10.00")));
		assertThat(argument.getValue().getCashoutData().getBeneficiary(), equalTo(request.getCashoutData().getBeneficiary()));

		ArgumentCaptor<TransactionLog> logArg = ArgumentCaptor.forClass(TransactionLog.class);
		verify(transactionLogAerospikeDao).createTransactionLog(logArg.capture());
		assertThat(logArg.getValue().getStatus(), equalTo(TransactionStatus.INITIATED));
		assertThat(logArg.getValue().getCurrency(), equalTo(Currency.MONEY));
		assertThat(logArg.getValue().getCurrencyTo(), equalTo(Currency.CREDIT));
		assertThat(logArg.getValue().getAmount(), equalTo(new BigDecimal("-12.34")));
		verify(transactionLogAerospikeDao).updateTransactionLog(logId, transactionFromPayment.getId(),
				TransactionStatus.PROCESSING);
		
		verify(transactionLogCacheManager).put(transactionFromPayment.getId(), logId);
		verify(paymentSuccessCallback).onPaymentSuccess(transactionId, false);
	}
	
	@Test
	public void testInitiateExternalPaymentToPayoutFailure() throws Exception {
		String expectedMessage = "Expected exception";
		doThrow(new RuntimeException(expectedMessage)).when(paymentTransactionRestWrapper)
				.startPaymentTransactionToGetForwardURL(any());
		String logId = "logId";
		when(transactionLogAerospikeDao.createTransactionLog(any())).thenReturn(logId);

		PaymentClientInfo paymentClientInfo = new PaymentClientInfoImpl("tenant", "profile", "appId");
		InitExternalPaymentRequest request = EnhancedRandom.random(InitExternalPaymentRequest.class);
		request.setAmount(new BigDecimal("-12.34"));
		request.setCurrency(Currency.CREDIT);
		try {
			userPaymentManager.initiateExternalPayment(paymentClientInfo, request);
			fail();
		} catch (RuntimeException e) {
			assertEquals(expectedMessage, e.getMessage());
			verify(paymentErrorCallback).onPaymentError(logId, null);
		}
	}

	@Test
	public void testGetIdentification() throws Exception {
		PaymentClientInfo paymentClientInfo = new PaymentClientInfoImpl("tenant", "profile", "appId");
		IdentityRest identityRest = EnhancedRandom.random(IdentityRest.class);
		when(userRestWrapper.getUserIdentity("tenant_profile")).thenReturn(identityRest);
		GetIdentificationResponse identification = userPaymentManager.getIdentification(paymentClientInfo);
		assertEquals(identityRest.getCity(), identification.getCity());
		assertEquals(identityRest.getCountry(), identification.getCountry());
		assertEquals(identityRest.getDateOfBirth(), identification.getDateOfBirth());
		assertEquals(identityRest.getFirstName(), identification.getFirstName());
		assertEquals(identityRest.getId(), identification.getId());
		assertEquals(identityRest.getName(), identification.getName());
		assertEquals(identityRest.getPlaceOfBirth(), identification.getPlaceOfBirth());
		assertEquals(identityRest.getStreet(), identification.getStreet());
		assertEquals(identityRest.getType(), identification.getType());
		assertEquals(identityRest.getZip().toString(), identification.getZip());
	}
	
	@Test
	public void testGetExternalPayment() throws Exception {
		PaymentClientInfo paymentClientInfo = new PaymentClientInfoImpl("tenant", "profile", "appId");
		GetExternalPaymentRequest request = new GetExternalPaymentRequest();
		request.setTransactionId("1111");
		PaymentTransactionRest transactionRest = EnhancedRandom.random(PaymentTransactionRest.class);
		when(paymentTransactionRestWrapper.getPaymentTransaction(request.getTransactionId())).thenReturn(transactionRest );
		GetExternalPaymentResponse externalPayment = userPaymentManager.getExternalPayment(paymentClientInfo, request);
		assertThat(externalPayment.getAmount(), comparesEqualTo(transactionRest.getValue()));
		assertEquals(transactionRest.getCreated(), externalPayment.getCreated());
		assertEquals(transactionRest.getReference(), externalPayment.getDescription());
		assertEquals(transactionRest.getPaymentToken(), externalPayment.getPaymentToken());
		assertEquals(transactionRest.getProcessed(), externalPayment.getProcessed());
		assertEquals(transactionRest.getState(), externalPayment.getState());
		assertEquals(transactionRest.getId(), externalPayment.getTransactionId());
		assertEquals(transactionRest.getType(), externalPayment.getType());
	}
	
	@Test
	public void testIdentityInformationSynchronization() {
		paymentConfig.setProperty(PaymentConfig.IDENTIFICATION_SCHEDULE_TIMEOUT, 0);
		IdentityRest identityRest = EnhancedRandom.random(IdentityRest.class);
		identityRest.setDateOfBirth(DateTimeUtil.getCurrentDateTime().minusYears(18).minusDays(1));
		when(userRestWrapper.getUserIdentity(paymentUserId)).thenReturn(identityRest);
		userPaymentManager.synchronizeIdentityInformation(paymentUserId);
		ArgumentCaptor<Profile> argument = ArgumentCaptor.forClass(Profile.class);
		verify(dependencyServicesCommunicator).requestUpdateProfileIdentity(any(), argument.capture());
		assertThat(argument.getValue().getPersonWrapper().getFirstName(), equalTo(identityRest.getFirstName()));
		assertThat(argument.getValue().getPersonWrapper().getLastName(), equalTo(identityRest.getName()));
		assertThat(argument.getValue().getAddress().getCity(), equalTo(identityRest.getCity()));
		assertThat(argument.getValue().getAddress().getPostalCode(), equalTo(identityRest.getZip().toString()));
		
		ArgumentCaptor<SetUserRoleRequest> argumentRole = ArgumentCaptor.forClass(SetUserRoleRequest.class);
		verify(dependencyServicesCommunicator).updateUserRoles(argumentRole.capture());
		assertThat(argumentRole.getValue().getRolesToAdd()[0], equalTo(UserRole.FULLY_REGISTERED_BANK_O18.toString()));
		assertThat(argumentRole.getValue().getUserId(), equalTo(profileId));
	}
}