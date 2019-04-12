package de.ascendro.f4m.service.payment.callback;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class TestCallback<T extends JsonMessageContent> implements Callback<T>, AutoCloseable {
	private Consumer<T> consumer;
	private AtomicInteger wasCalled = new AtomicInteger();

	public TestCallback(Consumer<T> consumer) {
		this.consumer = consumer;
	}

	@Override
	public void completed(T response) {
		wasCalled.incrementAndGet();
		consumer.accept(response);
	}

	@Override
	public void failed(Throwable throwable) {
		wasCalled.incrementAndGet();
		throw new RuntimeException(throwable);
	}

	@Override
	public void close() throws Exception {
		assertEquals("Callback was not called during test", 1, wasCalled.get());
	}
	
	public static class WithIgnoringConsumer<T extends JsonMessageContent> extends TestCallback<T> {
		public WithIgnoringConsumer() {
			super(r -> {});
		}
	}
}