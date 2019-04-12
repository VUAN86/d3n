package de.ascendro.f4m.service.dao.aerospike;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Host;
import com.aerospike.client.Key;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;
import com.aerospike.client.ResultCode;
import com.aerospike.client.ScanCallback;
import com.aerospike.client.Value;
import com.aerospike.client.Value.BytesValue;
import com.aerospike.client.Value.IntegerValue;
import com.aerospike.client.cdt.MapBase;
import com.aerospike.client.cdt.MapReturnType;
import com.aerospike.client.cluster.Cluster;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.cluster.NodeValidator;
import com.aerospike.client.command.MultiCommand;
import com.aerospike.client.large.LargeList;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.Policy;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.IndexCollectionType;
import com.aerospike.client.query.IndexType;
import com.aerospike.client.query.KeyRecord;
import com.aerospike.client.query.PredExp;
import com.aerospike.client.query.QueryExecutor;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import com.aerospike.client.task.IndexTask;
import com.aerospike.client.util.Unpacker;
import com.github.srini156.aerospike.client.MockAerospikeClient;

import de.ascendro.f4m.server.exception.F4MAerospikeResultCodes;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;

public class AerospikeClientMock extends MockAerospikeClient {
	private static final int MAX_BIN_NAME_LENGTH = 14;
	private static final String DATA_FIELD_NAME = "data";
	private static final String PUT_METHOD_NAME = "put";
	private static final int MAP_DATA_OFFSET = 2;
	private List<String> createdIndexes = new ArrayList<>(1);

	@Override
	public LargeList getLargeList(WritePolicy policy, Key key, String binName) {
		return super.getLargeList(policy, key, binName);
	}

	@Override
	public void put(WritePolicy policy, Key key, Bin... bins) throws AerospikeException {
		validateKey(key);
		validateBinSize(bins);
		putInSingleStep(policy, key, bins);
	}
	
	@Override
	public void add(WritePolicy policy, Key key, Bin... bins) throws AerospikeException {
		validateKey(key);
		validateBinSize(bins);
		addInSingleStep(policy, key, bins);
	}

	private synchronized void addInSingleStep(WritePolicy policy, Key key, Bin[] bins) {
		final Record originalRecord = get(null, key);
		
		//add new value to original value
		final Bin[] resultBins = Arrays.stream(bins)
			.map(b -> addToBin(originalRecord, b))
			.toArray(size -> new Bin[size]);
		
		final Bin[] targetBins;
		if(originalRecord != null){
			//replace bins
			Arrays.stream(resultBins)
				.forEach(b -> originalRecord.bins.put(b.name, b.value.getObject()));
			//convert to bins
			targetBins = originalRecord.bins.entrySet().stream()
				.map(e -> new Bin(e.getKey(), e.getValue()))
				.toArray(size -> new Bin[size]);
		}else{
			targetBins = resultBins;
		}
		put(policy, key, targetBins);
	}
	
	private Bin addToBin(Record originalRecord, Bin newBin){
		final long currentValue;
		if(originalRecord != null && originalRecord.bins.containsKey(newBin.name) && originalRecord.getValue(newBin.name) != null){
			currentValue = originalRecord.getLong(newBin.name);
		}else{
			currentValue = 0L;
		}
		return new Bin(newBin.name, currentValue + (Long)newBin.value.getObject());
	}

	private void validateKey(Key key) {
		Validate.notNull(key);
		Validate.notNull(key.namespace);
		Validate.notNull(key.setName);		
	}

	private void validateBinSize(Bin[] bins) {
		for(Bin bin : bins){
			if(bin.name.length() > MAX_BIN_NAME_LENGTH){
				throw new AerospikeException(F4MAerospikeResultCodes.AS_PROTO_RESULT_FAIL_BIN_NAME.getCode());
			}
		}		
	}

	@Override
	public void scanAll(ScanPolicy policy, String namespace, String setName, ScanCallback callback, String... binNames)
			throws AerospikeException {
		for (Entry<Key, Record> entry : getData().entrySet()) {
			callback.scanCallback(entry.getKey(), entry.getValue());
		}
	}

