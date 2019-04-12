package de.ascendro.f4m.service.payment.integration;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.integration.test.F4MServiceIntegrationTestBase;
import de.ascendro.f4m.service.payment.PaymentServiceStartup;
import de.ascendro.f4m.service.payment.callback.Callback;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.dao.TenantAerospikeDaoImpl;
import de.ascendro.f4m.service.payment.dao.TenantInfo;
import de.ascendro.f4m.service.payment.di.PaymentDefaultMessageTypeMapper;
import de.ascendro.f4m.service.payment.di.RestClientProvider;
import de.ascendro.f4m.service.payment.manager.PaymentManager;
import de.ascendro.f4m.service.payment.model.external.PaymentTransactionType;
import de.ascendro.f4m.service.payment.model.internal.GetTransactionRequest;
import de.ascendro.f4m.service.payment.model.internal.GetTransactionResponse;
import de.ascendro.f4m.service.payment.model.schema.PaymentMessageSchemaMapper;
import de.ascendro.f4m.service.payment.rest.model.CashoutDataRequest;
import de.ascendro.f4m.service.payment.rest.model.PaymentTransactionInitializationRest;
import de.ascendro.f4m.service.payment.rest.wrapper.RestClient;
import de.ascendro.f4m.service.payment.system.AccountTransactionsResource;
import de.ascendro.f4m.service.payment.system.PaymentSystemExternalResource;

public class PaymentIntegrationTest extends F4MServiceIntegrationTestBase {
	private TransactionLogAerospikeDao transactionLogAerospikeDao;
	private IAerospikeClient aerospikeClient;
	private List<String> restClientTenantIds;
	
	@Rule
	public PaymentSystemExternalResource paymentSystem = new PaymentSystemExternalResource();

	@Override
	public void setUp() throws Exception {
		super.setUp();
		
		config.setProperty(PaymentConfig.ADMIN_EMAIL, ""); //don't try to send an email, if test fails

		Injector injector = jettyServerRule.getServerStartup().getInjector();
		final AerospikeClientProvider aerospikeClientProvider = injector.getInstance(AerospikeClientProvider.class);
		aerospikeClient = aerospikeClientProvider.get();
		loadTenantData("/de/ascendro/f4m/service/payment/dao/tenantInfo_expectedTenantId.json", "expectedTenantId");
		loadTenantData("/de/ascendro/f4m/service/payment/dao/tenantInfo_tenant.json", "tenant");
		loadTenantData("/de/ascendro/f4m/service/payment/dao/tenantInfo_searchTenant.json", "searchTenant");
		restClientTenantIds = new ArrayList<>();
	}

	private void loadTenantData(String path, String tenantId) throws IOException {
		String namespace = AerospikeConfigImpl.AEROSPIKE_NAMESPACE_DEFAULT;
		String set = TenantAerospikeDaoImpl.TENANT_SET_NAME;
		String bin = TenantAerospikeDaoImpl.TENANT_BIN_NAME;
		try (InputStream is = this.getClass().getResourceAsStream(path)) {
			byte[] tenantData = IOUtils.toByteArray(is);
			aerospikeClient.put(null, new Key(namespace, set, TenantAerospikeDaoImpl.TENANT_KEY_PREFIX + tenantId),
					new Bin(bin, tenantData));
		}
	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(PaymentDefaultMessageTypeMapper.class, PaymentMessageSchemaMapper.class);
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new PaymentServiceStartup(DEFAULT_TEST_STAGE) {
			@Override
			public void register() throws F4MException, URISyntaxException {
				//do not register in non-existent service registry
			}
			
			@Override
			protected Module getModule() {
				return Modules.override(super.getModule()).with(new AbstractModule() {
					@Override
					protected void configure() {
						bind(AerospikeClientProvider.class).to(AerospikeClientMockProvider.class).in(Singleton.class);
						
						transactionLogAerospikeDao = mock(TransactionLogAerospikeDao.class);
						bind(TransactionLogAerospikeDao.class).toInstance(transactionLogAerospikeDao);
						
						bind(RestClientProvider.class).toInstance(new RestClientProvider(null, null, null) {
							@Override
							public RestClient get(String tenantId) {
								restClientTenantIds.add(tenantId);
								return new RestClient(paymentSystem.getJerseyTestServer().client(), new TenantInfo());
							}
						});
					}
				});
			}
		};
	}

	@Test
	public void testGetTransaction() throws Exception {
		verifyResponseOnRequest("GetTransactionRequest.json", "GetTransactionResponse.json");
	}
	
