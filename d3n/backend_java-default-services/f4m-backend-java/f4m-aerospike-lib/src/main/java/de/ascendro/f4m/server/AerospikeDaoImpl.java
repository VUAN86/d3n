package de.ascendro.f4m.server;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import de.ascendro.f4m.service.di.GsonProvider;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Info;
import com.aerospike.client.Key;
import com.aerospike.client.Language;
import com.aerospike.client.Record;
import com.aerospike.client.ResultCode;
import com.aerospike.client.Value;
import com.aerospike.client.Value.NullValue;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.command.ParticleType;
import com.aerospike.client.lua.LuaConfig;
import com.aerospike.client.policy.CommitLevel;
import com.aerospike.client.policy.GenerationPolicy;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.PredExp;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.ResultSet;
import com.aerospike.client.query.Statement;
import com.aerospike.client.task.RegisterTask;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.json.model.FilterCriteria;

/**
 * Data access object for Aerospike Database. Provides main methods to write, read and delete data.
 */
public class AerospikeDaoImpl<P extends PrimaryKeyUtil<?>> implements AerospikeDao {

	public static final String DEFAULT_CHARSET_AEROSPIKE = "UTF-8";
	private static final Logger LOGGER = LoggerFactory.getLogger(AerospikeDaoImpl.class);
	private static final String VALUE_FOR_UPDATECALL_ON_CREATE = null;
	protected static final int GENERATION_ERROR_MAX_RETRIES = 20;

	protected final Config config;
	protected final P primaryKeyUtil;
	protected final JsonUtil jsonUtil;

	protected Boolean durableDeleteSupported = null;
	private final AerospikeClientProvider aerospikeClientProvider;

	@Inject
	private GsonProvider gsonProvider;

	@Inject
	public AerospikeDaoImpl(Config config, P primaryKeyUtil, JsonUtil jsonUtil,
			AerospikeClientProvider aerospikeClientProvider) {
		this.config = config;
		this.primaryKeyUtil = primaryKeyUtil;
		this.jsonUtil = jsonUtil;
		this.aerospikeClientProvider = aerospikeClientProvider;
	}
	
	@PreDestroy
	public void destroyClient() {
		if(getAerospikeClient().isConnected()){
			LOGGER.debug("Closing Aerospike client from {}", getClass().getSimpleName());
			getAerospikeClient().close();
		}
	}
	
	protected IAerospikeClient getAerospikeClient(){
		return aerospikeClientProvider.get();
	}
	
	@Override
	public boolean exists(String set, String key){
		final Key k = new Key(getNamespace(), set, key);
		return getAerospikeClient().exists(null, k);
	}

	@Override
	public void createString(String set, String key, String binName, String binValue) {
		createString(set, key, binName, binValue, getCreatePolicy());
	}
	
	@Override
	public void createString(String set, String key, String binName, String binValue, WritePolicy policy) {
		writeBin(policy, set, key, new Bin(binName, binValue));
	}

	@Override
	public void createJson(String set, String key, String binName, String binValueAsString) {
		createRecord(set, key, getJsonBin(binName, binValueAsString));
	}

	@Override
	public void createOrUpdateLong(String set, String key, String binName, Long binValueAsLong) {
		createOrUpdateRecord(set, key, getLongBin(binName,binValueAsLong));
	}

    @Override
    public void createOrUpdateString(String set, String key, String binName, String binValueAsString) {
        createOrUpdateRecord(set, key, getStringBin(binName, binValueAsString));
    }

	protected Bin getJsonBin(String name, String value) {
		final byte[] binValue = value == null ? null : value.getBytes(getDefaultCharset());
		return new Bin(name, binValue);
	}
	
	protected Bin getLongBin(String name, long value) {
		return new Bin(name, value);
	}
	
	protected Bin getStringBin(String name, String value) {
		return new Bin(name, value);
	}
	
	/**
	 * Deletes whole record by key
	 */
	@Override
	public void delete(String set, String key) {
		LOGGER.debug("Deleting record from set '{}', key '{}'", set, key);

		final Key k = new Key(getNamespace(), set, key);

		final Record record = readRecord(set, key);
		if (record != null) {
			final WritePolicy deletePolicy = getWritePolicy(record.generation);
			deletePolicy.durableDelete = isDurableDeleteSupported();
			getAerospikeClient().delete(deletePolicy, k);
		} else {
			throw new F4MEntryNotFoundException();
		}
	}	
	
