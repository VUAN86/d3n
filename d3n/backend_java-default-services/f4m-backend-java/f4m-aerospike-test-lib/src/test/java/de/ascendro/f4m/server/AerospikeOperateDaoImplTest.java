package de.ascendro.f4m.server;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.Operation;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.MapOperation;
import com.aerospike.client.cdt.MapPolicy;
import com.aerospike.client.cdt.MapReturnType;
import com.aerospike.client.policy.GenerationPolicy;
import com.aerospike.client.policy.WritePolicy;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.dao.aerospike.RealAerospikeTestBase;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;

public class AerospikeOperateDaoImplTest extends RealAerospikeTestBase{

	private static final Logger LOGGER = LoggerFactory.getLogger(AerospikeOperateDaoImplTest.class);

	private static final String SET = "test-set";
	
	private static final String BIN_NAME1 = "test-bin1";
	private static final String BIN_NAME2 = "test-bin2";
	
	private static final String KEY = "test-key";
	
	private static final String MAP_KEY1 = "test-map-key-1";
	private static final String MAP_VALUE1 = "test-map-value-1";
	
	private static final String MAP_KEY2 = "test-map-key-2";
	private static final String MAP_VALUE2 = "test-map-value-2";

	private PrimaryKeyUtil<String> primaryKeyUtil;
	private JsonUtil jsonUtil;

	private AerospikeOperateDao aerospikeOperateDao;

	@Before
	@Override
	public void setUp()  {
		super.setUp();
		primaryKeyUtil = new PrimaryKeyUtil<>(config);
		jsonUtil = new JsonUtil();
		
		if (aerospikeClientProvider != null && !(aerospikeClientProvider instanceof AerospikeClientMockProvider)) {
			clearTestSet();
		}
	}
	
	@After
	@Override
	public void tearDown() {
		if (aerospikeClientProvider != null && !(aerospikeClientProvider instanceof AerospikeClientMockProvider)) {
			try {
				clearTestSet();
			} finally {
				super.tearDown();
			}
		}
	}