	@Test
	public void testIfGetTransactionIsAsynchronous() throws Exception {
		PaymentManager paymentManager = jettyServerRule.getServerStartup().getInjector().getInstance(PaymentManager.class);
		AtomicInteger success = new AtomicInteger();
		AtomicInteger failures = new AtomicInteger();
		CountDownLatch finalCountdown = new CountDownLatch(1);
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
		
		CountDownLatch callFinishedAndReturned = new CountDownLatch(1);
		when(paymentSystem.getAccountTransactionsResource().getTransation(any())).thenAnswer((invocation) -> {
			callFinishedAndReturned.await(60, TimeUnit.SECONDS);
			return AccountTransactionsResource.createTransaction();
		});
		
		GetTransactionRequest getTransactionRequest = new GetTransactionRequest();
		getTransactionRequest.setTenantId("tenant");
		getTransactionRequest.setTransactionId("transactionId");
		paymentManager.getTransaction(getTransactionRequest, callback);
		callFinishedAndReturned.countDown(); //allow mocked Payment System to return the transaction
		finalCountdown.await(60, TimeUnit.SECONDS); //wait for callback to complete
		assertEquals(0, failures.get());
		assertEquals(1, success.get());
	}
	
	@Test
	public void testAccountHistory() throws Exception {
		verifyResponseOnRequest("GetAccountHistoryRequest.json", "GetAccountHistoryResponse.json");
		ArgumentCaptor<UriInfo> uriInfoArg = ArgumentCaptor.forClass(UriInfo.class);
		ArgumentCaptor<String> accountIdArg = ArgumentCaptor.forClass(String.class);

		verify(paymentSystem.getAccountsResource()).searchTransactions(accountIdArg.capture(), uriInfoArg.capture());
		MultivaluedMap<String, String> queryParameters = uriInfoArg.getValue().getQueryParameters();
		assertThat(queryParameters.getFirst("Offset"), nullValue());
		assertThat(queryParameters.getFirst("Limit"), equalTo("6"));
		assertThat(queryParameters.getFirst("StartDate"), equalTo("2016-11-24"));
		assertThat(queryParameters.getFirst("EndDate"), equalTo("2016-12-01"));
	}

	@Test
	public void testTenantAccountBalance() throws Exception {
		verifyResponseOnRequest("GetAccountBalanceRequestForTenant.json", "GetAccountBalanceResponse.json");
		//bit tricky - one GET /accounts/clientaccount is for CurrencyManager to initialize currency names, only second is for returning tenant account 
		verify(paymentSystem.getAccountsResource(), times(2)).getClientAccount();
		for (String tenantId : restClientTenantIds) {
			assertEquals("searchTenant", tenantId);
		}
	}
	
	@Test
	public void testAccountBalanceCache() throws Exception {
		verifyResponseOnRequest("GetAccountBalanceRequest.json", "GetAccountBalanceResponse.json");
		doThrow(new RuntimeException()).when(paymentSystem.getUsersResource()).getUserAccounts(any());
		verifyResponseOnRequest("GetAccountBalanceRequest.json", "GetAccountBalanceResponse.json");
	}
	
	@Test
	public void testPayoutWithInitExternalPayment() throws Exception {
		//partially overlaps with PaymentServiceStartupTest.testInitExternalPayment()
		verifyResponseOnRequest("InitExternalPaymentRequest.json", "InitExternalPaymentResponse.json");
		
		ArgumentCaptor<PaymentTransactionInitializationRest> arg = ArgumentCaptor.forClass(PaymentTransactionInitializationRest.class);
		verify(paymentSystem.getPaymentTransactionsResource()).initiatePaymentTransaction(arg.capture());
		PaymentTransactionInitializationRest request = arg.getValue();
		System.out.println("Payment call was " + request);
		assertThat(request.getAccountId(), equalTo("1"));
		assertThat(request.getValue(), comparesEqualTo(new BigDecimal("12.34")));
		assertThat(request.getRate(), nullValue());
		assertThat(request.getReference(), equalTo("{\"additionalInfo\":\"no useful information here\"}"));
		assertThat(request.getType(), equalTo(PaymentTransactionType.DEBIT));
		
		assertThat(request.getCallbackUrlSuccess(), endsWith("/callback/paymentSuccess"));
		assertThat(request.getCallbackUrlError(), endsWith("/callback/paymentError"));
		assertThat(request.getRedirectUrlSuccess(), equalTo("pay.ment/success"));
		assertThat(request.getRedirectUrlError(), equalTo("pay.ment/error"));
		
		CashoutDataRequest cashoutData = request.getCashoutData();
		assertThat(cashoutData.getBeneficiary(), equalTo("Identified Name"));
		assertThat(cashoutData.getIban(), equalTo("DE02120300000000202051"));
		assertThat(cashoutData.getBic(), equalTo("BYLADEM1001"));
	}
}
