package de.ascendro.f4m.server;

import java.util.List;
import java.util.Map;

import com.aerospike.client.Operation;
import com.aerospike.client.Record;

import de.ascendro.f4m.server.AerospikeOperateDaoImpl.OperateCall;

public interface AerospikeOperateDao extends AerospikeDao {

	/**
	 * Update Map within bin. Throws KEY_NOT_FOUND_ERROR if record does not exist.
	 * 
	 * @param set
	 *            Aerospike Set
	 * @param key
	 *            Record key
	 * @param binName
	 *            Record bin name
	 * @param updateCall
	 *            Map update callback
	 * @return result state of the map
	 */
	<K, V> Map<K, V> updateMap(String set, String key, String binName, UpdateCall<Map<K, V>> updateCall);

	/**
	 * Update or create Map within bin.
	 * 
	 * @param set
	 *            Aerospike Set
	 * @param key
	 *            Record key
	 * @param binName
	 *            Record bin name
	 * @param updateCall
	 *            Map update callback
	 * @return result state of the map
	 */
	<K, V> Map<K, V> createOrUpdateMap(String set, String key, String binName, UpdateCall<Map<K, V>> updateCall);

	/**
	 * Update map value by key
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @param binName
	 *            - Record bin name
	 * @param mapKey
	 *            - Map record key
	 * @param updateCallback
	 *            - Map value update callback
	 */
	<K, V> V createOrUpdateMapValueByKey(String set, String key, String binName, K mapKey, UpdateCall<V> updateCallback);

	/**
	 * Operate read and write operations (Read-Write-Read) on single record
	 * CAUTION: Not tested with all possible operate combinations, tested within simple map operations (put+put, removeByKey+put)
	 * @param set - Aerospike set
	 * @param key - Single record key
	 * @param readOperations - Read operations to be performed
	 * @param operateCall - call for write operations to be performed
	 * @return record which consists of all read results (e.g., binName -> List<Object> (depends on performed operations))
	 */
	Record operate(String set, String key, Operation[] readOperations,
			OperateCall<Record, List<Operation>> operateCall);
	/**
	 * Get map records by key
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @param binName
	 *            - Record bin name
	 * @param mapKey
	 *            - Map record key
	 * @return Return found map records
	 */
	<K, V> V getByKeyFromMap(String set, String key, String binName, K mapKey);

	/**
	 * Get all map records
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @param binNames
	 *            - Record bin names to retrieve. All bins will be merged in single map and keys overwritten, if exists in multiple bins.
	 * @return Return found map records
	 */
	<K, V> Map<K, V> getAllMap(String set, String key, String... binNames);

	/**
	 * Remove record from map. Throws KEY_NOT_FOUND_ERROR if record does not exist.
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @param binName
	 *            - Record bin name
	 * @param mapKey
	 *            - Map record key
	 */
	void deleteByKeyFromMap(String set, String key, String binName, Object mapKey);
	
	/**
	 * Remove record from map.
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @param binName
	 *            - Record bin name
	 * @param mapKey
	 *            - Map record key
	 */
	void deleteByKeyFromMapSilently(String set, String key, String binName, Object mapKey);
	
	/**
	 * Checks Map element count within provided bin.
	 * 
	 * @param set
	 *            - Aerospike Set
	 * @param key
	 *            - Record unique key
	 * @param binName
	 *            - Record bin name
	 * @return map element count or null if record or map does not exist
	 */
	Integer getMapSize(String set, String key, String binName);
}
