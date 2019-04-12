package de.ascendro.f4m.service.payment.rest.wrapper;

import java.util.function.Function;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.callback.Callback;
import de.ascendro.f4m.service.payment.exception.F4MPaymentSystemIOException;

public class JerseyInvocationCallback<T> implements InvocationCallback<Response> {
	private static final Logger LOGGER = LoggerFactory.getLogger(JerseyInvocationCallback.class);
	private WebTarget target;
	private Stopwatch stopwatch;
	private Callback<T> callback;
	private Function<Response, T> entityReader;
	private String method; //method could be retrieved from response, but involves casting to implementation specific package private InboundJaxrsResponse
	private LoggingUtil loggingUtil;
	
	public JerseyInvocationCallback(WebTarget target, String method, Callback<T> callback,
			Function<Response, T> entityReader, LoggingUtil loggingUtil) {
		this.target = target;
		this.method = method;
		this.callback = callback;
		this.entityReader = entityReader;
		this.loggingUtil = loggingUtil;
		this.stopwatch = Stopwatch.createStarted();
	}
	
	@Override
	public void completed(Response response) {
		loggingUtil.saveBasicInformationInThreadContext();
		LOGGER.debug("{} {} executed in {} with response {}", method, target, stopwatch.stop(), response);
		try {
			ResponseErrorProcessor.verifyError(method, null, response);
			T result = entityReader.apply(response);
			callback.completed(result);
		} catch (Exception e) {
			callback.failed(e);
		} finally {
			response.close();
		}
	}

	@Override
	public void failed(Throwable throwable) {
		loggingUtil.saveBasicInformationInThreadContext();
		LOGGER.debug("{} {} executed in {} with error", method, target, stopwatch.stop(), throwable);
		Throwable t = throwable;
		if (throwable instanceof ProcessingException) {
			t = new F4MPaymentSystemIOException("Error contacting payment system asynchronously", throwable);
		}
		callback.failed(t);
	}
}