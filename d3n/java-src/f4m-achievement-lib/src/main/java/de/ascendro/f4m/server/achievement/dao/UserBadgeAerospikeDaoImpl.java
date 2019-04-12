package de.ascendro.f4m.server.achievement.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.aerospike.client.Bin;
import com.aerospike.client.Operation;
import com.aerospike.client.Record;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.achievement.config.AchievementConfig;
import de.ascendro.f4m.server.achievement.model.Badge;
import de.ascendro.f4m.server.achievement.model.BadgeType;
import de.ascendro.f4m.server.achievement.model.UserBadge;
import de.ascendro.f4m.server.achievement.util.UserBadgePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class UserBadgeAerospikeDaoImpl extends AerospikeOperateDaoImpl<UserBadgePrimaryKeyUtil> implements UserBadgeAerospikeDao {

	private static final String USER_BADGE_BLOB_BIN_NAME = "userBadge";
	private static final String USER_BADGE_PROGRESS_BIN_NAME = "progress:";
	private static final String USER_BADGE_LIST_BIN_NAME = "userBadgeList";

	@Inject
	public UserBadgeAerospikeDaoImpl(Config config, UserBadgePrimaryKeyUtil achievementPrimaryKeyUtil,
			AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil) {
		super(config, achievementPrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public UserBadge getUserBadgeByBadgeId(String badgeId, String userId, String tenantId) {
		final String key = primaryKeyUtil.createPrimaryKey(badgeId, userId);
		Record record = readRecord(getUserBadgeSet(), key);

		return getUserBadge(tenantId, record);
	}

	private UserBadge getUserBadge(String tenantId, Record record) {
		final String userBadgeJson = readJson(USER_BADGE_BLOB_BIN_NAME, record);
		UserBadge userBadge = StringUtils.isNotBlank(userBadgeJson) ? jsonUtil.fromJson(userBadgeJson, UserBadge.class)
				: null;
		if (userBadge == null || !StringUtils.equals(userBadge.getTenantId(), tenantId)) {
			throw new F4MEntryNotFoundException();
		}
		for (int i = 0; i < userBadge.getRules().size(); i++) {
			userBadge.getRules().get(i).setProgress(record.getInt(USER_BADGE_PROGRESS_BIN_NAME + i));
		}
		return userBadge;
	}

	@Override
	public List<UserBadge> getUserBadgeInstanceList(String tenantId, long offset, int limit, String badgeId,
			String userId) {
		UserBadge badge = getUserBadgeByBadgeId(badgeId, userId, tenantId);
		String[] keys = new String[badge.getWonCount()];
		for (int i = 0; i < badge.getWonCount(); i++) {
			keys[i] = primaryKeyUtil.createInstancePrimaryKey(badgeId, userId, i);
		}

		String[] badgeJsons = readJsons(getUserBadgeSet(), keys, USER_BADGE_BLOB_BIN_NAME);

		List<UserBadge> result = new ArrayList<>();
		for (String badgeJson : badgeJsons) {
			result.add(jsonUtil.fromJson(badgeJson, UserBadge.class));
		}

		result = removeExtraItems(result, offset, limit);

		return result;
	}

	@Override
	public ListResult<UserBadge> getUserBadgeList(String tenantId, String userId, long offset, int limit,
			BadgeType type) {
		List<UserBadge> items = getAllMap(getUserBadgeListSet(),
				primaryKeyUtil.createListPrimaryKey(tenantId, userId, type), USER_BADGE_LIST_BIN_NAME)
						.entrySet().stream().map(e -> jsonUtil.fromJson((String) e.getValue(), UserBadge.class))
						.collect(Collectors.toList());

		long total = items.size();
		items = removeExtraItems(items, offset, limit);
		return new ListResult<>(limit, offset, total, items);
	}

	@Override
	public boolean updateBadgeProgress(Badge badge, Map<Integer, Integer> countersToIncrement, String userId,
			String tenantId) {
		String userBadgeKey = primaryKeyUtil.createPrimaryKey(badge.getId(), userId);

		// make atomic incrementation for the progress counter :
		// check if counterRecord exists, if not create it, otherwise we get a NPE below :
		boolean exists = exists(getUserBadgeSet(), userBadgeKey);
		if(!exists) {
			UserBadge userBadge = new UserBadge(badge, userId, tenantId);
			createRecord(getUserBadgeSet(), userBadgeKey, getJsonBin(USER_BADGE_BLOB_BIN_NAME, jsonUtil.toJson(userBadge)));
		}

		Record record = operate(getUserBadgeSet(), userBadgeKey, new Operation[] { Operation.get() },
				(readResult, ops) -> {
					List<Operation> writeOperations = new ArrayList<>();

					for (Map.Entry<Integer, Integer> incrementingCounter : countersToIncrement.entrySet()) {
						Bin bin = getLongBin(USER_BADGE_PROGRESS_BIN_NAME + incrementingCounter.getKey(),
								incrementingCounter.getValue());
						writeOperations.add(Operation.add(bin));
					}
					return writeOperations;
				});
		// if needed, create won instance for badge :
		UserBadge userBadge = this.getUserBadge(tenantId, record);

		boolean hasWonBadge = checkAndCreateUserBadgeInstance(userBadge, userId, badge.getType());
		this.updateUserBadgeList(userBadge, badge.getType());
		return hasWonBadge;
	}

	private boolean checkAndCreateUserBadgeInstance(UserBadge userBadge, String userId, BadgeType type) {
		Integer wonCount = userBadge.getWonCount();
		Integer lowestRuleProgress = 0;
		boolean wonBadge = false;
		for (int i = 0; i < userBadge.getRules().size(); i++) {
			if (i == 0) {
				lowestRuleProgress = userBadge.getRules().get(i).getProgress()
						/ userBadge.getRules().get(i).getLevel();
			} else {
				Integer winForRule = userBadge.getRules().get(i).getProgress()
						/ userBadge.getRules().get(i).getLevel();
				if (winForRule < lowestRuleProgress) {
					lowestRuleProgress = winForRule;
				}
			}
		}

		if (lowestRuleProgress > wonCount) {
			// need to create new badge instances :
			for (int i = 0; i < lowestRuleProgress - wonCount; i++) {
				writeNewUserBadge(userBadge, wonCount + i);
			}
			updateUserBadgeWonCounts(userId, userBadge.getBadgeId(), lowestRuleProgress, type);

			wonBadge = true;
		}

		return wonBadge;
	}

	private void updateUserBadgeWonCounts(String userId, String badgeId, int wonCount, BadgeType type) {

		final String userBadgeJson = updateJson(getUserBadgeSet(), primaryKeyUtil.createPrimaryKey(badgeId, userId), USER_BADGE_BLOB_BIN_NAME, (json, wp) -> {
			UserBadge badge;
			badge = jsonUtil.fromJson(json, UserBadge.class);
			badge.setWonCount(wonCount);
			return jsonUtil.toJson(badge);
		});

		UserBadge userBadge = jsonUtil.fromJson(userBadgeJson, UserBadge.class);
		updateUserBadgeList(userBadge, type);
	}

	private void writeNewUserBadge(UserBadge userBadge, int instanceId) {
		UserBadge newInstance = new UserBadge();
		newInstance.setUserId(userBadge.getUserId());
		newInstance.setBadgeId(userBadge.getBadgeId());
		newInstance.setCreatorId(userBadge.getUserId());
		newInstance.setHistory(new ArrayList<>());
		newInstance.setCreatedOn(DateTimeUtil.getCurrentDateTime());
		// @Todo : what should expiresOn be set to?
		newInstance.setExpiresOn(userBadge.getExpiresOn());

		createJson(getUserBadgeSet(), primaryKeyUtil.createInstancePrimaryKey(userBadge.getBadgeId(),
				userBadge.getUserId(), instanceId), USER_BADGE_BLOB_BIN_NAME, jsonUtil.toJson(newInstance));
	}

	private void updateUserBadgeList(UserBadge userBadge, BadgeType badgeType) {
		String key = primaryKeyUtil.createListPrimaryKey(userBadge.getTenantId(), userBadge.getUserId(),
				badgeType);
		createOrUpdateMapValueByKey(getUserBadgeListSet(), key, USER_BADGE_LIST_BIN_NAME, userBadge.getBadgeId(),
				((readResult, writePolicy) -> jsonUtil.toJson(userBadge)));
	}

	private String getUserBadgeSet() {
		return config.getProperty(AchievementConfig.AEROSPIKE_USER_BADGE_SET);
	}

	private String getUserBadgeListSet() {
		return config.getProperty(AchievementConfig.AEROSPIKE_USER_BADGE_LIST_SET);
	}
}