	private void clearTestSet() {
		LOGGER.info("Clearing Aerospike set {} within namespace {} at exit", SET, AerospikeConfigImpl.AEROSPIKE_NAMESPACE);
		clearSet(config.getProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE), SET);
	}
	
	@Override
	protected void setUpAerospike() {
		aerospikeOperateDao = new AerospikeOperateDaoImpl<PrimaryKeyUtil<String>>(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Test(expected = F4MEntryNotFoundException.class)
	public void testUpdateListIfNotCreated() {
		aerospikeOperateDao.updateList(SET, KEY, BIN_NAME1, (v, wp) -> {
			return Arrays.asList("fail");
		});
	}

	@Test
	public void testUpdateList() {
		aerospikeOperateDao.createOrUpdateList(SET, KEY, BIN_NAME1, (v, wp) -> {
			return new ArrayList<>();
		});
		aerospikeOperateDao.updateList(SET, KEY, BIN_NAME1, (v, wp) -> {
			return Arrays.asList("test1");
		});

		aerospikeOperateDao.updateList(SET, KEY, BIN_NAME1, (v, wp) -> {
			assertThat(v, containsInAnyOrder("test1"));
			return v;
		});
	}

	@Test
	public void testCreateOrUpdateList() {
		aerospikeOperateDao.<String> createOrUpdateList(SET, KEY, BIN_NAME1, (v, wp) -> {
			return new ArrayList<String>(Arrays.asList("test1"));
		});
		aerospikeOperateDao.<String> createOrUpdateList(SET, KEY, BIN_NAME1, (v, wp) -> {
			v.add("test2");
			return v;
		});
		aerospikeOperateDao.<String> updateList(SET, KEY, BIN_NAME1, (v, wp) -> {
			assertThat(v, containsInAnyOrder("test1", "test2"));
			return v;
		});
	}

	@Test(expected = F4MEntryNotFoundException.class)
	public void testUpdateMapIfNotCreated() {
		aerospikeOperateDao.<String, Integer> updateMap(SET, KEY, BIN_NAME1, (v, wp) -> {
			return null;
		});
	}

	@Test
	public void testUpdateMap() {
		aerospikeOperateDao.<String, String> createOrUpdateMap(SET, KEY, BIN_NAME1, (v, wp) -> {
			return new HashMap<String, String>();
		});
		aerospikeOperateDao.<String, String> updateMap(SET, KEY, BIN_NAME1, (v, wp) -> {
			v.put("test-key-1", "test-value-1");
			return v;
		});

		aerospikeOperateDao.<String, String> updateMap(SET, KEY, BIN_NAME1, (v, wp) -> {
			assertEquals("test-value-1", v.get("test-key-1"));
			return v;
		});
	}

	@Test
	public void testCreateOrUpdateMap() {
		aerospikeOperateDao.<String, String> createOrUpdateMap(SET, KEY, BIN_NAME1, (v, wp) -> {
			final Map<String, String> map = new HashMap<>();
			map.put("test-key-1", "test-value-1");
			return map;
		});

		aerospikeOperateDao.<String, String> updateMap(SET, KEY, BIN_NAME1, (v, wp) -> {
			assertEquals("test-value-1", v.get("test-key-1"));
			return v;
		});
	}
	
	@Test
	public void testCreateOrUpdateMapValueByKeyForMissingRecord() {
		aerospikeOperateDao.createOrUpdateMapValueByKey(SET, KEY, "anyBin", "anyKey", (v, wp) -> {
			return "anyValue";
		});
		assertEquals("anyValue", aerospikeOperateDao.getByKeyFromMap(SET, KEY, "anyBin", "anyKey"));
	}

	/**
	 * Test createOrUpdateMapValueByKey Test getByKeyFromMap
	 */
	@Test
	public void testCreateOrUpdateMapValueByKey() {
		final String mapKey = "test-key-1";
		final String mapValue = "test-value-1";
		aerospikeOperateDao.createString(SET, KEY, "anyBin", "anyValue");//Just to createRecord
		aerospikeOperateDao.createOrUpdateMapValueByKey(SET, KEY, BIN_NAME1, mapKey, (v, wp) -> {
			return mapValue;
		});
		assertEquals(mapValue, aerospikeOperateDao.getByKeyFromMap(SET, KEY, BIN_NAME1, mapKey));
	}
	
	@Test
	public void testOperateOnNotExistingRecord(){
		final Operation[] readOperations =
				new Operation[] { MapOperation.getByKey(BIN_NAME1, Value.get(MAP_KEY1), MapReturnType.VALUE),
						MapOperation.getByKey(BIN_NAME2, Value.get(MAP_KEY2), MapReturnType.KEY_VALUE) };
		aerospikeOperateDao.operate(SET, KEY, readOperations, (r, ops) -> {
			final Object value1 = r.getValue(BIN_NAME1);
			assertNull(value1);

			final Object value2 = r.getValue(BIN_NAME2);
			assertNull(value2);

			return Arrays.asList(MapOperation
					.put(MapPolicy.Default, BIN_NAME1, Value.get(MAP_KEY1), Value.get(MAP_VALUE1)), MapOperation
							.put(MapPolicy.Default, BIN_NAME2, Value.get(MAP_KEY2), Value.get(MAP_VALUE2)));
		});
		assertEquals(MAP_VALUE1, aerospikeOperateDao.getByKeyFromMap(SET, KEY, BIN_NAME1, MAP_KEY1));
		assertEquals(MAP_VALUE2, aerospikeOperateDao.getByKeyFromMap(SET, KEY, BIN_NAME2, MAP_KEY2));
	}

	@Test
	public void testOperateDualMapPut() {
		final Operation[] readOperations =
				new Operation[] { MapOperation.getByKey(BIN_NAME1, Value.get(MAP_KEY1), MapReturnType.VALUE),
						MapOperation.getByKey(BIN_NAME2, Value.get(MAP_KEY2), MapReturnType.VALUE) };

		aerospikeOperateDao.createString(SET, KEY, "anyBin", "anyValue");//Create record
		
		//Create
		aerospikeOperateDao.operate(SET, KEY, readOperations, (r, ops) -> {
			final Object value1 = r.getValue(BIN_NAME1);			
			assertNull(value1);

			final Object value2 = r.getValue(BIN_NAME2);			
			assertNull(value2);

			return Arrays.asList(MapOperation
					.put(MapPolicy.Default, BIN_NAME1, Value.get(MAP_KEY1), Value.get(MAP_VALUE1)), MapOperation
							.put(MapPolicy.Default, BIN_NAME2, Value.get(MAP_KEY2), Value.get(MAP_VALUE2)));
		});
		assertEquals(MAP_VALUE1, aerospikeOperateDao.getByKeyFromMap(SET, KEY, BIN_NAME1, MAP_KEY1));
		assertEquals(MAP_VALUE2, aerospikeOperateDao.getByKeyFromMap(SET, KEY, BIN_NAME2, MAP_KEY2));

		//Update
		aerospikeOperateDao.operate(SET, KEY, readOperations, (r, ops) -> {
			final Object value1 = r.getValue(BIN_NAME1);
			assertEquals(MAP_VALUE1, value1);

			final Object value2 = r.getValue(BIN_NAME2);
			assertEquals(MAP_VALUE2, value2);

			return Arrays.asList(MapOperation.put(MapPolicy.Default, BIN_NAME1, Value.get(MAP_KEY1), Value.get(MAP_VALUE1 + MAP_VALUE1)), 
					MapOperation.put(MapPolicy.Default, BIN_NAME2, Value.get(MAP_KEY2), Value.get(MAP_VALUE2 + MAP_VALUE2)));
		});
		assertEquals(MAP_VALUE1 + MAP_VALUE1, aerospikeOperateDao.getByKeyFromMap(SET, KEY, BIN_NAME1, MAP_KEY1));
		assertEquals(MAP_VALUE2 + MAP_VALUE2, aerospikeOperateDao.getByKeyFromMap(SET, KEY, BIN_NAME2, MAP_KEY2));
	}
	
	@Test
	public void testOperateMapMove() {
		final Operation[] readOperations =
				new Operation[] { MapOperation.getByKey(BIN_NAME1, Value.get(MAP_KEY1), MapReturnType.VALUE),
						MapOperation.getByKey(BIN_NAME2, Value.get(MAP_KEY2), MapReturnType.VALUE) };

		aerospikeOperateDao.createString(SET, KEY, "anyBin", "anyValue");//Create record
		
		//Create
		aerospikeOperateDao.operate(SET, KEY, readOperations, (r, ops) -> {
			return Arrays.asList(MapOperation
					.put(MapPolicy.Default, BIN_NAME1, Value.get(MAP_KEY1), Value.get(MAP_VALUE1)), MapOperation
							.put(MapPolicy.Default, BIN_NAME2, Value.get(MAP_KEY2), Value.get(MAP_VALUE2)));
		});
		assertEquals(MAP_VALUE1, aerospikeOperateDao.getByKeyFromMap(SET, KEY, BIN_NAME1, MAP_KEY1));
		assertEquals(MAP_VALUE2, aerospikeOperateDao.getByKeyFromMap(SET, KEY, BIN_NAME2, MAP_KEY2));

		//Move
		aerospikeOperateDao.operate(SET, KEY, readOperations, (r, ops) -> {
			final Object value1 = r.getValue(BIN_NAME1);
			assertEquals(MAP_VALUE1, value1);

			final Object value2 = r.getValue(BIN_NAME2);
			assertEquals(MAP_VALUE2, value2);

			return Arrays.asList(MapOperation.removeByKey(BIN_NAME1, Value.get(MAP_KEY1), MapReturnType.NONE), 
					MapOperation.put(MapPolicy.Default, BIN_NAME2, Value.get(MAP_KEY1), Value.get(value1)));
		});
		assertNull(aerospikeOperateDao.getByKeyFromMap(SET, KEY, BIN_NAME1, MAP_KEY1));
		assertEquals(MAP_VALUE1, aerospikeOperateDao.getByKeyFromMap(SET, KEY, BIN_NAME2, MAP_KEY1));
		assertEquals(MAP_VALUE2, aerospikeOperateDao.getByKeyFromMap(SET, KEY, BIN_NAME2, MAP_KEY2));
	}

	@Test
	public void testGetAllMap() {
		final String testKey1 = "test-key-1";
		final String testKey2 = "test-key-2";
		final String testValue = "test-value-1";

		aerospikeOperateDao.<String, String> createOrUpdateMap(SET, KEY, BIN_NAME1, (v, wp) -> {
			final Map<String, String> map = new HashMap<>();

			map.put(testKey1, testValue);
			map.put(testKey2, testValue);
			return map;
		});
		final Map<String, String> resultMap = aerospikeOperateDao.<String, String> getAllMap(SET, KEY, BIN_NAME1);
		assertEquals("Expected to have two result entries", 2, resultMap.size());
	}

	@Test
	public void testDeleteByKeyFromMap() {
		final String testKey1 = "test-key-1";
		final String testKey2 = "test-key-2";
		final String testValue1 = "test-value-1";
		final String testValue2 = "test-value-1";

		aerospikeOperateDao.<String, String> createOrUpdateMap(SET, KEY, BIN_NAME1, (v, wp) -> {
			final Map<String, String> map = new HashMap<>();

			map.put(testKey1, testValue1);
			map.put(testKey2, testValue2);
			return map;
		});

		aerospikeOperateDao.deleteByKeyFromMap(SET, KEY, BIN_NAME1, testKey1);

		final Map<String, String> resultMap = aerospikeOperateDao.<String, String> getAllMap(SET, KEY, BIN_NAME1);
		assertEquals("Expected to have two result entries", 1, resultMap.size());
		assertEquals(testValue2, resultMap.get(testKey2));
	}

	@Test(expected = F4MEntryNotFoundException.class)
	public void testDeleteByKeyFromMapIfNotExist() {
		aerospikeOperateDao.deleteByKeyFromMap(SET, KEY, BIN_NAME1, UUID.randomUUID().toString());
	}

	@Test
	public void testDeleteByKeyFromMapSilently() {
		final String testKey1 = "test-key-1";
		final String testValue1 = "test-value-1";

		aerospikeOperateDao.deleteByKeyFromMapSilently(SET, KEY, BIN_NAME1, UUID.randomUUID().toString());
		aerospikeOperateDao.<String, String> createOrUpdateMap(SET, KEY, BIN_NAME1, (v, wp) -> {
			final Map<String, String> map = new HashMap<>();

			map.put(testKey1, testValue1);
			return map;
		});

		aerospikeOperateDao.deleteByKeyFromMapSilently(SET, KEY, BIN_NAME1, testKey1);
		aerospikeOperateDao.deleteByKeyFromMapSilently(SET, KEY, BIN_NAME1, UUID.randomUUID().toString());

		final Map<String, String> resultMap = aerospikeOperateDao.<String, String> getAllMap(SET, KEY, BIN_NAME1);
		assertTrue(resultMap.isEmpty());
	}
	
	@Test
	public void testGetMapSize() throws Exception {
		assertNull(aerospikeOperateDao.getMapSize(SET, KEY, BIN_NAME1));
		
		aerospikeOperateDao.createJson(SET, KEY, BIN_NAME2, "anyJsonString");
		assertNull(aerospikeOperateDao.getMapSize(SET, KEY, BIN_NAME1));
		
		aerospikeOperateDao.createOrUpdateMapValueByKey(SET, KEY, BIN_NAME1, MAP_KEY1, (wp, v) -> MAP_VALUE1);
		assertEquals(Integer.valueOf(1), aerospikeOperateDao.getMapSize(SET, KEY, BIN_NAME1));
	}
	

	@Test
	public void testMapPutWithRealAerospike() {
		final String binName = "map";
		final String recordKey = "record1";

		try {
			aerospikeOperateDao.createOrUpdateMap(SET, recordKey, binName, (v, wp) -> {
				return new HashMap<>();
			});

			aerospikeOperateDao.createOrUpdateMapValueByKey(SET, recordKey, binName, "mapKey1", (v, wp) -> {
				return "test1";
			});
			aerospikeOperateDao.createOrUpdateMapValueByKey(SET, recordKey, binName, "mapKey2", (v, wp) -> {
				return "test2";
			});
			aerospikeOperateDao.createOrUpdateMapValueByKey(SET, recordKey, binName, "mapKey3", (v, wp) -> {
				return "test3";
			});

			final String result = aerospikeOperateDao.<String, String> createOrUpdateMapValueByKey(SET, recordKey, binName,
					"mapKey1", new UpdateCall<String>() {
						@Override
						public String update(String readResult, WritePolicy writePolicy) {
							return readResult + readResult;
						}
					});
			assertEquals("test1test1", result);
		} finally {
			// Delete test data
			aerospikeOperateDao.deleteSilently(SET, recordKey);
		}
	}

	/* Test is only relevant with real aerospike (mock doesn't handle generations) */
	@Test
	public void testConcurrentPutToMapDifferentMapKeys() throws InterruptedException {
		Assume.assumeNotNull(config.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST));
		final String binName = "map";
		final String recordKey = "record1";

		int numberOfConcurrentCalls = 20;
		int maxTimeoutSeconds = 5;

		List<Runnable> runnables = new ArrayList<>();

		IntStream.range(0, numberOfConcurrentCalls).forEach(value ->
				addMapUpdateRunnable(binName, recordKey, String.valueOf(value), runnables,
						(readResult, wp) -> "Value" + String.valueOf(value)));

		runConcurrently("Concurrent Aerospike add value to Map ", runnables, maxTimeoutSeconds);

		Map<String, String> readResult = aerospikeOperateDao.getAllMap(SET, recordKey, binName);

		assertEquals("Unexpected record count for map:" + readResult.toString(), numberOfConcurrentCalls, readResult.size());

		IntStream.range(0, numberOfConcurrentCalls).forEach(value -> {
				assertTrue("Read Result doesn't contain expected key: " + String.valueOf(value),
						readResult.containsKey(String.valueOf(value)));
				assertEquals("Read Result doesn't contain expected key -> value: "
						+ String.valueOf(value) + " -> " + "Value" + String.valueOf(value),
						"Value" + String.valueOf(value), readResult.get(String.valueOf(value)));
		});
	}

	/* Test is only relevant with real aerospike (mock doesn't handle generations) */
	@Test
	public void testConcurrentPutToMapSameMapKey() throws InterruptedException {
		Assume.assumeNotNull(config.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST));
		final String binName = "map";
		final String recordKey = "record1";
		final String mapKey = "mapKey";

		int numberOfConcurrentCalls = 20;
		int maxTimeoutSeconds = 5;

		List<Runnable> runnables = new ArrayList<>();

		IntStream.range(0, numberOfConcurrentCalls).forEach(value ->
				addMapUpdateRunnable(binName, recordKey, mapKey, runnables,
						(readResult, wp) -> {
					Integer currentValue = readResult != null ? Integer.valueOf((String) readResult) : 0;
					return String.valueOf(currentValue + 1);
				}));

		runConcurrently("Concurrent Aerospike add value to Map to same key", runnables, maxTimeoutSeconds);

		Map<String, String> readResult = aerospikeOperateDao.getAllMap(SET, recordKey, binName);

		assertEquals("Unexpected record count for map:" + readResult.toString(), 1, readResult.size());

		assertTrue("Read Result doesn't contain expected key: " + mapKey, readResult.containsKey(mapKey));
		assertEquals("Read Result doesn't contain expected key -> value: " + mapKey + " -> " + numberOfConcurrentCalls,
				Integer.valueOf(numberOfConcurrentCalls), Integer.valueOf(readResult.get(mapKey)));
	}

	private void addMapUpdateRunnable(String binName, String key, String mapKey, List<Runnable> runnables,
			UpdateCall<Object> updateCall) {
		runnables.add(() -> aerospikeOperateDao.createOrUpdateMapValueByKey(SET, key, binName, mapKey, updateCall));
	}

	@Test
	public void testMapOperationsWithRealAerospike() {
		config.setProperty(AerospikeConfigImpl.AEROSPIKE_POLICY_WRITE_GENERATION, GenerationPolicy.NONE.toString());

		String key = "key";
		String binName = "bin";
		aerospikeOperateDao.deleteSilently(SET, key);

		// Check that no existing test data
		assertEquals(0, aerospikeOperateDao.getAllMap(SET, key, binName).size());

		try {
			// Create new map and put in value
			aerospikeOperateDao.createOrUpdateMap(SET, key, binName, (v, wp) -> {
				return new HashMap<>();
			});
			aerospikeOperateDao.<String, String> createOrUpdateMapValueByKey(SET, key, binName, "key1", (v, wp) -> {
				return "value1";
			});

			assertEquals(1, aerospikeOperateDao.getAllMap(SET, key, binName).size());

			// Add other value
			aerospikeOperateDao.createOrUpdateMapValueByKey(SET, key, binName, "key2", (v, wp) -> {
				return "value2";
			});

			assertEquals(2, aerospikeOperateDao.getAllMap(SET, key, binName).size());

			// Update exiting record value
			aerospikeOperateDao.createOrUpdateMapValueByKey(SET, key, binName, "key2", (v, wp) -> {
				return "value2.1";
			});

			assertEquals(2, aerospikeOperateDao.getAllMap(SET, key, binName).size());
			assertEquals("value2.1", aerospikeOperateDao.getAllMap(SET, key, binName).get("key2"));

			// Get by key
			String resultString = aerospikeOperateDao.getByKeyFromMap(SET, key, binName, "key2");
			assertEquals("value2.1", resultString);

			// Get all map
			aerospikeOperateDao.createOrUpdateMapValueByKey(SET, key, binName, "key3", (v, wp) -> {
				return "value1";
			});
			
			Map<String, String> result = aerospikeOperateDao.getAllMap(SET, key, binName);
			assertEquals(3, result.size());
			assertEquals("value1", result.get("key1"));
			assertEquals("value2.1", result.get("key2"));
			assertEquals("value1", result.get("key3"));

			// Remove by key
			aerospikeOperateDao.deleteByKeyFromMap(SET, key, binName, "key1");
			result = aerospikeOperateDao.getAllMap(SET, key, binName);
			assertEquals(2, result.size());
			assertFalse(result.containsKey("key1"));
		} finally {
			// Delete test data
			aerospikeOperateDao.deleteSilently(SET, key);
		}
	}

}
