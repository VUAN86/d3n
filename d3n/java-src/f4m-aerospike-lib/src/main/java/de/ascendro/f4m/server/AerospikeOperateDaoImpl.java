package de.ascendro.f4m.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.ResultCode;
import com.aerospike.client.Value;
import com.aerospike.client.cdt.MapOperation;
import com.aerospike.client.cdt.MapPolicy;
import com.aerospike.client.cdt.MapReturnType;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;

public class AerospikeOperateDaoImpl<P extends PrimaryKeyUtil<?>> extends AerospikeDaoImpl<P> implements AerospikeOperateDao {

	private static final Logger LOGGER = LoggerFactory.getLogger(AerospikeOperateDaoImpl.class);

	@Inject
	public AerospikeOperateDaoImpl(Config config, P primaryKeyUtil, JsonUtil jsonUtil,
			AerospikeClientProvider aerospikeClientProvider) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}
 
	@Override
	public void deleteByKeyFromMap(String set, String key, String binName, Object mapKey) {
		if (readRecord(set, key) != null) {
			final Key k = new Key(getNamespace(), set, key);
			getAerospikeClient().operate(null, k, MapOperation.removeByKey(binName, Value.get(mapKey), MapReturnType.NONE));
		} else {
			throw new F4MEntryNotFoundException();
		}
	}

	@Override
	public void deleteByKeyFromMapSilently(String set, String key, String binName, Object mapKey) {
		if (readRecord(set, key) != null) {
			final Key k = new Key(getNamespace(), set, key);
			getAerospikeClient().operate(null, k, MapOperation.removeByKey(binName, Value.get(mapKey), MapReturnType.NONE));
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <K, V> Map<K, V> updateMap(String set, String key, String binName, UpdateCall<Map<K, V>> updateCall) {
		final Value value = update(set, key, binName, updateCall, new SameValueTransformator<>());
		return (Map<K, V>) value.getObject();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <K, V> Map<K, V> createOrUpdateMap(String set, String key, String binName, UpdateCall<Map<K, V>> updateCall) {
		final Value value = createOrUpdate(set, key, binName, updateCall, new SameValueTransformator<>());
		return (Map<K, V>) value.getObject();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <K, V> V createOrUpdateMapValueByKey(final String set, final String key, final String binName, final K mapKey,
			UpdateCall<V> updateCallback) {
		final Value mapKeyAsValue = Value.get(mapKey);
		final Operation getByKeyOperation = MapOperation.getByKey(binName, mapKeyAsValue, MapReturnType.VALUE);
		LOGGER.debug("CreateOrUpdate MapValueByKey in set '{}', key '{}', bin '{}', mapKey '{}'", set, key, binName,mapKey);

		final Record resultRecord = operate(set, key, new Operation[]{getByKeyOperation},
				(readResult, writePolicy) -> {
						final V resultValue = updateCallback.update((V) readResult.getValue(binName), writePolicy);
						return Arrays.asList(MapOperation.put(MapPolicy.Default, binName, mapKeyAsValue, Value.get(resultValue)));
				});
		
		return (V) getLastOperationValue(resultRecord.getValue(binName));
	}
	
	private Object getLastOperationValue(Object value){
		Object result = null;
		if(value !=null){
			if(value instanceof List ){
				final List<?> valueAsList = (List<?>) value;
				if(!valueAsList.isEmpty()){
					result = valueAsList.get(valueAsList.size() - 1);
				}
			}else{
				result = value;
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <K, V> V getByKeyFromMap(String set, String key, String binName, K mapKey) {
		Key k = new Key(getNamespace(), set, key);
		LOGGER.debug("Reading ByKeyFromMap from set '{}', key '{}', bin '{}', mapKey '{}'", set, key, binName,mapKey);
		Record record = getAerospikeClient().operate(null, k,
				MapOperation.getByKey(binName, Value.get(mapKey), MapReturnType.KEY_VALUE));
		final V result;
		if (record != null) {		
			final List<Entry<K, V>> list = (List<Entry<K, V>>) record.getList(binName);
			result = list != null && !list.isEmpty()? list.get(0).getValue(): null;
		}else{
			result = null;
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <K, V> Map<K, V> getAllMap(String set, String key, String... binNames) {
		Key k = new Key(getNamespace(), set, key);
		LOGGER.debug("Reading AllMap from set '{}', key '{}', bin '{}'", set, key, binNames);
		Record record = getAerospikeClient().get(null, k, binNames);
		final Map<K, V> result = new HashMap<>();
		if(record != null){			
			Stream.of(binNames).forEach(binName -> {
				Map<K, V> map = (Map<K, V>) record.getMap(binName);
				if (map != null) {
					result.putAll(map);
				}
			});
		}
		return result;
	}
	
	@Override
	public Integer getMapSize(String set, String key, String binName) {
		final Key k = new Key(getNamespace(), set, key);
		final Record resultRecord = getAerospikeClient().operate(null, k, MapOperation.size(binName));
		final Number size = resultRecord != null ? (Number) resultRecord.getValue(binName) : null;
		return size != null ? size.intValue() : null;
	}

	/* Run AerospikeOperateDaoImplTest.testConcurrentPutToMapDifferentMapKeys and
	 * AerospikeOperateDaoImplTest.testConcurrentPutToMapSameMapKey manually with real Aerospike
	 * if you make changes to this
	 */
	@Override
	public Record operate(String set, String key, Operation[] readOperations,
			OperateCall<Record, List<Operation>> operateCall) {
		//CAUTION: Not tested with all possible operate combinations, tested within simple map operations (put+put, removeByKey+put)

		Boolean generationError;
		Integer attemptCount = 0;

		final Key k = new Key(getNamespace(), set, key);
		Record record = null;
		do {
			generationError = false;

			try {
				record = operateReadWrite(k, operateCall, readOperations);
			} catch (AerospikeException aEx) {
				if (aEx.getResultCode() == ResultCode.GENERATION_ERROR && attemptCount < GENERATION_ERROR_MAX_RETRIES) {
					LOGGER.debug("Attempt nb. {} failed with generation error for operating set[{}], key[{}] using {} read operations: {}",
							attemptCount, set, key, readOperations.length, aEx.getMessage());
					attemptCount++;
					generationError = true;
				} else {
					throw aEx;
				}
			}
		} while (generationError);

		return record;
	}

	private Record operateReadWrite(Key key, OperateCall<Record, List<Operation>> writeOperateCalls, Operation[] readOperations) {
		Record record = getAerospikeClient().operate(null, key, readOperations);
		if (record == null) { //Check if something to read exists
			record = new Record(new HashMap<>(), 0, 0); // this is so we have generation = 0 for a new record
		}
		//Read
		final WritePolicy wp = getWritePolicy(record.generation);
		wp.recordExistsAction = RecordExistsAction.UPDATE; // we cannot use update only here
		final List<Operation> writeOperations = writeOperateCalls.operate(record, wp);

		//Write
		final List<Operation> allOperations = new ArrayList<>();
		if (!CollectionUtils.isEmpty(writeOperations)) {
			allOperations.addAll(writeOperations);
		}
		if(readOperations != null && readOperations.length > 0){
			Collections.addAll(allOperations, readOperations);
		}
		return getAerospikeClient().operate(wp, key, allOperations.toArray(new Operation[allOperations.size()]));
	}

	@FunctionalInterface
	protected interface OperateCall<I, O> {
		O operate(I readResult, WritePolicy writePolicy);
	}

}