	protected synchronized void putInSingleStep(WritePolicy policy, Key key, Bin... bins) {
		if (policy != null && policy.recordExistsAction != null) {
			handleRecordExistsActions(policy, key);
		}		
		super.put(policy, key, convertIntToLong(bins));
	}

	private Bin[] convertIntToLong(Bin[] bins) {
		for(int i = 0; i < bins.length; i++){
			final Bin bin = bins[i];
			if(bin.value != null && bin.value instanceof IntegerValue){
				final IntegerValue integerValue = (IntegerValue) bin.value;
				final Integer valueAsInteger = (Integer)integerValue.getObject();
				final Long valueAsLong = valueAsInteger != null ? valueAsInteger.longValue() : null;
				bins[i] = new Bin(bin.name, Value.get(valueAsLong));
			}
		}
		return bins;
	}

	private void handleRecordExistsActions(WritePolicy policy, Key key) {
		switch (policy.recordExistsAction) {
		case CREATE_ONLY: {
			final Record existingRecord = get(null, key);
			if (existingRecord != null) {
				throw new AerospikeException(ResultCode.KEY_EXISTS_ERROR);
			}
			break;
		}
		case UPDATE_ONLY: {
			final Record existingRecord = get(null, key);
			if (existingRecord == null) {
				throw new F4MEntryNotFoundException();
			}
			break;
		}
		case UPDATE://no need to check as create or updates
			break;
		case REPLACE://no need to check
			break;
		case REPLACE_ONLY:
			if (get(null, key) == null) {
				throw new F4MEntryNotFoundException();
			}
			break;
		}
	}

	@Override
	public synchronized Record operate(WritePolicy policy, Key key, Operation... operations) throws AerospikeException {
		if(policy != null){
			handleRecordExistsActions(policy, key);
		}
		Record returnedRecord = new Record(new HashMap<>(), 0, 0);
		for (Operation operation : operations) {
			Record existingRecord = getData().get(key);
			switch (operation.type) {
			case ADD:
				existingRecord = ensureExistingRecordExists(key, existingRecord);
				Object value = existingRecord.getValue(operation.binName);
				Object result;
				if (value == null) {
					result = operation.value.getObject();
				} else {
					Number n = (Number) value;
					if (n instanceof Integer) {
						result = ((int) n) + operation.value.toInteger();
					} else if (n instanceof Long) {
						result = ((long) n) + operation.value.toLong();
					} else {
						throw new UnsupportedOperationException("Adding of " + n.getClass() + " not implemented.");
					}
				}
				existingRecord.bins.put(operation.binName, result);
				break;
			case APPEND:
			case WRITE:
			case PREPEND:
				existingRecord = ensureExistingRecordExists(key, existingRecord);
				existingRecord.bins.put(operation.binName, operation.value.getObject());
				break;
			case MAP_MODIFY:
				existingRecord = onMapModify(existingRecord, policy, key, operation);
				break;
			case MAP_READ:
				Object mapReadResult = onMapRead(existingRecord, policy, key, operation);
				returnedRecord.bins.put(operation.binName, mapReadResult);
				break;
			case READ:
				if (operation.binName == null) {
					returnedRecord.bins.putAll(existingRecord.bins);
				} else {
					returnedRecord.bins.put(operation.binName, existingRecord == null ? null : existingRecord.bins.get(operation.binName));
				}
				break;
			default:
				throw new UnsupportedOperationException("Unssuported operation type: " + operation.type);
			}
		}
		return returnedRecord;
	}

	private Record ensureExistingRecordExists(Key key, Record existingRecord) {
		if (existingRecord == null) {
			existingRecord = new Record(new HashMap<>(), 0, 0);
			getData().put(key, existingRecord);
		}
		return existingRecord;
	}

