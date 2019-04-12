package de.ascendro.f4m.service.payment.callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WrapperCallback <T, J> implements Callback<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger(WrapperCallback.class);
	
	protected Callback<J> originalCallback;

	public WrapperCallback(Callback<J> originalCallback) {
		this.originalCallback = originalCallback;
	}

	@Override
	public void completed(T response) {
		try {
			LOGGER.debug("Callback success with data {}", response);
			executeOnCompleted(response);
		} catch (Exception t) {
			originalCallback.failed(t);
		}
	}

	/**
	 * Implement this method instead of {@link #completed(Object)} to maintain error catching and failure reporting 
	 * @param response
	 */
	protected abstract void executeOnCompleted(T response);

	@Override
	public void failed(Throwable throwable) {
		try {
			LOGGER.debug("Callback failed", throwable);
			executeOnFailed(throwable);
		} catch (Exception t) {
			originalCallback.failed(t);
		}
	}

	/**
	 * Override this method instead of  {@link #failed(Throwable)} to maintain error catching and failure reporting
	 * @param throwable
	 */
	protected void executeOnFailed(Throwable throwable) {
		originalCallback.failed(throwable);
	}
}