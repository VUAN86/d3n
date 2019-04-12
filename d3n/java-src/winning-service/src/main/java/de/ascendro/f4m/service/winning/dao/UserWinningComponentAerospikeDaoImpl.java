package de.ascendro.f4m.service.winning.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.client.F4MEntryAlreadyExistsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.json.model.OrderBy;
import de.ascendro.f4m.service.winning.config.WinningConfig;
import de.ascendro.f4m.service.winning.exception.F4MComponentAlreadyUsedException;
import de.ascendro.f4m.service.winning.model.UserWinningComponent;
import de.ascendro.f4m.service.winning.model.UserWinningComponentStatus;
import de.ascendro.f4m.service.winning.model.WinningOption;

public class UserWinningComponentAerospikeDaoImpl extends AerospikeOperateDaoImpl<UserWinningComponentPrimaryKeyUtil> implements UserWinningComponentAerospikeDao {

	private final UserWinningComponentPrimaryKeyUtil userWinningComponentPrimaryKeyUtil;

	@Inject
	public UserWinningComponentAerospikeDaoImpl(Config config,
			UserWinningComponentPrimaryKeyUtil userWinningComponentPrimaryKeyUtil,
			AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil) {
		super(config, userWinningComponentPrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
		this.userWinningComponentPrimaryKeyUtil = userWinningComponentPrimaryKeyUtil;
	}

	@Override
	public void saveUserWinningComponent(String appId, String userId, UserWinningComponent component)
			throws F4MEntryAlreadyExistsException {
		String key = userWinningComponentPrimaryKeyUtil.createUserWinningComponentPrimaryKey(component.getUserWinningComponentId());
		String mapKey = userWinningComponentPrimaryKeyUtil.createUserWinningComponentMapPrimaryKey(appId, userId);		
		if (isKeyInMap(mapKey, key)) {
			throw new F4MEntryAlreadyExistsException("User winning component already exists");
		}
		createJson(getSet(), key, BLOB_BIN_NAME, jsonUtil.toJson(component));
		
		createOrUpdateMapValueByKey(getMapSet(), mapKey, COMPONENTS_BIN_NAME, key, (existingValue, policy) -> {
			if (existingValue == null) {
				return key;
			} else {
				throw new F4MEntryAlreadyExistsException("User winning component already exists");
			}
		});
	}

	@Override
	public UserWinningComponent getUserWinningComponent(String appId, String userId, String userWinningComponentId) {
		final String key = userWinningComponentPrimaryKeyUtil.createUserWinningComponentPrimaryKey(userWinningComponentId);
		if (!isKeyInMap(appId, userId, key)) {
			throw new F4MEntryNotFoundException("User winning component does not exists");
		}
		final String userWinningComponentJson = readJson(getSet(), key, BLOB_BIN_NAME);
		return StringUtils.isNotBlank(userWinningComponentJson)
				? jsonUtil.fromJson(userWinningComponentJson, UserWinningComponent.class) : null;
	}
	
	@Override
	public List<JsonObject> getUserWinningComponents(String appId, String userId, int limit, long offset, List<OrderBy> orderBy) {
		String mapKey = userWinningComponentPrimaryKeyUtil.createUserWinningComponentMapPrimaryKey(appId, userId);
		Map<String, String> componentsMap = getAllMap(getMapSet(), mapKey, COMPONENTS_BIN_NAME);
		Set<String> pkSet = componentsMap.keySet();
		String[] records = readJsons(getSet(), pkSet.toArray(new String[pkSet.size()]), BLOB_BIN_NAME);
		
		List<JsonObject> newComponents = Stream.of(records)
				.filter(c -> isAvailableForUse(c))
				.map(c -> jsonUtil.fromJson((String) c, UserWinningComponent.class).getJsonObject())
				.collect(Collectors.toList());
		
		JsonUtil.sort(newComponents, orderBy);
		
		return newComponents.stream()
				.skip(offset < 0 ? 0 : offset)
				.limit(limit < 0 ? 0 : limit)
				.collect(Collectors.toList());
	}

	@Override
	public void markUserWinningComponentUsed(String appId, String userId, String userWinningComponentId) {
		String key = userWinningComponentPrimaryKeyUtil.createUserWinningComponentPrimaryKey(userWinningComponentId);
		if (!isKeyInMap(appId, userId, key)) {
			throw new F4MEntryNotFoundException("User winning component does not exists");
		}
		createOrUpdateJson(getSet(), key, BLOB_BIN_NAME, (existingValue, policy) -> {
			if (existingValue != null) {
				UserWinningComponent component = jsonUtil.fromJson((String) existingValue, UserWinningComponent.class);
				if (component.getStatus() != UserWinningComponentStatus.NEW) {
					// winning component already used
					throw new F4MComponentAlreadyUsedException("Component with ID " + userId + ", " + userWinningComponentId + " already used");					
				}
				component.setStatus(UserWinningComponentStatus.USED);
				return jsonUtil.toJson(component);
			} else {
				throw new F4MEntryNotFoundException("Winning component not found");
			}
		});
	}

	@Override
	public void saveUserWinningComponentWinningOption(String appId, String userId, String userWinningComponentId, WinningOption winning) {
		String key = userWinningComponentPrimaryKeyUtil.createUserWinningComponentPrimaryKey(userWinningComponentId);
		if (!isKeyInMap(appId, userId, key)) {
			throw new F4MEntryNotFoundException("User winning component does not exists");
		}
		createOrUpdateJson(getSet(), key, BLOB_BIN_NAME, (existingValue, policy) -> {
			if (existingValue != null) {
				UserWinningComponent component = jsonUtil.fromJson((String) existingValue, UserWinningComponent.class);
				if (component.getStatus() != UserWinningComponentStatus.USED) {
					throw new F4MEntryNotFoundException("Winning component not used yet - have to call markUserWinningComponentUsed first");
				}
				if (component.getWinning() != null) {
					throw new F4MEntryAlreadyExistsException("User winning already assigned");
				}
				component.setWinning(winning);
				return jsonUtil.toJson(component);
			} else {
				throw new F4MEntryNotFoundException("User winning component not found");
			}
		});
	}

	@Override
	public void markUserWinningComponentFiled(String appId, String userId, String userWinningComponentId) {
		String key = userWinningComponentPrimaryKeyUtil.createUserWinningComponentPrimaryKey(userWinningComponentId);
		if (!isKeyInMap(appId, userId, key)) {
			throw new F4MEntryNotFoundException("User winning component does not exists");
		}
		createOrUpdateJson(getSet(), key, BLOB_BIN_NAME, (existingValue, policy) -> {
			if (existingValue != null) {
				UserWinningComponent component = jsonUtil.fromJson((String) existingValue, UserWinningComponent.class);
				if (component.getStatus() != UserWinningComponentStatus.USED) {
					throw new F4MEntryNotFoundException("Winning component not used yet - have to call markUserWinningComponentUsed first");
				}
				component.setStatus(UserWinningComponentStatus.FILED);
				return jsonUtil.toJson(component);
			} else {
				throw new F4MEntryNotFoundException("User winning component not found");
			}
		});
	}

	@Override
	public void moveUserWinningComponents(String appId, String sourceUserId, String targetUserId) {
		String sourceKey = userWinningComponentPrimaryKeyUtil.createUserWinningComponentMapPrimaryKey(appId, sourceUserId);
		Map<Object, Object> sourceComponentsMap = getAllMap(getMapSet(), sourceKey, COMPONENTS_BIN_NAME);
		if (MapUtils.isNotEmpty(sourceComponentsMap)) {
			String targetKey = userWinningComponentPrimaryKeyUtil.createUserWinningComponentMapPrimaryKey(appId, targetUserId);
			createOrUpdateMap(getMapSet(), targetKey, COMPONENTS_BIN_NAME, (v, wp) -> {
				if (v != null) {
					sourceComponentsMap.putAll(v);
				}
				return sourceComponentsMap;
			});
			delete(getMapSet(), sourceKey);
		}
	}

	private boolean isKeyInMap(String appId, String userId, final String key) {
		final String mapKey = userWinningComponentPrimaryKeyUtil.createUserWinningComponentMapPrimaryKey(appId, userId);
		return isKeyInMap(mapKey, key);
	}

	private boolean isKeyInMap(final String mapKey, final String key) {
		final String componentsMapJsonString = getByKeyFromMap(getMapSet(), mapKey, COMPONENTS_BIN_NAME, key);
		return componentsMapJsonString != null;
	}

	private boolean isAvailableForUse(String c) {
		UserWinningComponent component = jsonUtil.fromJson((String) c, UserWinningComponent.class);
		return component != null && UserWinningComponentStatus.NEW == component.getStatus();
	}

	private String getSet() {
		return config.getProperty(WinningConfig.AEROSPIKE_USER_WINNING_COMPONENT_SET);
	}

	private String getMapSet() {
		return config.getProperty(WinningConfig.AEROSPIKE_USER_WINNING_COMPONENTS_MAP_SET);
	}
}