	/**
	 * Try to determine if durable delete is supported based on Aeropsike server nodes' version
	 * @return true - first node client has successfully communicated to version is Enterprise
	 */
	protected boolean isDurableDeleteSupported(){
		if (durableDeleteSupported == null) {
			AerospikeException lastAerospikeException = null;
			for (Node node : aerospikeClientProvider.get().getNodes()) {
				try {
					durableDeleteSupported = isEnterprise(node);
					break;
				} catch (AerospikeException aEx) {	
					lastAerospikeException = aEx;
					LOGGER.error("Failed to request Aeropsike server version from node [{}] ", node.getName(), aEx);
				}
			}
			//check if all nodes failed to communicate
			if (durableDeleteSupported == null && lastAerospikeException != null) {
				throw lastAerospikeException;
			}
		}
		return firstNonNull(durableDeleteSupported, false);
	}

	@Override
	public void deleteSilently(String set, String key) {
		final Key k = new Key(getNamespace(), set, key);
		deleteSilently(k);
	}

	@Override
	public void deleteSilently(Key k) {
		final WritePolicy deletePolicy = getWritePolicy();
		deletePolicy.generationPolicy = GenerationPolicy.NONE;
		deletePolicy.durableDelete = isDurableDeleteSupported();

		getAerospikeClient().delete(deletePolicy, k);
	}

	/**
	 * Reads single bin value by key and bin value
	 * 
	 * @return bin value
	 */
	@Override
	public String readString(String set, String key, String binName) {
		LOGGER.debug("Reading single bin as String of a record from set '{}', key '{}', bin '{}'", set, key, binName);
		return read(set, key, binName);
	}
	
	@Override
	public Long readLong(String set, String key, String binName) {
		LOGGER.debug("Reading single bin as Long of a record from set '{}', key '{}', bin '{}'", set, key, binName);
		return read(set, key, binName);
	}

	@Override
	public Long[] readLongs(String set, String[] keys, String binName) {
		LOGGER.debug("Reading multiple Long's in a single batch call from set '{}', key '{}', bin '{}'", set, keys, binName);
		Record[] records = readRecords(set, keys, binName);
		return Arrays.stream(records)
			.map(r -> r == null ? null : r.getLong(binName))
			.toArray(Long[]::new);
	}

	@Override
	public Integer readInteger(String set, String key, String binName) {
		LOGGER.debug("Reading single bin as Integer of a record from set '{}', key '{}', bin '{}'", set, key, binName);
		final Long longValue = this.<Long>read(set, key, binName);
		return longValue != null ? longValue.intValue() : null;
	}
	
	@SuppressWarnings("unchecked")
	protected <T> T read(String set, String key, String binName) {
		Key k = new Key(getNamespace(), set, key);
		Record r = getAerospikeClient().get(null, k);

		return r != null ? (T) r.getValue(binName) : null;
	}

	@Override
	public String readJson(String set, String key, String binName) {
		LOGGER.debug("Reading single bin as byte[] of a record from set '{}', key '{}', bin '{}'", set, key, binName);

		final Record r = readRecord(set, key);
		return readJson(binName, r);
	}

