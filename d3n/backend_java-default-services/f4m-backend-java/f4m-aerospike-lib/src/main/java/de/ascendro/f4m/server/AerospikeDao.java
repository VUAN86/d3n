package de.ascendro.f4m.server;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.PredExp;
import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.FilterCriteria;

public interface AerospikeDao {

	boolean isConnected();

	/**
	 * Create new record with default write policy, provided key and string value (e.g. key to other record) for bin
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @param binName
	 *            - Record bin name
	 * @param binValue
	 *            - record value as String
	 * @throws AerospikeException
	 *             - Aerospike Exception with @see ResultCode (e.g. KEY_EXISTS_ERROR in case of already existing record
	 *             with provided key)
	 */
	void createString(String set, String key, String binName, String binValue) throws AerospikeException;

	/**
	 * Create new record with specified write policy, provided key and string value (e.g. key to other record) for bin
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @param binName
	 *            - Record bin name
	 * @param binValue
	 *            - record value as String
	 * @param policy
	 *            - Write policy
	 * @throws AerospikeException
	 *             - Aerospike Exception with @see ResultCode (e.g. KEY_EXISTS_ERROR in case of already existing record
	 *             with provided key)
	 */
	void createString(String set, String key, String binName, String binValue, WritePolicy policy)
			throws AerospikeException;

	/**
	 * Create new record with provided key and json as string for bin
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @param binName
	 *            - Record bin name
	 * @param binValue
	 *            - record value as Json String
	 * @throws AerospikeException
	 *             - Aerospike Exception with @see ResultCode (e.g. KEY_EXISTS_ERROR in case of already existing record
	 *             with provided key)
	 */
	void createJson(String set, String key, String binName, String jsonAsString) throws AerospikeException;


	void createOrUpdateLong(String set, String key, String binName, Long binValueAsLong);

	void createOrUpdateString(String set, String key, String binName, String binValueAsString);

	/**
	 * Delete record by provided key within provided set. Delete succeeds only if records exists within Aerospike
	 * database and provided selected record generation.
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @throws AerospikeException
	 *             - Aerospike Exception with @see ResultCode (e.g. KEY_NOT_FOUND_ERROR in case of missing record)
	 */
	void delete(String set, String key) throws AerospikeException;

	/**
	 * Delete record by provided key within provided set. Delete succeeds anyway if records exists or does not exist
	 * within Aerospike database. Disabled Record Generation check mode.
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @throws AerospikeException
	 */
	void deleteSilently(String set, String key) throws AerospikeException;

	/**
	 * Delete record by provided key within provided set. Delete succeeds anyway if records exists or does not exist
	 * within Aerospike database. Disabled Record Generation check mode.
	 *
	 * @param key
	 *            - Record key
	 * @throws AerospikeException
	 */
	void deleteSilently(Key key) throws AerospikeException;

	/**
	 * Attempt (twice) to update Aerospike bin with string (e.g. key to other record) using read-modify-write pattern.
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @param binName
	 *            - Record bin name
	 * @param updateCall
	 *            - data manipulation between read and write. Executed once or twice as second attempt
	 * @return new state of bin value
	 */
	String updateString(String set, String key, String binName, UpdateCall<String> updateCall)
			throws AerospikeException;

	/**
	 * Creates new or updates existing Aerospike bin with string (e.g. key to other record). See
	 * {@link #updateString(String, String, String, UpdateCall)}
	 * 
	 * @param set
	 * @param key
	 * @param binName
	 * @param updateCall
	 * @return
	 * @throws AerospikeException
	 */
	String createOrUpdateString(String set, String key, String binName, UpdateCall<String> updateCall)
			throws AerospikeException;

	/**
	 * Attempt (twice) to update Aerospike bin with JSON using read-modify-write pattern.
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @param binName
	 *            - Record bin name
	 * @param updateCall
	 *            - data manipulation between read and write. Executed once or twice as second attempt
	 * @return new state of bin value
	 */
	String updateJson(String set, String key, String binName, UpdateCall<String> updateCall) throws AerospikeException;

	/**
	 * Creates new or updates existing Aerospike bin with JSON. See
	 * {@link #updateJson(String, String, String, UpdateCall)}
	 * 
	 * @param set
	 * @param key
	 * @param binName
	 * @param updateCall
	 * @return
	 * @throws AerospikeException
	 */
	String createOrUpdateJson(String set, String key, String binName, UpdateCall<String> updateCall)
			throws AerospikeException;
	
