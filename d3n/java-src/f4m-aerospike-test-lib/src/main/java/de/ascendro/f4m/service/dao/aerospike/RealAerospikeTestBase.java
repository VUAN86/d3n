package de.ascendro.f4m.service.dao.aerospike;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.ScanCallback;
import com.aerospike.client.policy.ConsistencyLevel;
import com.aerospike.client.policy.ScanPolicy;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.exception.F4MAerospikeResultCodes;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.util.register.ServiceMonitoringRegister;

public abstract class RealAerospikeTestBase {

	private static final Logger LOGGER = LoggerFactory.getLogger(RealAerospikeTestBase.class);
	
	private ServiceMonitoringRegister serviceMonitoringRegister = new ServiceMonitoringRegister();
	
	protected Config config;
	protected AerospikeClientProvider aerospikeClientProvider;

	@Before
	public void setUp() {
		config = createConfig();
		
		// setting real Aerospike host
		// if not set, tests will be skipped
//				config.setProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST, "35.157.88.32");//dev-aerospike4
		
		if(StringUtils.isNotEmpty(config.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST))){
			setUpRealAerospikeClientProvider();
		}else{
			setUpMockAerospikeClientProvider();
		}

		setUpAerospike();
		assertTrue(aerospikeClientProvider.get()
				.isConnected());
	}
	
	protected Config createConfig(){
		return new AerospikeConfigImpl(); 
	}

	private void setUpRealAerospikeClientProvider() {
		aerospikeClientProvider = new AerospikeClientProvider(config, serviceMonitoringRegister);
	}
	
	private void setUpMockAerospikeClientProvider() {
		aerospikeClientProvider = new AerospikeClientMockProvider(config, serviceMonitoringRegister);
	}

	@After
	public void tearDown() {
		if (aerospikeClientProvider != null) {
			aerospikeClientProvider.get()
					.close();
		}
	}
	
	protected void clearSet(String namespace, String set) {
		clearSet(aerospikeClientProvider.get(), namespace, set);
	}
	
	public static void clearSet(IAerospikeClient iAerospikeClient, String namespace, String set){
		LOGGER.info("Clearing set[{}] within namespace[{}]", set, namespace);
		final ScanPolicy scanPolicy = new ScanPolicy();
		scanPolicy.consistencyLevel = ConsistencyLevel.CONSISTENCY_ALL;
		iAerospikeClient.scanAll(scanPolicy, namespace, set, new ScanCallback() {

			@Override
			public void scanCallback(Key key, Record record) throws AerospikeException {
				if(key.namespace.equals(namespace) && key.setName.equals(set)){
					iAerospikeClient.delete(null, key);
				}
			}
		});
		LOGGER.info("Done clearing set[{}] within namespace[{}]", set, namespace);
	}
	
	public void clearProfileSets(String namespace){
		clearSet(namespace, "auth");
		clearSet(namespace, "profile");
		clearSet(namespace, "profileSync");
		
		clearSet(namespace, "questionStatistics");
		clearSet(namespace, "gameInstance");
		clearSet(namespace, "gameHistory");
		clearSet(namespace, "multiplayerGameInstance");
		clearSet(namespace, "results");
		clearSet(namespace, "multiplayerResults");
		clearSet(namespace, "gameStatistics");
	}

	protected abstract void setUpAerospike();


	protected void dropIndex(String namespace, String set, String indexName) {
		try {
			aerospikeClientProvider.get().dropIndex(null, namespace, set, indexName);
		} catch (AerospikeException aEx) {
			if (aEx.getResultCode() != F4MAerospikeResultCodes.AS_PROTO_RESULT_FAIL_INDEX_NOT_FOUND.getCode()) {
				throw aEx;
			}
		}
	}

	protected static void runConcurrently(final String message, final List<? extends Runnable> runnables,
			final int maxTimeoutSeconds) throws InterruptedException {
		final int numThreads = runnables.size();
		final List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());
		final ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
		try {
			final CountDownLatch allExecutorThreadsReady = new CountDownLatch(numThreads);
			final CountDownLatch afterInitBlocker = new CountDownLatch(1);
			final CountDownLatch allDone = new CountDownLatch(numThreads);
			for (final Runnable submittedTestRunnable : runnables) {
				threadPool.submit(() -> {
					allExecutorThreadsReady.countDown();
					try {
						afterInitBlocker.await();
						submittedTestRunnable.run();
					} catch (final Throwable e) {
						exceptions.add(e);
					} finally {
						allDone.countDown();
					}
				});
			}
			// wait until all threads are ready
			assertTrue("Timeout initializing threads! Perform long lasting initializations before passing runnable list " +
					"to runConcurrently", allExecutorThreadsReady.await(runnables.size() * 10, TimeUnit.MILLISECONDS));
			// start all test runners
			afterInitBlocker.countDown();
			assertTrue(message +" timeout! More than " + maxTimeoutSeconds + " seconds",
					allDone.await(maxTimeoutSeconds, TimeUnit.SECONDS));
		} finally {
			threadPool.shutdownNow();
		}
		assertTrue(message + "failed with exception(s)" + exceptions, exceptions.isEmpty());
	}
	
}
