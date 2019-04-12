package de.ascendro.f4m.service.config;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class F4MConfigImpl extends F4MConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(F4MConfigImpl.class);

	/**
	 * Parameter used as global variable to mark that service is shutting down.
	 */
	public static final String SERVICE_SHUTTING_DOWN = "service.shutting.down";
	
	public static final String JETTY_SSL_PORT = "jetty.httpConfig.securePort";
	public static final String JETTY_CONTEXT_PATH = "jetty.context.path";
	public static final String JETTY_HANDSHAKE_REQUIRE_CLIENT_AUTH = "jetty.clientAuth";
	public static final boolean JETTY_HANDSHAKE_REQUIRE_CLIENT_AUTH_DEFAULT = false;
	public static final String JETTY_MAX_THREADS = "jetty.maxThreads";
	public static final String JETTY_EXCLUDE_CHIPHER_SUITES = "jetty.ssl.excludeCipherSuites";
	public static final String JETTY_EXCLUDE_PROTCOLS = "jetty.ssl.excludeProtocols";
	public static final String RABBIT_EXCHANGE_NAME = "rabbit.exchange.name";
	public static final String RABBIT_USERNAME = "rabbit.username";
	public static final String RABBIT_PASSWORD = "rabbit.password";
	public static final String RABBIT_HOST_VIRTUAL = "rabbit.host.virtual";
	public static final String RABBIT_HOST = "rabbit.host";
	public static final String RABBIT_PORT = "rabbit.port";

	/**
	 * In MILLISECONDS
	 */
	public static final String JETTY_DEFAULT_MAX_SESSION_IDLE_TIMEOUT = "jetty.websocket.defaultMaxSessionIdleTimeout";
	
	//Parallel message handler config
	public static final String WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_INITIAL_SIZE = "websocketConnection.messageHandlerThreadPoolInitialSize";
	public static final int WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_INITIAL_SIZE_DEFAULT = 2;
	
	public static final String WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_MAX_SIZE = "websocketConnection.messageHandlerThreadPoolMaxSize";
	public static final int WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_MAX_SIZE_DEFAULT = 10;
	
	public static final String WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_KEEP_ALIVE_MS = "websocketConnection.messageHandlerThreadPoolMaxKeepAliveMs";
	public static final long WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_KEEP_ALIVE_MS_DEFAULT = 60_000L; //60s idle time for thread

	public static final String WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_MAX_MESSAGE_QUEUE_SIZE = "websocketConnection.messageHandlerThreadPoolMaxMessageQueueSize";
	public static final int WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_MAX_MESSAGE_QUEUE_SIZE_DEFAULT = 1024;

	public static final String ADMIN_EMAIL_DEFAULT = "m.kuldyaev@f4m.tv";

	public static final String SERVICE_REGISTRY_URI = "service.registry.uris";
	public static final String SERVICE_RECONNECT_ON_SERVICE_REGISTRY_CLOSE = "service.registry.reconnectOnClose";
	public static final String REQUEST_SERVICE_CONNECTION_INFO_ON_CONNECTION_CLOSE = "service.requestServiceConnectionInfoOnConnectionClose";

	/**
	 * In MILLISECONDS
	 */
	public static final String CACHE_MANAGER_CLEAN_UP_INTERVAL = "cache.manager.clean.delay";
	/**
	 * In MILLISECONDS
	 */
	public static final String SUBSCRIPTION_STORE_CLEAN_UP_INTERVAL = "subscription.store.clean.delay";
	/**
	 * In MILLISECONDS
	 */
	public static final String USERS_CACHE_TIME_TO_LIVE = "cache.users.timeToLive";
	/**
	 * In MILLISECONDS
	 */
	public static final String REQUESTS_CACHE_TIME_TO_LIVE = "cache.requests.timeToLive";

	/**
	 * In MILLISECONDS
	 */
	public static final String SERVICE_DISCOVERY_DELAY = "service.registry.discovery.retry.delay";

	/**
	 * In MILLISECONDS
	 */
	public static final String SERVICE_CONNECTION_RETRY_DELAY = "service.registry.connection.retry.delay";
	
	/**
	 * In MILLISECONDS
	 */
	public static final String EVENT_SERVICE_DISCOVERY_RETRY_DELAY = "event.service.discoverty.retry.delay";

	/**
	 * Should missing message schema result in exception (true) or warning
	 * (false). Default: true.
	 */
	public static final String SERVICE_MESSAGE_MISSING_SCHEMA_THROWS_EXCEPTION = "service.message.missingSchema.throwsException";

	/**
	 * Is validation errors throwing an exception (true) or just producing a warning
	 * (false). Default: true.
	 */
	public static final String SERVICE_MESSAGE_VALIDATION_ERROR_THROWS_EXCEPTION = "service.message.validationError.throwsException";

	/**
	 * Attempt to send event/resubscribe message via event client queue, when connection with event service is lost 
	 */
	public static final String EVENT_SERVICE_CLIENT_AUTO_RESUBSCRIBE_ON_CLOSE = "event.service.autoResubscribeOnClose";
	/**
	 * Delay in milliseconds before task is to be executed
	 */
	public static final String MONITORING_START_DELAY = "service.registry.monitoring.start.delay";
	/**
	 * Period time in milliseconds between successive monitoring message pushes.
	 */
	public static final String MONITORING_INTERVAL = "service.registry.monitoring.interval";
	
	/**
	 * Should JSON message validator check for user roles' permissions for each message
	 */
	public static final String SERVICE_MESSAGE_VALIDATION_CHECKS_PERMISSIONS = "service.message.validation.checkPermissions";

	/**
	 * Admin email used for error notifications
	 */
	public static final String ADMIN_EMAIL ="service.message.admin.email";

	public F4MConfigImpl() {
		setProperty(RABBIT_EXCHANGE_NAME, "k8s");
		setProperty(RABBIT_USERNAME, "guest");
		setProperty(RABBIT_PASSWORD, "guest");
		setProperty(RABBIT_HOST_VIRTUAL, "/");
		setProperty(RABBIT_HOST, "localhost");
		setProperty(RABBIT_PORT, 5672);

		setProperty(SERVICE_SHUTTING_DOWN, false);
		setProperty(JETTY_SSL_PORT, 8443);
		setProperty(JETTY_HANDSHAKE_REQUIRE_CLIENT_AUTH, JETTY_HANDSHAKE_REQUIRE_CLIENT_AUTH_DEFAULT);
		setProperty(JETTY_CONTEXT_PATH, "/");
		setProperty(JETTY_MAX_THREADS, 200);
		setProperty(JETTY_EXCLUDE_CHIPHER_SUITES,
				".*NULL.*,.*RC4.*,.*MD5.*,.*DES.*,.*DSS.*,TLS_DHE.*,TLS_EDH.*,SSL_DHE.*");
		setProperty(JETTY_EXCLUDE_PROTCOLS, "SSLv3,TLSv1");
		setProperty(JETTY_DEFAULT_MAX_SESSION_IDLE_TIMEOUT, 0);

		setProperty(SERVICE_HOST, getLocalIpAddress());
		setProperty(SERVICE_NAME, "<ChangeIt>");

		setProperty(SERVICE_DISCOVERY_DELAY, 60 * 1000);

		setProperty(CACHE_MANAGER_CLEAN_UP_INTERVAL, 5 * 1000);
		setProperty(SUBSCRIPTION_STORE_CLEAN_UP_INTERVAL, 48 * 60 * 60 * 1000);//48h
		setProperty(USERS_CACHE_TIME_TO_LIVE, 0);//no client id cache by default
		setProperty(REQUESTS_CACHE_TIME_TO_LIVE, 10 * 1000);

		setProperty(SERVICE_CONNECTION_RETRY_DELAY, 10 * 1000);
		
		setProperty(MONITORING_START_DELAY, 10 * 1000);
		setProperty(MONITORING_INTERVAL, 10 * 1000);
		
		setProperty(EVENT_SERVICE_DISCOVERY_RETRY_DELAY, 10 * 1000);
		setProperty(EVENT_SERVICE_CLIENT_AUTO_RESUBSCRIBE_ON_CLOSE, true);

		setProperty(SERVICE_RECONNECT_ON_SERVICE_REGISTRY_CLOSE, true);

		setProperty(SERVICE_MESSAGE_MISSING_SCHEMA_THROWS_EXCEPTION, true);
		setProperty(SERVICE_MESSAGE_VALIDATION_ERROR_THROWS_EXCEPTION, true);
		setProperty(SERVICE_MESSAGE_VALIDATION_CHECKS_PERMISSIONS, true);
		
		setProperty(REQUEST_SERVICE_CONNECTION_INFO_ON_CONNECTION_CLOSE, true);
		
		setProperty(WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_INITIAL_SIZE, WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_INITIAL_SIZE_DEFAULT);
		setProperty(WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_KEEP_ALIVE_MS, WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_KEEP_ALIVE_MS_DEFAULT);
		setProperty(WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_MAX_SIZE, WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_MAX_SIZE_DEFAULT);
		setProperty(WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_MAX_MESSAGE_QUEUE_SIZE, WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_MAX_MESSAGE_QUEUE_SIZE_DEFAULT);
		setProperty(ADMIN_EMAIL, ADMIN_EMAIL_DEFAULT);

		loadProperties();
	}

	public F4MConfigImpl(Config... configs) {
		this();
		if (ArrayUtils.isNotEmpty(configs)) {
			Arrays.stream(configs).forEach(config -> 
				config.getProperties().forEach((k, v) -> properties.putIfAbsent(k, v))
			);
		}
	}

	private String getLocalIpAddress() {
		String localIpAddress;
		try {
			localIpAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			LOGGER.error("Error while retrieving local ip address: {}", e);
			localIpAddress = "localhost";
		}
		return localIpAddress;
	}

	public URI getServiceURI() throws URISyntaxException {
		return new URI("wss", null, getProperty(SERVICE_HOST), getPropertyAsInteger(JETTY_SSL_PORT),
				getProperty(JETTY_CONTEXT_PATH), null, null);
	}

	public String getServiceIdentifier() {
		return "/" + getProperty(SERVICE_NAME) + ":" + getProperty(JETTY_SSL_PORT);
	}

	public String getServiceEndpointIdentifier() {
		return getProperty(SERVICE_HOST) + ":" + getProperty(JETTY_SSL_PORT);
	}

}
