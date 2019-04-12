package de.ascendro.f4m.service.payment.di;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.apache.commons.lang3.Validate;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.payment.F4MPaymentException;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.dao.TenantDao;
import de.ascendro.f4m.service.payment.dao.TenantInfo;
import de.ascendro.f4m.service.payment.rest.wrapper.AuthRestWrapper;
import de.ascendro.f4m.service.payment.rest.wrapper.RestClient;
import de.ascendro.f4m.service.payment.rest.wrapper.RestWrapperFactory;

public class RestClientProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(RestClientProvider.class);
	
	private Map<String, RestClient> clientPool = new ConcurrentHashMap<>();
	private TenantDao tenantDao;
	private RestWrapperFactory<AuthRestWrapper> authRestWrapperFactory;
	private PaymentConfig config;
	
	@Inject
	public RestClientProvider(TenantDao tenantDao, RestWrapperFactory<AuthRestWrapper> authRestWrapperFactory, PaymentConfig config) {
		this.tenantDao = tenantDao;
		this.authRestWrapperFactory = authRestWrapperFactory;
		this.config = config;
	}

	public RestClient get(String tenantId) {
		Validate.notNull(tenantId);
		RestClient client = clientPool.get(tenantId);
		if (client == null) {
			client = createClient(tenantId);
			clientPool.put(tenantId, client);
		}
		return client;
	}

	private RestClient createClient(String tenantId) {
		LOGGER.debug("Initiating Paydent REST client");
		TenantInfo tenantInfo = tenantDao.getTenantInfo(tenantId);
		LOGGER.debug("Read tenant configuration for Payment System {}", tenantInfo);
		String username = tenantInfo.getApiId();
		String password = getApiKey(tenantInfo);
		
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(username, password);
		LoggingFeature loggingFeature = new LoggingFeature();
		JacksonJsonProvider jacksonJsonProvider = new JacksonJaxbJsonProvider().configure(
				DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
				config.getPropertyAsBoolean(PaymentConfig.FAIL_ON_UNKNOWN_REST_PROPERTIES));
		ClientConfig clientConfig = new ClientConfig();
		clientConfig.property(ClientProperties.CONNECT_TIMEOUT,
				config.getPropertyAsInteger(PaymentConfig.REST_CONNECT_TIMEOUT));
		clientConfig.property(ClientProperties.READ_TIMEOUT,
				config.getPropertyAsInteger(PaymentConfig.REST_READ_TIMEOUT));
		
		ClientBuilder builder = new JerseyClientBuilder(); //ClientBuilder.newBuilder() provides RestEasy impl because of f4m-auth-lib
		LOGGER.debug("Using REST client builder {}", builder);
		Client client = builder
				.withConfig(clientConfig)
				.register(jacksonJsonProvider)
				.register(JacksonFeature.class)
				.register(loggingFeature)
				.register(feature)
				.build();
		LOGGER.debug("Created REST client {}", client);
		return new RestClient(client, tenantInfo);
	}
	
	@PreDestroy
	public void close() {
		LOGGER.debug("Closing Paydent REST client");
		for (RestClient client : clientPool.values()) {
			try {
				client.getClient().close();
			} catch (RuntimeException e) {
				LOGGER.error("Error closing client", e);
			}
		}
	}

	private String getApiKey(TenantInfo tenantInfo) {
		String apiKey = null;
		try {
			apiKey = authRestWrapperFactory.create(tenantInfo.getTenantId()).getAuthorizationKey(tenantInfo);
		} catch (F4MIOException e) {
			throw new F4MPaymentException("Error getting authorization data", e);
		}
		return apiKey;
	}
}
