package de.ascendro.f4m.service.payment.config;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.UriBuilder;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;

public class PaymentConfig extends F4MConfigImpl implements Config {
	public static final String MOCK_MODE = "mockMode";
	
	/**
	 * If specified, tenant configuration will be read from the specified path instead of reading from Aerospike.
	 */
	public static final String TENANT_INFO_MOCK_PATH = "tenantInfoMockPath";
	public static final String PAYMENT_SYSTEM_API_KEY_TEST = "payment.system.api.key.test";
	
	public static final String PAYMENT_SYSTEM_REST_URL = "payment.system.rest";
	public static final String PAYMENT_SYSTEM_WEB_URL = "payment.system.web";
	public static final String PAYMENT_SYSTEM_AUTH_PATH = "payment.system.auth.path";
	public static final String PAYMENT_SYSTEM_PRIVATE_KEY_PATH = "payment.system.private.key.path";
	public static final String PAYMENT_SYSTEM_PUBLIC_KEY_PATH = "payment.system.public.key.path";
	public static final String PAYMENT_SYSTEM_KEY_PASSWORD = "payment.system.key.password";
	public static final String PAYMENT_SYSTEM_ITUNES = "payment.system.itunes.apple.com";
	public static final String PAYMENT_SYSTEM_ITUNES_TEST = "payment.system.itunes.apple.com.test";
	public static final String PAYMENT_SYSTEM_PLAYMARKET = "payment.system.itunes.playmarket.com";
	public static final String PAYMENT_SYSTEM_SSL_REPOSITORY_SRC = "system.ssl.reposotory";

	public static final String EXTERNAL_HOSTNAME = "service.host.external";
	public static final String EXTERNAL_HOSTNAME_SCHEME = "service.host.external.scheme";
	public static final String CALLBACK_BASE_CONTEXT_PATH = "callbackContextPath";
	public static final String CALLBACK_BASE_CONTEXT_PATH_DEFAULT = "/callback";
	public static final String IDENTIFICATION_SUCCESS_CONTEXT_PATH = "/identificationSuccess";
	public static final String IDENTIFICATION_ERROR_CONTEXT_PATH = "/identificationError";
	public static final String PAYMENT_SUCCESS_CONTEXT_PATH = "/paymentSuccess";
	public static final String PAYMENT_ERROR_CONTEXT_PATH = "/paymentError";
	public static final String PAYMENT_APP_ID = "payment.system.appId";
	public static final String IDENTIFICATION_POOL_SIZE = "identification.pool.size";
	public static final String IDENTIFICATION_SCHEDULE_TIMEOUT = "identification.schedule.timeout";
	public static final String TENANT_MONEY_ACCOUNT_ID = "tenant.money.account.id";
	public static final String EXCHANGE_RATE_MONEY_BONUS = "exchange.rate.money.bonus";
	public static final String EXCHANGE_RATE_MONEY_CREDIT = "exchange.rate.money.credit";
	public static final String EXCHANGE_RATE_BONUS_CREDIT = "exchange.rate.bonus.credit";
	public static final BigDecimal DUMMY_EXCHANGE_RATE_DEFAULT = new BigDecimal("77.7");
	public static final String AEROSPIKE_MULTIPLAYER_KEY_PREFIX = "aerospike.multiplayer.key.prefix";
	public static final String AEROSPIKE_MULTIPLAYER_SET = "aerospike.set.multiplayer";

	/**
	 * Clean-up interval in milliseconds, how long logs of external transactions are stored in cache waiting for callback.
	 */
	public static final String LOG_CACHE_ENTRY_TIME_TO_LIVE = "logCacheEntyTTL";
	public static final String LOG_CACHE_CLEAN_UP_INTERVAL = "logCacheCleanupInterval";
	
	public static final String TRANSACTION_RETRY_TIMES = "logCacheCleanupInterval";
	
	public static final String REST_CONNECT_TIMEOUT = "payment.restConnectTimeout";
	public static final String REST_READ_TIMEOUT = "payment.restReadTimeout";
	public static final String FAIL_ON_UNKNOWN_REST_PROPERTIES = "payment.failOnUnknownRestProperties";

	// MySQL
	public static final String HIBERNATE_CONNECTION_DRIVER_CLASS = "hibernate.connection.driver_class";
	public static final String HIBERNATE_CONNECTION_URL = "hibernate.connection.url";
	public static final String HIBERNATE_CONNECTION_USERNAME = "hibernate.connection.username";
	public static final String HIBERNATE_CONNECTION_PASSWORD = "hibernate.connection.password";
	public static final String HIBERNATE_CONNECTION_POOL_SIZE = "hibernate.connection.pool_size";
	public static final String HIBERNATE_DIALECT = "hibernate.dialect";

