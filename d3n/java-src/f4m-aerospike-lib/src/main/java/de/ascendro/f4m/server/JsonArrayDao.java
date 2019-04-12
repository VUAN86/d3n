package de.ascendro.f4m.server;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.aerospike.client.policy.WritePolicy;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;

/**
 * Wraps AerospikeDao providing functionality to manipulate with JSON arrays in Aerospike.
 * 
 * It is expected that this class will be extended with specific data type and init call in constructor.
 * 
 * @param <T>
 */
public class JsonArrayDao<T, K, P extends PrimaryKeyUtil<K>> {
	
	private AerospikeDao aerospikeDao;
	private P primaryKeyUtil;
	private JsonUtil jsonUtil;
	
	private String binName;
	private String setName;
	private Type type;

	public JsonArrayDao(AerospikeDao aerospikeDao, P primaryKeyUtil, JsonUtil jsonUtil) {
		this.aerospikeDao = aerospikeDao;
		this.primaryKeyUtil = primaryKeyUtil;
		this.jsonUtil = jsonUtil;
	}
	
	public void init(String setName, String binName, Type type) {
		this.type = type;
		this.setName = setName;
		this.binName = binName;
	}

	public void addEntityToDbJsonArray(K keyId, T entity) {
		aerospikeDao.createOrUpdateJson(getSetName(), primaryKeyUtil.createPrimaryKey(keyId), getBinName(),
				(result, policy) -> update(result, policy, entity));
	}
	
	public void removeEntityFromJsonArray(K keyId, Predicate<T> deleteCondition) {
		aerospikeDao.updateJson(getSetName(), primaryKeyUtil.createPrimaryKey(keyId), getBinName(), 
				(read, write) -> removeFromList(read, write, deleteCondition));
	}
	
	public List<T> getEntityList(K keyId) {
		String entityListAsString = aerospikeDao.readJson(getSetName(), primaryKeyUtil.createPrimaryKey(keyId), getBinName());
		return jsonUtil.getEntityListFromJsonString(entityListAsString, getType());
	}
	
	protected String update(String readResult, WritePolicy writePolicy, T newEntity) {
		List<T> list = jsonUtil.getEntityListFromJsonString(readResult, getType());
		list.add(newEntity);
		return jsonUtil.convertEntitiesToJsonString(list);
	}

	protected String removeFromList(String readResult, WritePolicy writePolicy, Predicate<T> deleteCondition) {
		List<T> existingList = jsonUtil.getEntityListFromJsonString(readResult, getType());
		List<T> toDbList = new ArrayList<>(); //it seems, that existingList cannot be modified, but not sure
		for (T entity : existingList) {
			if (!deleteCondition.test(entity)) {
				toDbList.add(entity);
			}
		}
		return jsonUtil.convertEntitiesToJsonString(existingList);
	}

	protected String getSetName() {
		return setName;
	}

	protected Type getType() {
		if (type == null) {
			throw new F4MFatalErrorException("JsonArrayDao not properly initialized - no type information");
		} else {
			return type;
		}
	}

	protected String getBinName() {
		return binName;
	}
}
