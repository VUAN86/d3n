package de.ascendro.f4m.service.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.handler.MessageHandlerProvider;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.session.SessionWrapperFactory;
import de.ascendro.f4m.service.util.RetriedAssertForDefaultService;

public class ParallelServiceJsonMessageHandlerTest {
	private final ReentrantLock PROCESSING_LOCK = new ReentrantLock(); //non-static to avoid polluting other test results if one fails
	private AtomicInteger START_COUNTER = new AtomicInteger(0);

	@Mock
	private MessageHandlerProvider<String> messageHandlerProvider;
	@Mock
	private SessionWrapperFactory sessionWrapperFactory;
	@Mock
	private LoggingUtil loggingUtil;
	
	private Config config;
	private ParallelServiceJsonMessageHandler parallelServiceJsonMessageHandler;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		START_COUNTER.set(0);

		config = new F4MConfigImpl();
		config.setProperty(F4MConfigImpl.WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_INITIAL_SIZE, "1");
		config.setProperty(F4MConfigImpl.WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_MAX_SIZE, "1");
		config.setProperty(F4MConfigImpl.WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_MAX_MESSAGE_QUEUE_SIZE, "2");

		parallelServiceJsonMessageHandler = new ParallelServiceJsonMessageHandler(messageHandlerProvider, config,
				loggingUtil, sessionWrapperFactory) {
			@Override
			protected void executeOnMessage(String originalMessageEncoded) {
				START_COUNTER.incrementAndGet();
				PROCESSING_LOCK.lock();
				PROCESSING_LOCK.unlock();
			}
		};
	}

	@Test
	public void testBlockingWithInterrupt() {
		assertFalse(PROCESSING_LOCK.isLocked());
		assertEquals(0, START_COUNTER.get());
		PROCESSING_LOCK.lock();

		final List<Throwable> errorCollector = new ArrayList<>(1);
		final Thread executeRequestThread = new Thread(() -> {
			try {
				parallelServiceJsonMessageHandler.onMessage("1");
				parallelServiceJsonMessageHandler.onMessage("2");
				parallelServiceJsonMessageHandler.onMessage("3");
				parallelServiceJsonMessageHandler.onMessage("4");
			} catch (Throwable th) {
				errorCollector.add(th);
			}
		});
		executeRequestThread.start();

		RetriedAssertForDefaultService.assertWithWait(() -> assertEquals(1, START_COUNTER.get()));

		// Interrupt first call
		executeRequestThread.interrupt();

		// Expect rejection if await for message processing is interrupted
		RetriedAssertForDefaultService.assertWithWait(() -> assertEquals(1, errorCollector.size()));
		RetriedAssertForDefaultService
				.assertWithWait(() -> assertEquals(RejectedExecutionException.class, errorCollector.get(0).getClass()));
		RetriedAssertForDefaultService.assertWithWait(() -> assertEquals(1, START_COUNTER.get()));
		
		PROCESSING_LOCK.unlock();
		RetriedAssertForDefaultService.assertWithWait(() -> assertEquals(3, START_COUNTER.get()));
	}

	@Test
	public void testBlockingIfQueueIsFullAndNoThreadsAvailable() {
		assertFalse(PROCESSING_LOCK.isLocked());
		assertEquals(0, START_COUNTER.get());
		PROCESSING_LOCK.lock();

		parallelServiceJsonMessageHandler.onMessage("First call");
		RetriedAssertForDefaultService.assertWithWait(() -> assertEquals(1, START_COUNTER.get()));

		parallelServiceJsonMessageHandler.onMessage("Second call waiting in queue to be executed");
		RetriedAssertForDefaultService.assertWithWait(() -> assertEquals(1, START_COUNTER.get()));

		final AtomicBoolean thirdCallStarted = new AtomicBoolean(false);
		final AtomicBoolean thirdCallFinished = new AtomicBoolean(false);
		new Thread(() -> {
			thirdCallStarted.set(true);
			parallelServiceJsonMessageHandler.onMessage("Third call waiting in queue to be executed");
			parallelServiceJsonMessageHandler.onMessage("Forth call blocks thread as no space in queue available");
			thirdCallFinished.set(true);
		}).start();
		RetriedAssertForDefaultService.assertWithWait(() -> assertEquals(1, START_COUNTER.get()));
		RetriedAssertForDefaultService.assertWithWait(() -> assertTrue(thirdCallStarted.get()));// Third call execute request thread started
		RetriedAssertForDefaultService.assertWithWait(() -> assertFalse(thirdCallFinished.get()));// but not finished

		// Free all execution thread so second and third call can be executed
		PROCESSING_LOCK.unlock();
		RetriedAssertForDefaultService.assertWithWait(() -> assertTrue(thirdCallFinished.get()));// Third call submitted into queue
		RetriedAssertForDefaultService.assertWithWait(() -> assertEquals(4, START_COUNTER.get()));
	}

	@Test
	public void testWaitingInQueue() {
		assertFalse(PROCESSING_LOCK.isLocked());
		assertEquals(0, START_COUNTER.get());
		PROCESSING_LOCK.lock();

		parallelServiceJsonMessageHandler.onMessage("First call");
		RetriedAssertForDefaultService.assertWithWait(() -> assertEquals(1, START_COUNTER.get()));

		parallelServiceJsonMessageHandler.onMessage("Second call waiting in queue to be executed");
		RetriedAssertForDefaultService.assertWithWait(() -> assertEquals(1, START_COUNTER.get()));

		// Free execution thread so second call can be executed
		PROCESSING_LOCK.unlock();
		RetriedAssertForDefaultService.assertWithWait(() -> assertEquals(2, START_COUNTER.get()));
	}

}