	public PaymentConfig() {
		super(new AerospikeConfigImpl());
		setProperty(HIBERNATE_CONNECTION_DRIVER_CLASS, "");
		setProperty(HIBERNATE_CONNECTION_URL, "");
		setProperty(HIBERNATE_CONNECTION_USERNAME, "");
		setProperty(HIBERNATE_CONNECTION_PASSWORD, "");
		setProperty(HIBERNATE_CONNECTION_POOL_SIZE, "");
		setProperty(HIBERNATE_DIALECT, "");

		setProperty(MOCK_MODE, false);
		setProperty(CALLBACK_BASE_CONTEXT_PATH, CALLBACK_BASE_CONTEXT_PATH_DEFAULT);
		setProperty(LOG_CACHE_ENTRY_TIME_TO_LIVE, 30 * 60 * 1000); //30 minutes
		setProperty(LOG_CACHE_CLEAN_UP_INTERVAL, 10 * 60 * 1000); //10 minutes
		setProperty(EXTERNAL_HOSTNAME_SCHEME, "https");
		setProperty(IDENTIFICATION_POOL_SIZE, 5);
		setProperty(IDENTIFICATION_SCHEDULE_TIMEOUT, 5 * 1000); // 5 seconds
		setProperty(TENANT_MONEY_ACCOUNT_ID, "0");
		setProperty(AEROSPIKE_MULTIPLAYER_KEY_PREFIX, "mgi");
		setProperty(AEROSPIKE_MULTIPLAYER_SET, "mgi");


		setProperty(PAYMENT_SYSTEM_REST_URL, "https://api.paydent.net");
		setProperty(PAYMENT_SYSTEM_WEB_URL, "https://web.paydent.net");
		setProperty(PAYMENT_SYSTEM_ITUNES, "https://buy.itunes.apple.com/verifyReceipt");
		setProperty(PAYMENT_SYSTEM_ITUNES_TEST, "https://sandbox.itunes.apple.com/verifyReceipt");
		setProperty(PAYMENT_SYSTEM_PLAYMARKET, "https://www.googleapis.com/auth/androidpublisher");
		setProperty(PAYMENT_SYSTEM_SSL_REPOSITORY_SRC, "/etc/ssl/certs");

		setProperty(EXCHANGE_RATE_MONEY_BONUS, DUMMY_EXCHANGE_RATE_DEFAULT);
		setProperty(EXCHANGE_RATE_MONEY_CREDIT, DUMMY_EXCHANGE_RATE_DEFAULT);
		setProperty(EXCHANGE_RATE_BONUS_CREDIT, DUMMY_EXCHANGE_RATE_DEFAULT);

		setProperty(TRANSACTION_RETRY_TIMES, 10);
		setProperty(PAYMENT_APP_ID, 37);
		
		setProperty(FAIL_ON_UNKNOWN_REST_PROPERTIES, false);
		int defaultTimeout = calculateDefaultTimeout();
		setProperty(REST_CONNECT_TIMEOUT, defaultTimeout);
		setProperty(REST_READ_TIMEOUT, defaultTimeout);
		
		
		setProperty(ADMIN_EMAIL, ADMIN_EMAIL_DEFAULT);

		loadProperties();
	}
	
	private int calculateDefaultTimeout() {
		//if another service is waiting with loadOrWithdrawWithoutCoverage in cache, wait for Paydent 9 seconds and hope that internal response will happen in 1 sec.
		return (int) (this.getPropertyAsInteger(REQUESTS_CACHE_TIME_TO_LIVE) * 0.9);
	}
	
	//all URL calculations maybe could be extracted into separate CallbackUrlConfig or similar
	public String getCallbackBaseContextPath() {
		return this.getProperty(CALLBACK_BASE_CONTEXT_PATH);
	}
	
	public String getExternalHostname() {
		String hostname = this.getProperty(EXTERNAL_HOSTNAME);
		if (StringUtils.isBlank(hostname)) {
			hostname = this.getProperty(SERVICE_HOST);
		}
		return hostname;
	}
	
	public URI getExternalCallbackForContext(String paramName) {
		try {
			//probably at some point there it will be necessary to provide other means of connecting - different ports and paths because of forwarding
			String scheme = this.getProperty(EXTERNAL_HOSTNAME_SCHEME);
			URI baseUri = new URI(scheme, null, getExternalHostname(), getPropertyAsInteger(JETTY_SSL_PORT), null, null, null);
			UriBuilder builder = UriBuilder.fromUri(baseUri)
					.path(this.getCallbackBaseContextPath())
					.path(paramName);
			return builder.build();
		} catch (URISyntaxException e) {
			throw new F4MFatalErrorException("Incorrect URL for callback " + paramName, e);
		}
	}
}
