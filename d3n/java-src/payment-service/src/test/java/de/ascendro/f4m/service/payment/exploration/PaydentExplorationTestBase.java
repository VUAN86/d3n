package de.ascendro.f4m.service.payment.exploration;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.client.Client;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.JerseyClient;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.server.analytics.AnalyticsDao;
import de.ascendro.f4m.server.analytics.tracker.TrackerImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.dao.TenantFileDaoImpl;
import de.ascendro.f4m.service.payment.di.RestClientProvider;
import de.ascendro.f4m.service.payment.manager.AnalyticsEventManager;
import de.ascendro.f4m.service.payment.manager.CurrencyManager;
import de.ascendro.f4m.service.payment.manager.impl.GameManagerImpl;
import de.ascendro.f4m.service.payment.manager.impl.PaymentManagerImpl;
import de.ascendro.f4m.service.payment.manager.impl.UserAccountManagerImpl;
import de.ascendro.f4m.service.payment.rest.wrapper.AccountRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.AuthRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.CurrencyRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.ExchangeRateRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.GameRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.IdentificationRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.PaymentTransactionRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.PaymentWrapperUtils;
import de.ascendro.f4m.service.payment.rest.wrapper.TransactionRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.UserRestWrapper;
import de.ascendro.f4m.service.session.pool.SessionPool;
import de.ascendro.f4m.service.util.TestGsonProvider;

public abstract class PaydentExplorationTestBase {
	public static final String FIRST_NAME = "J\u0101nis";
	public static final String LAST_NAME = "B\u0113rzi\u0146\u0161";
	
	protected String REAL_EUR_CURRENCY_ID = "2";

	protected PaymentConfig config;
	protected Client client;
	protected RestClientProvider clientProvider;
	protected PaymentManagerImpl paymentManager;
	protected GameManagerImpl gameManager;
	protected UserAccountManagerImpl userAccountManager;
	protected CurrencyManager currencyManager;
	protected CurrencyRestWrapper currencyRestWrapper;
	protected ExchangeRateRestWrapper exchangeRateRestWrapper;
	protected UserRestWrapper userRestWrapper;
	protected AccountRestWrapper accountRestWrapper;
	protected TransactionRestWrapper transactionRestWrapper;
	protected IdentificationRestWrapper identificationRestWrapper;
	protected PaymentTransactionRestWrapper paymentTransactionRestWrapper;
	protected GameRestWrapper gameRestWrapper;
	private CommonProfileAerospikeDao commonProfileAerospikeDao;

	@Mock
	private SessionPool sessionPool;
	@Mock
	private AnalyticsDao analyticsDao;
	@Mock
	private AuthRestWrapper authRestWrapper;
	
	protected String tenantId;
	protected String userId;
	protected String profileId;
	protected String appId = "appId";
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		initUserId();
		config = new PaymentConfig();
		config.setProperty(PaymentConfig.FAIL_ON_UNKNOWN_REST_PROPERTIES, true);
		TrackerImpl tracker = new TrackerImpl(analyticsDao);
		String tenantInfoFilePath = config.getProperty(PaymentConfig.TENANT_INFO_MOCK_PATH);
		Assume.assumeTrue("JSON file with tenant configuration not specified", StringUtils.isNotBlank(tenantInfoFilePath));
		JsonUtil jsonUtil = new JsonUtil();
		TenantFileDaoImpl tenantDao = new TenantFileDaoImpl(jsonUtil);
		tenantDao.setPath(tenantInfoFilePath);
		clientProvider = new RestClientProvider(tenantDao, (id) -> authRestWrapper, config);
		String paymentSystemApiKey = config.getProperty(PaymentConfig.PAYMENT_SYSTEM_API_KEY_TEST);
		Assume.assumeTrue("Payment System ApiKey not set", StringUtils.isNotBlank(paymentSystemApiKey));
		when(authRestWrapper.getAuthorizationKey(any())).thenReturn(paymentSystemApiKey);
		//clientProvider.get(tenantInfoFilePath).getClient()
		LoggingUtil loggingUtil = new LoggingUtil(config);
		currencyRestWrapper = new CurrencyRestWrapper(tenantId, clientProvider, config, loggingUtil);
		exchangeRateRestWrapper = new ExchangeRateRestWrapper(tenantId, clientProvider, config, loggingUtil);
		userRestWrapper = new UserRestWrapper(tenantId, clientProvider, config, loggingUtil);
		accountRestWrapper = new AccountRestWrapper(tenantId, clientProvider, config, loggingUtil);
		PaymentWrapperUtils utils = new PaymentWrapperUtils(config);
		transactionRestWrapper = new TransactionRestWrapper(tenantId, clientProvider, config, loggingUtil, utils);
		identificationRestWrapper = new IdentificationRestWrapper(tenantId, clientProvider, config, loggingUtil);
		paymentTransactionRestWrapper = new PaymentTransactionRestWrapper(tenantId, clientProvider, config, loggingUtil);
		gameRestWrapper = new GameRestWrapper(tenantId, clientProvider, config, loggingUtil, utils);
		GsonProvider gsonProvider = new TestGsonProvider();
		currencyManager = new CurrencyManager((id) -> currencyRestWrapper, (id) -> accountRestWrapper, tenantDao,
				(id) -> exchangeRateRestWrapper, config);
		commonProfileAerospikeDao = null;

		AnalyticsEventManager analyticsEventManager = new AnalyticsEventManager(gsonProvider, tracker);

		userAccountManager = new UserAccountManagerImpl(currencyManager, (id) -> userRestWrapper,
				(id) -> accountRestWrapper, config, commonProfileAerospikeDao);
		paymentManager = new PaymentManagerImpl(currencyManager, userAccountManager, (id) -> userRestWrapper, (id) -> accountRestWrapper,
				(id) -> transactionRestWrapper, mock(TransactionLogAerospikeDao.class), gsonProvider, analyticsEventManager, tracker);
		gameManager = new GameManagerImpl(currencyManager, (id) -> gameRestWrapper, gsonProvider, analyticsEventManager);
		client = clientProvider.get(tenantId).getClient();
		assertNotNull(client);
		assertThat(client, instanceOf(JerseyClient.class));
	}
	
	private void initUserId() {
		tenantId = "1";
//		tenantId = "00000000-e89b-12d3-a456-426655440000";
		profileId = "123e4567-e89b-12d3-a456-426655440001";
		userId = tenantId + "_" + profileId;
	}

    @After
    public void tearDown() {
    	if (clientProvider != null) {
    		clientProvider.close();
    	}
    }
}
