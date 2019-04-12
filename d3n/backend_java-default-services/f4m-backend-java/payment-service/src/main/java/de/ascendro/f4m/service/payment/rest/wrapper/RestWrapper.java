package de.ascendro.f4m.service.payment.rest.wrapper;

import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.callback.Callback;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.di.RestClientProvider;
import de.ascendro.f4m.service.payment.exception.F4MPaymentSystemIOException;

public abstract class RestWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(RestWrapper.class);

	protected RestClientProvider restClientProvider;
	protected PaymentConfig paymentConfig;
	private String tenantId;
	private LoggingUtil loggingUtil;

	public RestWrapper(String tenantId, RestClientProvider restClientProvider, PaymentConfig paymentConfig,
			LoggingUtil loggingUtil) {
		this.tenantId = tenantId;
		this.restClientProvider = restClientProvider;
		this.paymentConfig = paymentConfig;
		this.loggingUtil = loggingUtil;
	}

	private WebTarget getTarget(String... paths) {
		RestClient restClient = getClient();
		return restClient.getClient().target(getURI(paths));
	}

	protected URI getURI(String... paths) {
		String baseURL = getRestUrl();
		if (StringUtils.isBlank(baseURL)) {
			throw new F4MFatalErrorException("Paydent URL not set");
		}
		UriBuilder builder = UriBuilder.fromUri(baseURL).path(getUriPath());
		for (int i = 0; i < paths.length; i++) {
			builder.path(paths[i]);
		}
		return builder.build();
	}

	protected String getRestUrl() {
		return paymentConfig.getProperty(PaymentConfig.PAYMENT_SYSTEM_REST_URL);
	}

	public String getForwardUriBase() {
		return paymentConfig.getProperty(PaymentConfig.PAYMENT_SYSTEM_WEB_URL);
	}

	protected abstract String getUriPath();

	protected RestClient getClient() {
		return restClientProvider.get(tenantId);
	}

	protected <T> T callGet(GenericType<T> entityType) {
		return callGet(entityType, (Map<String, Object>)null);
	}

	protected void addToSearchParams(Map<String, Object> params, String paramName, Integer value) {
		if (value != null) {
			params.put(paramName, value);
		}
	}

	protected void addToSearchParams(Map<String, Object> params, String paramName, String value) {
		if (value != null) {
			params.put(paramName, value);
		}
	}
	
	protected <T> T callGet(Class<T> entityType, Map<String, Object> params, String... paths) {
		return callGet(response -> response.readEntity(entityType), params, paths);
	}

	protected <T> T callGet(GenericType<T> entityType, Map<String, Object> params, String... paths) {
		return callGet(response -> response.readEntity(entityType), params, paths);
	}

	protected <T> void callGetAsync(Class<T> entityType, Callback<T> callback, Map<String, Object> params, String... paths) {
		callGetAsync(response -> response.readEntity(entityType), callback, params, paths);
	}

	protected <T> void callGetAsync(Function<Response, T> function, Callback<T> callback, Map<String, Object> params, String... paths) {
		WebTarget target = getTarget(paths);
		if (params != null) {
			for (Entry<String, Object> entry : params.entrySet()) {
				target = target.queryParam(entry.getKey(), entry.getValue());
			}
		}
		LOGGER.debug("GETAsync {}", target);
		target.request().accept(MediaType.APPLICATION_JSON_TYPE).async()
				.get(new JerseyInvocationCallback<T>(target, "GET", callback, function::apply, loggingUtil));
	}

	protected <T> T callGet(Function<Response, T> function, Map<String, Object> params, String... paths){

		WebTarget target = getTarget(paths);

		if (params != null) {
			for (Entry<String, Object> entry : params.entrySet()) {
				target = target.queryParam(entry.getKey(), entry.getValue());
			}
		}

		Stopwatch stopwatch = Stopwatch.createStarted();

		Response response = executePaymentRestCall(target, Builder::get);
		LOGGER.debug("GET {} executed in {} with response {}", target, stopwatch.stop(), response);
		try {
			ResponseErrorProcessor.verifyError("GET", null, response);
			return function.apply(response);
		} finally {
			response.close();
		}
	}
	
	private Response executePaymentRestCall(WebTarget target, Function<Builder, Response> restCall) {
		Builder builder = null;
		try {
			builder = target.request().accept(MediaType.APPLICATION_JSON_TYPE);
			return restCall.apply(builder);
		} catch (ProcessingException e) {
			LOGGER.error("Exception ocurred on REST call to target {} exception {}", target, e);
			throw new F4MPaymentSystemIOException("Error contacting payment system", e);
		}
	}

	protected <T, U> U callPut(T entity, Class<U> responseEntityType, String... subpaths) {
		Stopwatch stopwatch = Stopwatch.createStarted();
		WebTarget target = getTarget(subpaths);
		Response response = executePaymentRestCall(target, b -> b.put(Entity.json(entity)));
		LOGGER.debug("PUT {} executed in {} with response {} entity {}", target, stopwatch.stop(), response,Entity.json(entity));
        try {
			ResponseErrorProcessor.verifyError("PUT", entity, response);
			return response.readEntity(responseEntityType);
		} finally {
			response.close();
		}
	}

	protected <T, U> void callPutAsync(T entity, Class<U> responseEntityType, Callback<U> callback, String... subpaths) {
		WebTarget target = getTarget(subpaths);
		LOGGER.debug("PUTAsync {} entity {}", target, (Entity.json(entity)));
		target.request().async().put(Entity.json(entity),
				new JerseyInvocationCallback<U>(target, "PUT", callback, response -> response.readEntity(responseEntityType), loggingUtil));
	}
	
	protected <T, U> U callPost(T entity, Class<U> responseEntityType) {
		Stopwatch stopwatch = Stopwatch.createStarted();
		WebTarget target = getTarget();
		Response response = executePaymentRestCall(target, b -> b.post(Entity.json(entity)));
		LOGGER.debug("POST {} executed in {} entity {} with response {}", target, stopwatch.stop(), (Entity.json(entity)), response);
		try {
			ResponseErrorProcessor.verifyError("POST", entity, response);
			return response.readEntity(responseEntityType);
		} finally {
			response.close();
		}
	}
	
	protected <T, U> void callPostAsync(T entity, Class<U> responseEntityType, Callback<U> callback) {
		WebTarget target = getTarget();
		LOGGER.debug("POSTAsync {} entity {}", target, (Entity.json(entity)));
		target.request().async().post(Entity.json(entity),
									  new JerseyInvocationCallback<U>(target, "POST", callback, response -> response.readEntity(responseEntityType), loggingUtil));
	}
}