	@Override
	public String[] readJsons(String set, String[] keys, String binName) {
		LOGGER.debug("Reading multiple records in a single batch call from set '{}', key '{}', bin '{}'", set, keys, binName);
		Record[] records = readRecords(set, keys, binName);
		return Arrays.stream(records)
				.map(r -> readJson(binName, r))
				.toArray(String[]::new);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> readList(String set, String key, String binName) {
		LOGGER.debug("Reading single bin as List of a record from set '{}', key '{}', bin '{}'", set, key, binName);
		final Record r = readRecord(set, key);
		return r != null ? (List<T>)  r.getList(binName) : null;
	}

	protected String readJson(String binName, final Record r) {
		Object object = r.bins.get(binName);
		final byte[] jsonBytes = (byte[]) r.bins.get(binName);

		return jsonBytes != null ? new String(jsonBytes, getDefaultCharset()) : null;
	}
	
	protected Record[] readRecords(String set, String[] keys, String binName) {
		Key[] k = Arrays.stream(keys)
				.map(key -> new Key(getNamespace(), set, key))
				.toArray(Key[]::new);
		return getAerospikeClient().get(null, k, binName);
	}

	protected RecordSet readByFilters(String set, String[] binNames, PredExp[] predExp) {
		final Statement statement = new Statement();
		statement.setNamespace(getNamespace());
		statement.setSetName(set);
		if (ArrayUtils.isNotEmpty(binNames)) {
			statement.setBinNames(binNames);
		}
		statement.setPredExp(predExp);
		final RecordSet resultSet = getAerospikeClient().query(null, statement);
		return resultSet;
	}

	@Override
	public String updateString(String set, String key, String binName, UpdateCall<String> updateCall) {
		final Transformator<String> transformator = new SameValueTransformator<>();
		return (String) update(set, key, binName, updateCall, transformator).getObject();
	}

	@Override
	public String updateJson(String set, String key, String binName, UpdateCall<String> updateCall) {
		LOGGER.debug("updateJson set {} key {} bin {}  ", set, key, binName);
		final StringTransformatorToByteArrayValue transformator = new StringTransformatorToByteArrayValue(
				getDefaultCharset());
		return (String) update(set, key, binName, updateCall, transformator).getObject();
	}

	@Override
	public String createOrUpdateString(String set, String key, String binName, UpdateCall<String> updateCall) {
		final Transformator<String> transformator = new SameValueTransformator<>();
		return (String) createOrUpdate(set, key, binName, updateCall, transformator).getObject();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <V> List<V> updateList(String set, String key, String binName, UpdateCall<List<V>> updateCall) {
		final Value value = update(set, key, binName, updateCall, new SameValueTransformator<>());
		return (List<V>) value.getObject();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V> List<V> createOrUpdateList(String set, String key, String binName, UpdateCall<List<V>> updateCall) {
		final Value value = createOrUpdate(set, key, binName, updateCall, new SameValueTransformator<>());
		return (List<V>) value.getObject();
	}

	@Override
	public String createOrUpdateJson(String set, String key, String binName, UpdateCall<String> updateCall) {
		final StringTransformatorToByteArrayValue byteTransformator = new StringTransformatorToByteArrayValue(
				getDefaultCharset());
		return (String) createOrUpdate(set, key, binName, updateCall, byteTransformator).getObject();
	}

	@Override
	public Integer getRecordGeneration(String set, String key) {
		final Record record = readRecord(set, key);

		final Integer generation;
		if (record != null) {
			generation = record.generation;
		} else {
			generation = null;
		}
		return generation;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Map<String, String>> queryIntoMap(String set, String packageName, String functionName, String[] values) {
		LOGGER.debug("Querying records from set '{}', packageName '{}', functionName '{}', values '{}'", set,
				packageName, functionName, values);
		List<Map<String, String>> data = new ArrayList<>();

		Statement stmt = new Statement();
		stmt.setNamespace(getNamespace());
		stmt.setSetName(set);

		try (ResultSet rs = getAerospikeClient().queryAggregate(null, stmt, packageName, functionName, getValueArray(values))) {
			while (rs.next()) {
				LOGGER.trace("Record: {}", rs.getObject());
				data.add((Map<String, String>) rs.getObject());
			}
		}

		return data;
	}

	@Override
	public boolean isConnected() {
		LOGGER.debug("Checking Aerospike client connection");
		return getAerospikeClient().isConnected();
	}

	/* Run AerospikeDaoClientTest.testConcurrentAdd manually with real Aerospike
	 * if you make changes to this
	 */
	@Override
	public void add(String set, String key, String binName, long value) {
		Boolean generationError;
		Integer attemptCount = 0;
		do {
			generationError = false;

			Record originalRecord = readRecord(set, key);

			WritePolicy writePolicy = getWritePolicy(originalRecord != null ? originalRecord.generation : 0);
			writePolicy.recordExistsAction = RecordExistsAction.UPDATE;

			try {
				getAerospikeClient().add(writePolicy, new Key(getNamespace(), set, key), getLongBin(binName, value));
			} catch (AerospikeException aEx) {
				if (aEx.getResultCode() == ResultCode.GENERATION_ERROR && attemptCount < GENERATION_ERROR_MAX_RETRIES) {
					LOGGER.debug("Attempt nb. {} failed with generation error for updating set[{}], key[{}], bin[{}]: {}",
							attemptCount, set, key, binName, aEx.getMessage());
					attemptCount++;
					generationError = true;
				} else {
					throw aEx;
				}
			}
		} while (generationError);
	}

	protected <S, V> Value update(String set, String key, String binName, UpdateCall<V> updateCall,
			Transformator<S> transformator) {
		final UpdateCall<Value> valueUpdateCall = new GenericUpdateCall<>(updateCall);
		if (exists(set, key)) {
			return this.updateRecord(set, key, binName, valueUpdateCall, transformator);
		} else {
			throw new F4MEntryNotFoundException();
		}
	}

	protected <S, V> Value createOrUpdate(String set, String key, String binName, UpdateCall<V> updateCall,
			Transformator<S> stringTransformator) {
		final UpdateCall<Value> valueUpdateCall = new GenericUpdateCall<>(updateCall);
		if (exists(set,key)) {
			return updateRecord(set, key, binName, valueUpdateCall, stringTransformator);
		} else {
			final WritePolicy createPolicy = getCreatePolicy();
			final Value binValue = stringTransformator.toRealValue(VALUE_FOR_UPDATECALL_ON_CREATE);
			Value binFinalValue = valueUpdateCall.update(binValue, createPolicy);
			try {
				writeBin(createPolicy, set, key, new Bin(binName, stringTransformator.toStoreValue(binFinalValue)));
				return binFinalValue;
			} catch (AerospikeException e) {
				if (ResultCode.KEY_EXISTS_ERROR == e.getResultCode()) {
					return updateRecord(set, key, binName, valueUpdateCall, stringTransformator);
				} else {
					throw e;
				}
			}
		}
	}

	/* Run AerospikeDaoClientTest.testConcurrentWriteToListRecord manually with real Aerospike
	 * if you make changes to this
	 */
	protected <S> Value updateRecord(String set, String key, String binName, UpdateCall<Value> updateCall,
			Transformator<S> transformator) {
		Boolean generationError;
		Integer attemptCount = 0;
		Value binFinalValue;
		do {
			generationError = false;
			
			Record record = readRecord(set, key);
			WritePolicy wp = getWritePolicy(record.generation);

			Record finalRecord = record;
			ArrayList<Bin> binsAsList = record.bins.entrySet().stream()
					.filter(e -> !e.getKey().equals(binName))
					.map(e -> new Bin(e.getKey(), e.getValue()))
					.collect(Collectors.toCollection(() -> new ArrayList<Bin>(finalRecord.bins.size())));
			Bin[] bins = binsAsList.toArray(new Bin[binsAsList.size() + 1]);
			int lastElemIdx = bins.length - 1;
			final Value realBinValue = transformator.toRealValue(record.getValue(binName));
			binFinalValue = updateCall.update(realBinValue, wp);
			try {
				bins[lastElemIdx] = new Bin(binName, transformator.toStoreValue(binFinalValue));
				writeBin(wp, set, key, bins);
			} catch (AerospikeException aEx) {
				if (aEx.getResultCode() == ResultCode.GENERATION_ERROR && attemptCount < GENERATION_ERROR_MAX_RETRIES) {
					LOGGER.debug("Attempt nb. {} failed with generation error for updating set[{}], key[{}], bin[{}]: {}",
							attemptCount, set, key, binName, aEx.getMessage());
					attemptCount++;
					generationError = true;
				} else {
					throw aEx;
				}
			}
		} while (generationError);
		return binFinalValue;
	}

	protected void createRecord(String set, String key, Bin... bin) {
		final WritePolicy createPolicy = getCreatePolicy();
		writeBin(createPolicy, set, key, bin);
	}

	protected void createOrUpdateRecord(String set, String key, Bin... bin) {
		final WritePolicy createPolicy = new WritePolicy();
		writeBin(createPolicy, set, key, bin);
	}

	protected void registerUdf(File file) {
		LOGGER.debug("Registering UDF file: {}", file.getPath());
		LuaConfig.SourceDirectory = config.getProperty(AerospikeConfigImpl.AEROSPIKE_UDF_LOCATION);
		RegisterTask task = getAerospikeClient().register(null, file.getPath(), file.getName(), Language.LUA);
		task.waitTillComplete();
	}

	private Value[] getValueArray(String... values) {
		int length = values.length;
		Value[] array = new Value[length];
		for (int i = 0; i < length; i++) {
			array[i] = Value.get(values[i]);
		}
		return array;
	}

	protected String getNamespace() {
		return config.getProperty(AerospikeConfigImpl.AEROSPIKE_NAMESPACE);
	}

	protected WritePolicy getWritePolicy() {
		final WritePolicy defaultWritePolicy = getAerospikeClient().getWritePolicyDefault();

		final WritePolicy writePolicy;
		if (defaultWritePolicy != null) {
			writePolicy = new WritePolicy(defaultWritePolicy);
		} else {
			writePolicy = new WritePolicy();
		}

		writePolicy.expiration = config.getPropertyAsInteger(AerospikeConfigImpl.AEROSPIKE_POLICY_WRITE_EXPIRATION);
		writePolicy.generationPolicy = GenerationPolicy.valueOf(config
				.getProperty(AerospikeConfigImpl.AEROSPIKE_POLICY_WRITE_GENERATION));
		writePolicy.recordExistsAction = RecordExistsAction.valueOf(config
				.getProperty(AerospikeConfigImpl.AEROSPIKE_POLICY_WRITE_RECORD_EXISTS_ACTION));
		writePolicy.commitLevel = CommitLevel.valueOf(config
				.getProperty(AerospikeConfigImpl.AEROSPIKE_POLICY_WRITE_COMMIT_LEVEL));

		return writePolicy;
	}

	protected WritePolicy getCreatePolicy() {
		final WritePolicy createPolicy = getWritePolicy();
		createPolicy.recordExistsAction = RecordExistsAction.CREATE_ONLY;
		return createPolicy;
	}

	protected WritePolicy getWritePolicy(int generation) {
		final WritePolicy wp = getWritePolicy();
		wp.generation = generation;
		return wp;
	}

	protected WritePolicy getUpdateSilentPolicy() {
		final WritePolicy wp = getWritePolicy();
		wp.generationPolicy = GenerationPolicy.NONE;
		wp.recordExistsAction = RecordExistsAction.UPDATE;
		return wp;
	}

	protected Charset getDefaultCharset() {
		final String defaultCharset = config.getProperty(AerospikeConfigImpl.AEROSPIKE_WRITE_BYTES_CHARSET);
		return Charset.forName(defaultCharset != null ? defaultCharset : DEFAULT_CHARSET_AEROSPIKE);
	}

	protected Record readRecord(String set, String key) {
		final Key k = new Key(getNamespace(), set, key);
		return getAerospikeClient().get(null, k);
	}

	protected void writeBin(WritePolicy writePolicy, String set, String key, Bin... bins) {
		LOGGER.debug("Writing single bin set '{}', key '{}' bins '{}'", set, key, bins);
		final Key k = new Key(getNamespace(), set, key);

		getAerospikeClient().put(writePolicy, k, bins);
	}

	@Override
	public <T> List<T> getJsonObjects(String set, String binName, List<String> ids,
			Function<String, String> primaryKeyFunc, Function<String, T> stringToJsonFunc,
			boolean skipNulls) {
		String[] keys = ids.stream()
				.map(primaryKeyFunc::apply)
				.toArray(size -> new String[size]);
		String[] jsonsAsString = readJsons(set, keys, binName);
		return Stream.of(jsonsAsString)
				.filter(StringUtils::isNotBlank)
				.map(stringToJsonFunc::apply)
				.collect(Collectors.toList());
	}
	
	/**
	 * Get version name of Aerospike server node
	 * @param node - node to check version of
	 * @return
	 */
	protected String getVersion(Node node) {
		return requestInfo(node, "version");
	}
	
	protected String requestInfo(Node node, String name) {
		return Info.request(node, name);
	}
	
	/**
	 * Checks if Aerospike version connected to is Enterprise
	 * @param node - node to check if its version is enterprise
	 * @return true - for Enterprise
	 */
	public boolean isEnterprise(Node node) {
		final String version = getVersion(node);

		final boolean enterprise;
		if (StringUtils.isNotBlank(version)) {
			enterprise = version.contains("Enterprise");
		} else {
			enterprise = false;
		}
		return enterprise;
	}
	
	@Override
	public List<JsonObject> filterJsonObjects(List<JsonObject> objects, FilterCriteria filter) {
		List<JsonObject> filteredObjects = JsonUtil.filter(objects, filter.getSearchBy());
		
		JsonUtil.sort(filteredObjects, filter.getOrderBy());
		
		return filteredObjects.stream()
				.skip(filter.getOffset() < 0 ? 0 : filter.getOffset())
				.limit(filter.getLimit() < 0 ? 0 : filter.getLimit())
				.collect(Collectors.toList());
	}
	
	@Override
	public void scanUsingPredicateFilter(String set, String[] binNames, PredExp[] predExp,
			Consumer<Record> entryCallback) {
		final Statement statement = new Statement();
		statement.setNamespace(getNamespace());
		statement.setSetName(set);
		if (ArrayUtils.isNotEmpty(binNames)) {
			statement.setBinNames(binNames);
		}
		statement.setPredExp(predExp);
		try (final RecordSet resultSet = getAerospikeClient().query(null, statement)) {
			while (resultSet.next()) {
				entryCallback.accept(resultSet.getRecord());
			}
		}
	}

	public static interface Transformator<S> {
		@SuppressWarnings("unchecked")
		default S toStoreValue(Value realValue) {
			final S storeValue;
			if (realValue != null) {
				switch (realValue.getType()) {
				case ParticleType.NULL:
					storeValue = null;
					break;
				default:
					storeValue = (S) realValue.getObject();
				}
			} else {
				storeValue = null;
			}
			return storeValue;
		}

		default Value toRealValue(Object storeValue) {
			return Value.get(storeValue);
		}
	}

	static class StringTransformatorToByteArrayValue implements Transformator<byte[]> {
		private final Charset defaultBytesChartset;

		public StringTransformatorToByteArrayValue(Charset defaultBytesChartset) {
			this.defaultBytesChartset = defaultBytesChartset;
		}

		@Override
		public byte[] toStoreValue(Value realValue) {
			final byte[] storeValueAsByteArray;
			if (realValue != null) {
				switch (realValue.getType()) {
				case ParticleType.NULL:
					storeValueAsByteArray = null;
					break;
				case ParticleType.STRING:
					storeValueAsByteArray = realValue.toString().getBytes(defaultBytesChartset);
					break;
				default:
					final byte[] bytes = new byte[realValue.estimateSize()];
					realValue.write(bytes, 0);

					storeValueAsByteArray = bytes;
					break;
				}
			} else {
				storeValueAsByteArray = null;
			}
			return storeValueAsByteArray;
		}

		@Override
		public Value toRealValue(Object storeValue) {
			final Value value = Value.get(storeValue);

			final Value resultValue;
			if (value == null || value instanceof NullValue) {
				resultValue = new NullValue();
			} else {
				final byte[] byteValue = new byte[value.estimateSize()];
				value.write(byteValue, 0);

				final String realValueAsString = new String(byteValue, defaultBytesChartset);
				resultValue = Value.get(realValueAsString);
			}
			return resultValue;
		}
	}


	public static class SameValueTransformator<S> implements Transformator<S> {
	}

	static class GenericUpdateCall<S> implements UpdateCall<Value> {
		private final UpdateCall<S> stringUpdateCall;

		public GenericUpdateCall(UpdateCall<S> stringUpdateCall) {
			this.stringUpdateCall = stringUpdateCall;
		}

		@Override
		public Value update(Value readResult, WritePolicy writePolicy) {
			@SuppressWarnings("unchecked")
			final S result = stringUpdateCall.update((S) readResult.getObject(), writePolicy);
			return Value.get(result);
		}
	}

	protected <T> List<T> removeExtraItems(List<T> items, long offset, int limit) {
		return items.stream().skip(offset < 0 ? 0 : offset).limit(limit < 0 ? 0 : limit).collect(Collectors.toList());
	}
}
