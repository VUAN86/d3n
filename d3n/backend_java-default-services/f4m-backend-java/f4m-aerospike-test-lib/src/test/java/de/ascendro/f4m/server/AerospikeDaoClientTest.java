package de.ascendro.f4m.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.Info;
import com.aerospike.client.Record;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.PredExp;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;

public class AerospikeDaoClientTest extends RealAerospikeTestBase {

	private static final Logger LOGGER = LoggerFactory.getLogger(AerospikeDaoClientTest.class);
	
	private PrimaryKeyUtil<String> primaryKeyUtil = new PrimaryKeyUtil<>(config);
	private AerospikeDao aerospikeDao;
	private JsonUtil jsonUtil = new JsonUtil();

	private static final String binName = "testBinName";
	private static final String binName2 = "testBinName2";
	private static final String set = "testSet";
	
	@Override
	@Before
	public void setUp() {
		super.setUp();
		clearSet(config.getProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE), set);
	}
	
	@Override
	@After
	public void tearDown() {
		try {
			clearSet(config.getProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE), set);
		} finally {
			super.tearDown();
		}
	};

	@Override
	protected void setUpAerospike() {
		aerospikeDao = new AerospikeDaoImpl<PrimaryKeyUtil<String>>(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}
	
	@Test
	public void testConnection() {
		assertTrue(aerospikeDao.isConnected());
	}

	@Test
	public void testWriteReadDeleteStringValue() {
		String key = "testKey";

		String binValue = "testBinValue";

		aerospikeDao.createString(set, key, binName, binValue);

		String value = aerospikeDao.readString(set, key, binName);
		assertEquals(binValue, value);

		aerospikeDao.delete(set, key);
		String nullValue = aerospikeDao.readString(set, key, binName);
		assertNull(nullValue);
	}
	
    @Test
    public void checkAerospikeVersion(){
    	Assume.assumeNotNull(config.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST));
    	
		try(final Node firstNode = aerospikeClientProvider.get().getNodes()[0]){
			final String version = Info.request(firstNode, "version");
			LOGGER.info("Running Aerospike version: {}, enterprise: {}", version, 
					((AerospikeDaoImpl<?>)aerospikeDao).isEnterprise(firstNode));
		}
    }
	
	@Test
	public void testWriteReadDeleteJsonValue() {
		String key = "testKey";

		String binValue = "testBinValue";

		aerospikeDao.createJson(set, key, binName, binValue);

		String value = aerospikeDao.readJson(set, key, binName);
		assertEquals(binValue, value);

		aerospikeDao.delete(set, key);
		String nullValue = aerospikeDao.readJson(set, key, binName);
		assertNull(nullValue);
	}

	@Test(expected = F4MEntryNotFoundException.class)
	//AerospikeException: Error Code 2: Key not found
	public void testUpdateMissingJson() {
		final String key = "testKey";

		final String binValue = "binValueResult";

		aerospikeDao.updateJson(set, key, binName, new UpdateCall<String>() {

			@Override
			public String update(String readResult, WritePolicy writePolicy) {
				return binValue;
			}
		});

		assertEquals(binValue, aerospikeDao.readJson(set, key, binName));
	}

	@Test
	public void testUpdateJson() {
		final String key = "testKey";

		final String binValue = "binValueResult";

		aerospikeDao.createJson(set, key, binName, binValue);

		aerospikeDao.updateJson(set, key, binName, new UpdateCall<String>() {

			@Override
			public String update(String readResult, WritePolicy writePolicy) {
				return binValue;
			}
		});

		assertEquals(binValue, aerospikeDao.readJson(set, key, binName));
	}

	@Test
	public void testCreateOrUpdateJson() {
		final String key = "testKey";

		final String binValue = "binValueResult";

		assertEquals(binValue, aerospikeDao.createOrUpdateJson(set, key, binName, (result, policy) -> binValue));
		assertEquals(binValue, aerospikeDao.readJson(set, key, binName));
		final String updatedBinValue = "updatedBinValueResult";
		assertEquals(updatedBinValue, aerospikeDao.createOrUpdateJson(set, key, binName, (result, policy) -> updatedBinValue));
		assertEquals(updatedBinValue, aerospikeDao.readJson(set, key, binName));
	}
	
	@Test(expected = F4MEntryNotFoundException.class)
	//AerospikeException: Error Code 2: Key not found
	public void testUpdateMissingString() {
		final String key = "testKey";

		final String binValue = "binValueResult";

		aerospikeDao.updateString(set, key, binName, new UpdateCall<String>() {

			@Override
			public String update(String readResult, WritePolicy writePolicy) {
				return binValue;
			}
		});

		assertEquals(binValue, aerospikeDao.readString(set, key, binName));
	}

	@Test
	public void testUpdateString() {
		final String key = "testKey";

		final String binValue = "binValueResult";

		aerospikeDao.createString(set, key, binName, binValue);

		aerospikeDao.updateString(set, key, binName, new UpdateCall<String>() {

			@Override
			public String update(String readResult, WritePolicy writePolicy) {
				return binValue;
			}
		});

		assertEquals(binValue, aerospikeDao.readString(set, key, binName));
	}

	@Test
	public void testDelete() {
		final String key = "testKey";

		final String binValue = "binValueResult";

		aerospikeDao.createString(set, key, binName, binValue);
		aerospikeDao.delete(set, key);

		assertNull(aerospikeDao.readString(set, key, binName));
	}	
	
	@Test
	public void testAdd(){
		final String key = "anyKey";
		aerospikeDao.add(set, key, binName, +1);
		assertEquals(Long.valueOf(1L), aerospikeDao.readLong(set, key, binName));
		
		aerospikeDao.add(set, key, binName2, +1);
		assertEquals(Long.valueOf(1L), aerospikeDao.readLong(set, key, binName));
		assertEquals(Long.valueOf(1L), aerospikeDao.readLong(set, key, binName2));
		
		aerospikeDao.add(set, key, binName, +5);
		aerospikeDao.add(set, key, binName2, +7);
		assertEquals(Long.valueOf(6L), aerospikeDao.readLong(set, key, binName));
		assertEquals(Long.valueOf(8L), aerospikeDao.readLong(set, key, binName2));
		
		aerospikeDao.add(set, key, binName, -3);
		assertEquals(Long.valueOf(3L), aerospikeDao.readLong(set, key, binName));
		
		aerospikeDao.add(set, key, binName, -3);
		assertEquals(Long.valueOf(0L), aerospikeDao.readLong(set, key, binName));
		
		aerospikeDao.add(set, key, binName, -100);
		assertEquals(Long.valueOf(-100L), aerospikeDao.readLong(set, key, binName));
	}
	
	@Test
	public void testReadJsons() {
		// prepare
		String key1 = "key1";
		String key2 = "key2";
		String value1 = "value1";
		String value2 = "value2";

		aerospikeDao.createJson(set, key1, binName, value1);
		aerospikeDao.createJson(set, key2, binName, value2);
		
		// test
		String[] keys = new String[] { key1, "keyOfNotExistingRecord", key2 };
		String[] jsons = aerospikeDao.readJsons(set, keys, binName);
		
		// validate
		assertEquals(keys.length, jsons.length);
		assertEquals(value1, jsons[0]);
		assertNull(jsons[1]);
		assertEquals(value2, jsons[2]);
	}
	
	@Test
	public void testScanUsingPredicateFilter(){
		Assume.assumeTrue(StringUtils.isNotEmpty(config.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST)));
		aerospikeDao.createOrUpdateString(set, "key1", binName, (v, wp) -> "abc");
		aerospikeDao.createOrUpdateString(set, "key2", binName, (v, wp) -> "abc123");
		aerospikeDao.createOrUpdateString(set, "key3", binName, (v, wp) -> "123abc");
		
		final List<Record> records = new CopyOnWriteArrayList<>();
		final PredExp[] predExp = new PredExp[] {
			PredExp.stringBin(binName),
			PredExp.stringValue("abc123"),
			PredExp.stringEqual()
		};
		aerospikeDao.scanUsingPredicateFilter(set, new String[] { binName }, predExp,
				(record) -> records.add(record));
		assertEquals("Unexpected record count", 1, records.size());

		assertEquals(records.get(0).getString(binName), "abc123"); 
		
		aerospikeDao.deleteSilently(set, "key1");
		aerospikeDao.deleteSilently(set, "key2");
		aerospikeDao.deleteSilently(set, "key3");
	}

	/* Test is only relevant with real aerospike (mock doesn't handle generations) */
	@Test
	public void testConcurrentWriteToListRecord() throws InterruptedException {
		Assume.assumeNotNull(config.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST));
		final String key = "testKey";

		int numberOfConcurrentCalls = 20;
		int maxTimeoutSeconds = 5;

		List<Runnable> runnables = new ArrayList<>();

		IntStream.range(0, numberOfConcurrentCalls).forEach(value ->
				addCreateOrUpdateRunnable(key, String.valueOf(value), runnables));

		runConcurrently("Concurrent Aerospike write to List ", runnables, maxTimeoutSeconds);

		List<String> readResult = aerospikeDao.readList(set, key, binName);

		assertEquals("Unexpected record count for array:" + readResult.toString(), numberOfConcurrentCalls, readResult.size());

		IntStream.range(0, numberOfConcurrentCalls).forEach(value ->
				assertTrue("Read Result doesn't contain expected value: " + String.valueOf(value),
						readResult.contains(String.valueOf(value))));
	}

	/* Test is only relevant with real aerospike (mock doesn't handle generations) */
	@Test
	public void testConcurrentAdd() throws InterruptedException {
		Assume.assumeNotNull(config.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST));
		final String key = "testKey";

		int numberOfConcurrentCalls = 20;
		int maxTimeoutSeconds = 5;

		List<Runnable> runnables = new ArrayList<>();

		IntStream.range(0, numberOfConcurrentCalls).forEach(value -> addAddRunnable(key, +1, runnables));

		runConcurrently("Concurrent Aerospike Add ", runnables, maxTimeoutSeconds);

		Long readResult = aerospikeDao.readLong(set, key, binName);
		assertEquals("Unexpected value in counter:" + readResult, Long.valueOf(numberOfConcurrentCalls), readResult);
	}

	private void addCreateOrUpdateRunnable(String key, String value, List<Runnable> runnables) {
		runnables.add(() -> {
			LOGGER.debug("Writing {} concurrently.", value);
			aerospikeDao.createOrUpdateList(set, key, binName, (UpdateCall<List<String>>) (readResult, writePolicy) -> {
				List<String> result = readResult;
				if (result == null) {
					result = new ArrayList<>();
				}
				result.add(value);
				return result;
			});
		});
	}

	private void addAddRunnable(String key, int value, List<Runnable> runnables) {
		runnables.add(() -> {
			LOGGER.debug("Adding {} concurrently.", value);
			aerospikeDao.add(set, key, binName, value);
		});
	}

}
