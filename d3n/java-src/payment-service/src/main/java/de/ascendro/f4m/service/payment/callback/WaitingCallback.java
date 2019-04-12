package de.ascendro.f4m.service.payment.callback;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.payment.callback.Callback;

public class WaitingCallback<T> implements Callback<T> {
	private static final int DEFAULT_TIMEOUT = 180;

	private static final Logger LOGGER = LoggerFactory.getLogger(WaitingCallback.class);
	
	private CompletableFuture<T> future = new CompletableFuture<>();
	
	public T getResult() {
		try {
			return future.get(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new F4MFatalErrorException("Could not wait to finish", e);
		}
	}

	@Override
	public void completed(T response) {
		future.complete(response);
	}

	@Override
	public void failed(Throwable throwable) {
		LOGGER.error("Callback failed {}", throwable);
		future.cancel(false);
	}
}