	@SuppressWarnings("unchecked")
	private Record onMapModify(Record existingRecord, WritePolicy policy, Key key, Operation operation) {
		final byte[] bytes = (byte[]) ((BytesValue) operation.value).getObject();
		final List<?> operationValues = (List<?>) Unpacker.unpackObject(bytes, MAP_DATA_OFFSET, bytes.length);
		final Map<Object, Object> binValue;
		if (bytes[1] == MapBaseCopy.REMOVE_BY_KEY) {
			// Remove from map (this is guess and might be wrong but is working for now)
			if (existingRecord != null && existingRecord.bins.containsKey(operation.binName)) {
				binValue = (Map<Object, Object>) existingRecord.bins.get(operation.binName);
				binValue.remove(operationValues.get(1));
			} else {
				throw new F4MEntryNotFoundException();
			}
		} else {
			// Add in map
			existingRecord = ensureExistingRecordExists(key, existingRecord);
			if (!existingRecord.bins.containsKey(operation.binName)) {
				existingRecord.bins.put(operation.binName, new HashMap<>());
			}
			binValue = (Map<Object, Object>) existingRecord.bins.get(operation.binName);
			binValue.put(operationValues.get(0), operationValues.get(1));
		}
		return existingRecord;
	}

	private Object onMapRead(Record existingRecord, WritePolicy policy, Key key, Operation operation) {
		final byte command = toCommand(operation);
		switch (command) {
		case MapBaseCopy.GET_BY_KEY:
		case MapBaseCopy.GET_BY_VALUE:
			final List<Entry<?, ?>> searchResult = searchInMap(key, operation, existingRecord, command);
			final MapReturnType mapReturnType = toMapReturnType(operation);
			return toReturnType(mapReturnType, searchResult);
		case MapBaseCopy.SIZE:
			final Map<?, ?> savedMap = existingRecord == null ? null : (Map<?, ?>) existingRecord.bins.get(operation.binName);
			return savedMap != null ? savedMap.size() : null;
		default:
			throw new UnsupportedOperationException("Unsupported map command");
		}
	}