	/**
	 * Update List within bin. Throws KEY_NOT_FOUND_ERROR if record does not exist.
	 * 
	 * @param set
	 *            Aerospike Set
	 * @param key
	 *            Record key
	 * @param binName
	 *            Record bin name
	 * @param updateCall
	 *            List update callback
	 * @return result state of the list
	 */
	<V> List<V> updateList(String set, String key, String binName, UpdateCall<List<V>> updateCall);

	/**
	 * Update List within bin.
	 * 
	 * @param set
	 *            Aerospike Set
	 * @param key
	 *            Record key
	 * @param binName
	 *            Record bin name
	 * @param updateCall
	 *            List update callback
	 * @return result state of the list
	 */
	<V> List<V> createOrUpdateList(String set, String key, String binName, UpdateCall<List<V>> updateCall);

	/**
	 * Read String(e.g. key to other record) from specified record bin
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @param binName
	 *            - Record bin name
	 * @return read value or null if not found
	 * @throws AerospikeException
	 */
	String readString(String set, String key, String binName) throws AerospikeException;

	/**
	 * Read Long(Aerospike Integer resolve to Long in Java) from specified record bin
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @param binName
	 *            - Record bin name
	 * @return read value or null if not found
	 * @throws AerospikeException
	 */
	Long readLong(String set, String key, String binName) throws AerospikeException;

	/**
	 * Read multiple {@link Long}s for specified keys in one batch call. The
	 * returned {@link Long}s are in positional order with the original key
	 * array order. If a key is not found, the positional {@link Long} will be
	 * null.
	 * 
	 * @param set
	 * @param keys
	 * @param binName
	 * @return
	 * @throws AerospikeException
	 */
	Long[] readLongs(String set, String[] keys, String binName) throws AerospikeException;

	/**
	 * Read Integer from specified record bin
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @param binName
	 *            - Record bin name
	 * @return read value or null if not found
	 * @throws AerospikeException
	 */
	Integer readInteger(String set, String key, String binName) throws AerospikeException;

	/**
	 * Read Json as String from specified record bin
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @param binName
	 *            - Record bin name
	 * @return JSON as String or null if not found
	 * @throws AerospikeException
	 */
	String readJson(String set, String key, String binName) throws AerospikeException;

	/**
	 * Read multiple JSONs as Strings for specified keys in one batch call. The
	 * returned JSONs are in positional order with the original key array order.
	 * If a key is not found, the positional JSON will be null.
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param keys
	 *            - Array of primary keys
	 * @param binName
	 *            - Record bin name
	 * @return Array of JSONs as Strings or empty array
	 * @throws AerospikeException
	 */
	String[] readJsons(String set, String[] keys, String binName) throws AerospikeException;

	/**
	 * Read List from specified record bin
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @param binName
	 *            - Record bin name
	 * @return List of unknown type elements
	 * @throws AerospikeException
	 */
	<T> List<T> readList(String set, String key, String binName)  throws AerospikeException;
	
	/**
	 * Read record generation from the aerospike
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @return generation number or null if record not found
	 * @throws AerospikeException
	 */
	Integer getRecordGeneration(String set, String key) throws AerospikeException;

	/**
	 * Query data into map (Not used yet)
	 * 
	 * @return
	 */
	List<Map<String, String>> queryIntoMap(String set, String packageName, String functionName, String[] values);

	/**
	 * Check if particular key is present in Aerospike. Caution: this operation should not be used for deciding between
	 * update or create usage, as exists is return state at its execution time
	 * 
	 * @param set
	 *           - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @return true - if does exist, false - otherwise
	 */
	boolean exists(String set, String key);
	
	/**
	 * Add long to bin
	 * @param set
	 *           - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @param binName
	 *            - Record bin name
	 */
	void add(String set, String key, String binName, long value);
	
	<T> List<T> getJsonObjects(String set, String binName, List<String> ids,
			Function<String, String> primaryKeyFunc, Function<String, T> stringToJsonFunc,
			boolean skipNulls);
	
	/**
	 * Filter list by {@link FilterCriteria}
	 * @param objects
	 * @param filter
	 * @return List of filtered {@link JsonObject}s
	 */
	List<JsonObject> filterJsonObjects(List<JsonObject> objects, FilterCriteria filter);
	
	/**
	 * Scan records using predicate filtering
	 * @param set - name of the set to scan
	 * @param binNames - list of the bins to query
	 * @param predExp - valid predicate filtering expression
	 * @param entryCallback - record consumption callback
	 */
	void scanUsingPredicateFilter(String set, String[] binNames, PredExp[] predExp,
			Consumer<Record> entryCallback);
}
