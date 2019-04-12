package de.ascendro.f4m.server.winning;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.inject.Inject;

import de.ascendro.f4m.service.winning.model.UserWinningComponent;
import de.ascendro.f4m.service.winning.model.WinningComponent;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.server.winning.util.UserWinningPrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.json.model.OrderBy;
import de.ascendro.f4m.service.json.model.OrderBy.Direction;
import de.ascendro.f4m.service.winning.WinningMessageTypes;
import de.ascendro.f4m.service.winning.model.UserWinning;
import de.ascendro.f4m.service.winning.model.UserWinningType;
import org.apache.commons.lang3.StringUtils;

public class CommonUserWinningAerospikeDaoImpl extends AerospikeOperateDaoImpl<UserWinningPrimaryKeyUtil> implements CommonUserWinningAerospikeDao {

	private static String AEROSPIKE_USER_WINNING_COMPONENT_SET = "userWinningComponent";
	private static String AEROSPIKE_WINNING_COMPONENT_SET = "winningComponent";

	@Inject
	public CommonUserWinningAerospikeDaoImpl(Config config, UserWinningPrimaryKeyUtil userWinningPrimaryKeyUtil,
			JsonUtil jsonUtil, AerospikeClientProvider aerospikeClientProvider) {
		super(config, userWinningPrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public void saveUserWinning(String appId, String userId, UserWinning userWinning) {
		userWinning.setUserWinningId(primaryKeyUtil.generateId());
		final String key = primaryKeyUtil.createPrimaryKey(appId, userWinning.getUserWinningId());
		createOrUpdateJson(getSet(), key, BLOB_BIN_NAME, (v, wp) -> userWinning.getAsString());

		addUserWinningToMap(appId, userId, userWinning);
	}

	@Override
	public WinningComponent getWinningComponent(String id) {
		final String key = primaryKeyUtil.createWinningComponentKey(id);

		final String componentAsString = readJson(AEROSPIKE_WINNING_COMPONENT_SET, key, BLOB_BIN_NAME);
		return componentAsString == null ? null : new WinningComponent(jsonUtil.fromJson(componentAsString, JsonObject.class));
	}

	private void addUserWinningToMap(String appId, String userId, UserWinning userWinning) {
		final String key = primaryKeyUtil.createUserWinningsKey(appId, userId);
		final Map<String, Object> mapEntry = new HashMap<>(2);
		mapEntry.put(UserWinning.PROPERTY_TYPE, userWinning.getType());
		mapEntry.put(UserWinning.PROPERTY_OBTAIN_DATE, userWinning.getObtainDate());

		createOrUpdateMapValueByKey(getSet(), key, WINNINGS_BIN_NAME, userWinning.getUserWinningId(),
				(v, wp) -> mapEntry);
	}

	@Override
	public UserWinning getUserWinning(String appId, String userWinningId) {
		final String key = primaryKeyUtil.createPrimaryKey(appId, userWinningId);
		final String userWinningString = readJson(getSet(), key, BLOB_BIN_NAME);

		final UserWinning userWinning;
		if (userWinningString != null) {
			final JsonObject userWinningJson = jsonUtil.fromJson(userWinningString, JsonObject.class);
			userWinning = new UserWinning(userWinningJson);
		} else {
			userWinning = null;
		}

		return userWinning;
	}

	@Override
	public List<JsonObject> getUserWinnings(String appId, String userId, int limit, long offset, List<OrderBy> orderBy) {
		final String key = primaryKeyUtil.createUserWinningsKey(appId, userId);
		final Map<String, Map<String, Object>> userWinningMap = getAllMap(getSet(), key, WINNINGS_BIN_NAME);

		final List<Entry<String, Map<String, Object>>> entries;
		if (userWinningMap != null) {
			entries = new ArrayList<>(userWinningMap.entrySet());
		} else {
			entries = new ArrayList<>();
		}

		entries.sort((e1, e2) -> compareEntries(e1, e2, orderBy));
		
		return entries.stream()
				.skip(offset < 0 ? 0 : offset)
				.limit(limit < 0 ? 0 : limit)
				.map(e -> getUserWinning(appId, e.getKey()).getJsonObject())
				.collect(Collectors.toList());
	}

	@Override
	public void moveWinnings(String appId, String sourceUserId, String targetUserId) {
		final String sourceKey = primaryKeyUtil.createUserWinningsKey(appId, sourceUserId);
		final Map<Object, Object> userWinningMap = getAllMap(getSet(), sourceKey, WINNINGS_BIN_NAME);
		if (MapUtils.isNotEmpty(userWinningMap)) {
			final String targetKey = primaryKeyUtil.createUserWinningsKey(appId, targetUserId);
			createOrUpdateMap(getSet(), targetKey, WINNINGS_BIN_NAME, (v, wp) -> {
				if (v != null) {
					userWinningMap.putAll(v);
				}
				return userWinningMap;
			});
			delete(getSet(), sourceKey);
		}
	}

	@Override
	public UserWinningComponent getWinningComponentResult(String componentId){
		final String key = primaryKeyUtil.createUserWinningComponentPrimaryKey(componentId);
		final String userWinningComponentJson = readJson(AEROSPIKE_USER_WINNING_COMPONENT_SET, key, BLOB_BIN_NAME);
		return StringUtils.isNotBlank(userWinningComponentJson)
				? jsonUtil.fromJson(userWinningComponentJson, UserWinningComponent.class) : null;
	}

	private int compareEntries(Entry<String, Map<String, Object>> e1, Entry<String, Map<String, Object>> e2, List<OrderBy> orderBy) {
		final int result;
		
		if (e1.getValue().isEmpty() && e2.getValue().isEmpty()) {
			result = 0;
		} else if (e1.getValue().isEmpty()) {
			result = 1;
		} else if (e2.getValue().isEmpty()) {
			result = -1;
		} else {
			result = compareMaps(e1.getValue(), e2.getValue(), orderBy);
		}

		return result;
	}

	private int compareMaps(Map<String, Object> m1, Map<String, Object> m2, List<OrderBy> orderBy) {
		int result = 0;

		if (!CollectionUtils.isEmpty(orderBy)) {
			for (int i = 0; i < orderBy.size() && result == 0; i++) {
				Object object1 = m1.get(orderBy.get(i).getField());
				Object object2 = m2.get(orderBy.get(i).getField());
				if (object1 instanceof UserWinningType) {
					result = object1.toString().compareTo(object2.toString());
				} else if (object1 instanceof ZonedDateTime) {
					if (object2 instanceof ZonedDateTime) {
						result = ((ZonedDateTime) object1).compareTo((ZonedDateTime) object2);
					} else if (object2 instanceof LocalDateTime) {
						result = ((ZonedDateTime) object1).toInstant().compareTo(((LocalDateTime) object2).toInstant(ZoneOffset.UTC));
					}
				} else if (object1 instanceof LocalDateTime) {
					if (object2 instanceof ZonedDateTime) {
						result = ((LocalDateTime) object1).toInstant(ZoneOffset.UTC).compareTo(((ZonedDateTime) object2).toInstant());
					} else if (object2 instanceof LocalDateTime) {
						result = ((LocalDateTime) object1).compareTo((LocalDateTime) object2);
					}
				}
				result *= orderBy.get(i).getDirection() == Direction.asc ? 1 : -1;
			}
		}

		return result;
	}

	protected String getSet() {
		return WinningMessageTypes.SERVICE_NAME;
	}

}