	private List<Entry<?, ?>> searchInMap(Key key, Operation operation, Record existingRecord, byte command) {
		final byte[] operationValueAsBytes = (byte[]) ((BytesValue) operation.value).getObject();
		final List<?> searchObjects = (List<?>) Unpacker.unpackObject(operationValueAsBytes, MAP_DATA_OFFSET,
				operationValueAsBytes.length);
		final Object searchValue = searchObjects.get(1);

		final List<Entry<?, ?>> result = new ArrayList<>(); // result is list of map entries
		if (existingRecord != null && existingRecord.bins.containsKey(operation.binName)) {
			final Map<?, ?> savedMap = (Map<?, ?>) existingRecord.bins.get(operation.binName);

			for (Entry<?, ?> mapEntry : savedMap.entrySet()) {
				switch (command) {//command
				case MapBaseCopy.GET_BY_KEY:
					if (mapEntry.getKey().equals(searchValue)) {
						result.add(mapEntry);
					}
					break;
				case MapBaseCopy.GET_BY_VALUE:
					if (mapEntry.getValue().equals(searchValue)) {
						result.add(mapEntry);
					}
					break;
				default:
					throw new UnsupportedOperationException("Unsupported map command");
				}
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private Map<Key, Record> getData() {
		Map<Key, Record> data = null;
		try {
			Field dataField = MockAerospikeClient.class.getDeclaredField(DATA_FIELD_NAME);
			dataField.setAccessible(true);
			data = (Map<Key, Record>) dataField.get(this); // Get data field from super class
		} catch (Exception e) {
			throw new RuntimeException("Cannot get data field from super class");
		}
		return data;
	}

	private static byte toCommand(Operation operation) {
		final byte[] bytes = (byte[]) ((BytesValue) operation.value).getObject();
		return bytes[1];
	}

	private MapReturnType toMapReturnType(Operation operation) {
		final byte[] bytes = (byte[]) ((BytesValue) operation.value).getObject();
		final Object unpackedObject = Unpacker.unpackObject(bytes, bytes.length > MAP_DATA_OFFSET ? MAP_DATA_OFFSET : 1,
				bytes.length);
		
		final Long returnTypeAsLong; 
		if(unpackedObject instanceof List){
			returnTypeAsLong = (Long) ((List<?>)unpackedObject).get(0);
		}else if(unpackedObject instanceof Number){
			returnTypeAsLong = ((Number)unpackedObject).longValue();
		}else{
			throw new IllegalArgumentException("Illegal return object type");
		}
		
		final MapReturnType returnType;
		switch (returnTypeAsLong.intValue()) {
		case 0:
			returnType = MapReturnType.NONE;
			break;
		case 1:
			returnType = MapReturnType.INDEX;
			break;
		case 2:
			returnType = MapReturnType.REVERSE_INDEX;
			break;
		case 3:
			returnType = MapReturnType.RANK;
			break;
		case 4:
			returnType = MapReturnType.REVERSE_RANK;
			break;
		case 5:
			returnType = MapReturnType.COUNT;
			break;
		case 6:
			returnType = MapReturnType.KEY;
			break;
		case 8:
			returnType = MapReturnType.KEY_VALUE;
			break;
		case 7:
		default:
			returnType = MapReturnType.VALUE;
		}
		return returnType;
	}

	private static Object toReturnType(MapReturnType mapReturnType, List<Entry<?, ?>> searchResult) {
		if (!CollectionUtils.isEmpty(searchResult)) {
			switch (mapReturnType) {
			case KEY:
				return searchResult.stream().map(e -> e.getKey()).collect(Collectors.toList());
			case KEY_VALUE:
				return searchResult;
			case VALUE:
				return searchResult.stream().map(e -> e.getValue()).collect(Collectors.toList()).get(0);
			default:
				throw new UnsupportedOperationException("Unssoprted map return type: " + mapReturnType);
			}
		} else {
			return null;
		}
	}

	static class MapBaseCopy extends MapBase {
		public static final int SET_TYPE = MapBase.SET_TYPE;
		public static final int ADD = MapBase.ADD;
		public static final int ADD_ITEMS = MapBase.ADD_ITEMS;
		public static final int PUT = MapBase.PUT;
		public static final int PUT_ITEMS = MapBase.PUT_ITEMS;
		public static final int REPLACE = MapBase.REPLACE;
		public static final int REPLACE_ITEMS = MapBase.REPLACE_ITEMS;
		public static final int INCREMENT = MapBase.INCREMENT;
		public static final int DECREMENT = MapBase.DECREMENT;
		public static final int CLEAR = MapBase.CLEAR;
		public static final int REMOVE_BY_KEY = MapBase.REMOVE_BY_KEY;
		public static final int REMOVE_BY_INDEX = MapBase.REMOVE_BY_INDEX;
		public static final int REMOVE_BY_RANK = MapBase.REMOVE_BY_RANK;
		public static final int REMOVE_BY_KEY_LIST = MapBase.REMOVE_BY_KEY_LIST;
		public static final int REMOVE_BY_VALUE = MapBase.REMOVE_BY_VALUE;
		public static final int REMOVE_BY_VALUE_LIST = MapBase.REMOVE_BY_VALUE_LIST;
		public static final int REMOVE_BY_KEY_INTERVAL = MapBase.REMOVE_BY_KEY_INTERVAL;
		public static final int REMOVE_BY_INDEX_RANGE = MapBase.REMOVE_BY_INDEX_RANGE;
		public static final int REMOVE_BY_VALUE_INTERVAL = MapBase.REMOVE_BY_VALUE_INTERVAL;
		public static final int REMOVE_BY_RANK_RANGE = MapBase.REMOVE_BY_RANK_RANGE;
		public static final int SIZE = MapBase.SIZE;
		public static final int GET_BY_KEY = MapBase.GET_BY_KEY;
		public static final int GET_BY_INDEX = MapBase.GET_BY_INDEX;
		public static final int GET_BY_RANK = MapBase.GET_BY_RANK;
		public static final int GET_BY_VALUE = MapBase.GET_BY_VALUE;
		public static final int GET_BY_KEY_INTERVAL = MapBase.GET_BY_KEY_INTERVAL;
		public static final int GET_BY_INDEX_RANGE = MapBase.GET_BY_INDEX_RANGE;
		public static final int GET_BY_VALUE_INTERVAL = MapBase.GET_BY_VALUE_INTERVAL;
		public static final int GET_BY_RANK_RANGE = MapBase.GET_BY_RANK_RANGE;
	}

	@Override
	public IndexTask createIndex(Policy policy, String namespace, String setName, String indexName, String binName,
			IndexType indexType, IndexCollectionType indexCollectionType) {
		createdIndexes.add(indexName);
		return new IndexTask();
	}

	@Override
	public IndexTask createIndex(Policy policy, String namespace, String setName, String indexName, String binName,
			IndexType indexType) {
		createdIndexes.add(indexName);
		return new IndexTask();
	}
	
	public List<String> getCreatedIndexes() {
		return createdIndexes;
	}

	@Override
	public RecordSet query(QueryPolicy policy, Statement statement) throws AerospikeException {
		if (policy == null) {
			policy = new QueryPolicy();
		}
		Map<Key, Record> data = getData();
		try {
			RecordSet recordSet = prepareMockRecordSet(policy, statement, data.size());
			final Method putMethod = RecordSet.class.getDeclaredMethod(PUT_METHOD_NAME, KeyRecord.class);
			putMethod.setAccessible(true);
			for (Entry<Key, Record> entry : data.entrySet()) {
				if (statement.getSetName().equals(entry.getKey().setName) 
						&& isInFilters(statement.getFilter(), entry)
						&& isInPredFilters(statement.getPredExp(), entry)) {
					putMethod.invoke(recordSet, new KeyRecord(entry.getKey(), entry.getValue()));
				}			
			}
			putMethod.invoke(recordSet, RecordSet.END);

			return recordSet;
		} catch (Exception e) {
			throw new RuntimeException("Cannot create RecordSet with data entries", e);
		}
	}
	
	private boolean isInPredFilters(PredExp[] predFilters, Entry<Key, Record> entry) {
		return true;
	}

	private boolean isInFilters(Filter filter, Entry<Key, Record> entry) {
		boolean accept = true;
		if (filter != null) {
			String name = getValue(filter, "name");
			IndexCollectionType colType = getValue(filter, "colType");
			Value begin = getValue(filter, "begin");
			Value end = getValue(filter, "end");
			if (!filter(entry.getValue(), name, colType, begin, end)) {
				accept = false;
			}
		}
		return accept;
	}

	private boolean filter(Record record, String name, IndexCollectionType colType, Value begin, Value end) {
		if (! begin.equals(end)) {
			throw new UnsupportedOperationException("Not implemented");
		}
		Object val = record.getValue(name);
		return Objects.equals(Value.get(val), begin);
	}

	@SuppressWarnings("unchecked")
	private <T> T getValue(Object object, String fieldName) {
		Field field;
		try {
			field = object.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return (T) field.get(object);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private RecordSet prepareMockRecordSet(QueryPolicy policy, Statement statement, int size) throws Exception {
		NodeValidator nv = new NodeValidator();
		Field aliasesField = nv.getClass().getDeclaredField("aliases");
		aliasesField.setAccessible(true);
		aliasesField.set(nv, Arrays.asList(new Host("localhost", 1000)));

		Cluster cluster = new Cluster(new ClientPolicy(), new Host[]{new Host("localhost", 1000)});
		
		Constructor<RecordSet> constructor = RecordSet.class.getDeclaredConstructor(QueryExecutor.class, int.class);
		constructor.setAccessible(true);
		RecordSet recordSet = constructor.newInstance(new QueryExecutor(cluster, policy, statement, new Node(cluster, nv)) {
			@Override
			protected MultiCommand createCommand(Node node) {
				return null;
			}

			@Override
			protected void sendCancel() {
			}

			@Override
			protected void sendCompleted() {
			}
		}, size + 1);
		return recordSet;
	}

}
