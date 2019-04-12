package de.ascendro.f4m.service.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class QueuedThreadPoolExecutorTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(QueuedThreadPoolExecutorTest.class);
	private static final int CORE_POOL_SIZE = 2;
	private static final int MAX_POOL_SIZE = 4;
	private static final int QUEUE_SIZE = 10;
	
	private QueuedThreadPoolExecutor executor;
	private CountDownLatch teamCountdown;
	private CountDownLatch queuedTaskLatch;
	
	//@Rule public SimpleRepeatRule repeatRule = new SimpleRepeatRule(1_000);
	@Rule
	public ErrorCollector errorCollector = new ErrorCollector();
	@Rule
	public TestRule watcher = new TestWatcher() {
		@Override
		protected void starting(Description description) {
			LOGGER.debug("Starting test: " + description.getMethodName());
		}
	};

	@Before
	public void setUp() {
		ThreadFactory factory = new ThreadFactoryBuilder()
				.setNameFormat("philosophizing-thread-%d").build();
		executor = QueuedThreadPoolExecutor.newQueuedThreadPool(CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_SIZE, 1000, TimeUnit.MILLISECONDS, factory);
		//Here are examples of ready-made Executors, for which at least one test fails:
		// - executor = java.util.concurrent.Executors.newCachedThreadPool()
		// - BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(queueSize) or new SynchronousQueue<>()
		// - executor = new java.util.concurrent.ThreadPoolExecutor(corePoolSize, maxPoolSize, 1000, TimeUnit.MILLISECONDS, queue)
	}

	@After
	public void tearDown() throws Exception {
		executor.shutdown();
		boolean wasShutDown = executor.awaitTermination(1, TimeUnit.SECONDS);
		assertTrue("Executor was supposed to be shut down", wasShutDown);
	}

	@Test(timeout=5000)
	public void checkIfMaxPoolSizeThreadsAreCreated() throws Exception {
		teamCountdown = new CountDownLatch(MAX_POOL_SIZE);
		for (int i = 0; i < MAX_POOL_SIZE; i++) {
			executor.execute(waitForOthersInTeam(i));
		}
		teamCountdown.await();
	}

	@Test(timeout=5000)
	public void checkIfNextAfterMaxPoolSizeIsNotRejected() throws Exception {
		teamCountdown = new CountDownLatch(MAX_POOL_SIZE + 1);
		//fill up the thread poll by using all threads:
		for (int i = 0; i < MAX_POOL_SIZE; i++) {
			executor.execute(waitForOthersInTeam(i));
		}
		//create additional task, which should be queued for execution:
		queuedTaskLatch = new CountDownLatch(1);
		executor.execute(queuedTask(99));
		boolean additionalThreadExecuted = queuedTaskLatch.await(20, TimeUnit.MILLISECONDS); //check, that execution didn't finish
		teamCountdown.countDown(); //release the latch for threads in pool
		teamCountdown.await(); // and verify, that all exit cleanly
		queuedTaskLatch.await(); // and also additional task from queue is also executed
		assertFalse("Additional task was executed despite the fact that maxPoolSize was exceeded at that moment", additionalThreadExecuted);
	}
	
	@Test(timeout=5000)
	public void checkTaskSubmitterThreadIsBlockedIfPoolAndQueueAreFull() throws Exception {
		teamCountdown = new CountDownLatch(MAX_POOL_SIZE + 1);
		//fill up the thread poll by using all threads:
		for (int i = 0; i < MAX_POOL_SIZE; i++) {
			executor.execute(waitForOthersInTeam(i));
		}
		//fill up the secondary queue:
		queuedTaskLatch = new CountDownLatch(QUEUE_SIZE + 1);
		for (int i = 0; i < QUEUE_SIZE; i++) {
			executor.execute(queuedTask(i));
		}
		//try to add additional task:
		CountDownLatch blockedThreadMarker = new CountDownLatch(1);
		CountDownLatch extraThreadBlocked = new CountDownLatch(1);
		new Thread(() -> {
			blockedThreadMarker.countDown();
			executor.execute(queuedTask(99));
			extraThreadBlocked.countDown();
		}).start();
		//vague verification, that extra Thread was blocked by .execute:
		blockedThreadMarker.await();
		assertEquals(1, extraThreadBlocked.getCount());
		teamCountdown.countDown(); //release latch to allow threads in pool to finish executing
		extraThreadBlocked.await(); //check that extra Thread was released
		//check that first team-tasks for pool were executed and also all queued tasks were executed: 
		teamCountdown.await();
		queuedTaskLatch.await();
	}
	
	@Test(timeout=5000)
	public void checkPoolCanBeShutDownIfFull() throws Exception {
		teamCountdown = new CountDownLatch(MAX_POOL_SIZE + 1);
		for (int i = 0; i < MAX_POOL_SIZE; i++) {
			executor.execute(waitForOthersInTeam(i));
		}
		queuedTaskLatch = new CountDownLatch(QUEUE_SIZE);
		for (int i = 0; i < QUEUE_SIZE; i++) {
			executor.execute(queuedTask(i));
		}
		//wait for teamCountdown.getCount() == 1 to happen? Shutdown must work anyway, but without such check the test is more un-determined...  
		LOGGER.debug("Ready for shutdown");
		executor.shutdown(); //executor is shut down twice actually - also in tearDown
		teamCountdown.countDown(); //after initiating shutdown, allow already started threads to finish
		boolean wasShutDown = executor.awaitTermination(1, TimeUnit.SECONDS);
		LOGGER.debug("wasShutDown {}", wasShutDown);
		assertTrue("Executor was supposed to be shut down", wasShutDown);
	}
	
	@Test(timeout=5000)
	public void checkPoolCanBeShutDownIfEmpty() throws Exception {
		executor.shutdown();
	}
	
	private Runnable waitForOthersInTeam(int name) {
		return new Runnable() {
			@Override
			public void run() {
				teamCountdown.countDown();
				LOGGER.debug("Starting to wait with {}", teamCountdown.getCount());
				try {
					teamCountdown.await();
					LOGGER.debug("Successfully waited for other team members");
				} catch (InterruptedException e) {
					errorCollector.addError(e);
					LOGGER.error("Was interrupted with {}", teamCountdown.getCount());
				}
			}
			
			@Override
			public String toString() {
				return "waitForOthersInTeam_" + name;
			}
		};
	}

	private Runnable queuedTask(int name) {
		return new Runnable() {
			@Override
			public void run() {
				LOGGER.debug("Additional task outside maxPoolSize joined with {} left", queuedTaskLatch.getCount());
				queuedTaskLatch.countDown();
			}

			@Override
			public String toString() {
				return "queuedTask_" + name;
			}
		};
	}
}
