package de.ascendro.f4m.service.payment.rest.wrapper;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.F4MPaymentException;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.dao.TenantInfo;
import de.ascendro.f4m.service.payment.di.RestClientProvider;
import de.ascendro.f4m.service.payment.di.RsaDecryptor;
import de.ascendro.f4m.service.payment.rest.model.AuthRest;
/**
 * Authorization rest calls:<br>
 * <br>
 * >><b>Get authorization Crypted Key</b> URL/tenant/apiId<br>
 * Return: { "Base64" : "Crypted Key in Base64String" }<br>
 * Errors: { "Code" : "int", "Message" : "string", "AdditionalMessage" : "string" }<br>
 * 
 * <table  border="1" cellpadding="5" padding><tr><th>Code</th><th>Message</th><th>AdditionalMessage</th></tr>
 * <tr><td>100</td><td>Tenant not found!</td><td>-</td></tr>
 * <tr><td>101</td><td>Tenant deactivated!</td><td>-</td></tr>
 * <tr><td>500</td><td>Unexpected Error!</td><td>Exception Info</td></tr></table>
 */
public class AuthRestWrapper extends RestWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthRestWrapper.class);

	private static final String URI_PATH = "tenant";

	private TenantInfo tenantInfo;
	private RsaDecryptor rsaDecriptor;

	@Inject
	public AuthRestWrapper(@Assisted String tenantId, RestClientProvider restClientProvider,
			PaymentConfig paymentConfig, RsaDecryptor rsaDecriptor, LoggingUtil loggingUtil) {
		super(tenantId, restClientProvider, paymentConfig, loggingUtil);
		this.rsaDecriptor = rsaDecriptor;
	}

	public String getAuthorizationKey(TenantInfo tenantInfo) throws F4MIOException {
		this.tenantInfo = tenantInfo;
		AuthRest result;
		try {
			result = callGet(AuthRest.class, null, tenantInfo.getApiId());
		} catch (Exception e) {
			throw new F4MPaymentException("Payment system authorization error", e);
		}
		if (StringUtils.isBlank(result.getBase64())) { //tenant REST returns errors with HTTP 200, empty base64 will be parsed if FAIL_ON_UNKNOWN_PROPERTIES = false 
			throw new F4MPaymentException("Empty authorization key was returned");
		}
		return rsaDecriptor.decryptKey(result.getBase64());
	}

	@Override
	protected String getRestUrl() {
		return paymentConfig.getProperty(PaymentConfig.PAYMENT_SYSTEM_AUTH_PATH);
	}

	@Override
	protected String getUriPath() {
		return URI_PATH;
	}

	@Override
	protected RestClient getClient() {
		ClientBuilder builder = new JerseyClientBuilder();
		Client client = builder.register(JacksonFeature.class).build();
		LOGGER.debug("Created REST client {}", client);
		return new RestClient(client, tenantInfo);
	}
}