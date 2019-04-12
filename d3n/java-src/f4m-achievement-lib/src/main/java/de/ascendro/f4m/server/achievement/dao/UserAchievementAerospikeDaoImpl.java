package de.ascendro.f4m.server.achievement.dao;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.achievement.config.AchievementConfig;
import de.ascendro.f4m.server.achievement.model.Achievement;
import de.ascendro.f4m.server.achievement.model.UserAchievement;
import de.ascendro.f4m.server.achievement.model.UserAchievementStatus;
import de.ascendro.f4m.server.achievement.util.UserAchievementPrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class UserAchievementAerospikeDaoImpl extends AerospikeOperateDaoImpl<UserAchievementPrimaryKeyUtil>
		implements UserAchievementAerospikeDao {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UserAchievementAerospikeDaoImpl.class);

	private static final String USER_ACHIEVEMENT_LIST_BIN_NAME = "usrAchieveList";
	private static final String USER_ACHIEVEMENT_BLOB_BIN_NAME = "userAchievemnt";

	@Inject
	public UserAchievementAerospikeDaoImpl(Config config, UserAchievementPrimaryKeyUtil achievementPrimaryKeyUtil,
			AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil) {
		super(config, achievementPrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public ListResult<UserAchievement> getUserAchievementList(String tenantId, String userId, long offset, int limit,
			UserAchievementStatus status) {
		String set = getUserAchievementListSet();
		String key = primaryKeyUtil.createListPrimaryKey(tenantId, userId, status);
		List<UserAchievement> itemsList = getAllMap(set, key, USER_ACHIEVEMENT_LIST_BIN_NAME).entrySet().stream()
				.map(e -> jsonUtil.fromJson((String) e.getValue(), UserAchievement.class)).collect(Collectors.toList());
		itemsList.sort((o1, o2) -> {
			if(o1.getStartDate().isEqual(o2.getStartDate()))
				return 0;
			return o1.getStartDate().isBefore(o2.getStartDate()) ? -1 : 1;
		});

		int total = itemsList.size();
		// remove items based on offset and limit
		itemsList = removeExtraItems(itemsList, offset, limit);

		return new ListResult<>(limit, offset, total, itemsList);
	}

	@Override
	public UserAchievement getUserAchievementByAchievementId(String userId, String achievementId) {
		final String key = primaryKeyUtil.createPrimaryKey(achievementId, userId);
		final String userAchievementJson = readJson(getUserAchievementSet(), key, USER_ACHIEVEMENT_BLOB_BIN_NAME);
		return StringUtils.isNotBlank(userAchievementJson)
				? jsonUtil.fromJson(userAchievementJson, UserAchievement.class) : null;
	}

	@Override
	public void updateAchievementsProgress(String badgeId, List<Achievement> achievements, String userId) {
		for (Achievement achievement : achievements) {
			final String key = primaryKeyUtil.createPrimaryKey(achievement.getId(), userId);

			createOrUpdateJson(getUserAchievementSet(), key, USER_ACHIEVEMENT_BLOB_BIN_NAME, (existing, wp) -> {
				UserAchievement userAchievement;
				if (existing != null) {
					userAchievement = jsonUtil.fromJson(existing, UserAchievement.class);
				} else {
					userAchievement = new UserAchievement(achievement, userId);
				}
				if (userAchievement.isUserAchievementExpired()) {
					// time's up for the achievement, reset it :
					userAchievement = new UserAchievement(achievement, userId);
				}
				Map<String, Boolean> wonStatus = userAchievement.getBadgesWonStatus();
				wonStatus.put(badgeId, true);
				userAchievement.setBadgesWonStatus(wonStatus);

                boolean setCompleted = userAchievement.isAchievementWon();

				if (setCompleted) {
					LOGGER.info("user {} won a new achievement {} in tenant {}", userId, userAchievement.getAchievementId(), userAchievement.getTenantId());
					userAchievement.setCompletedOn(DateTimeUtil.getCurrentDateTime());
					updateUserAchievementsList(userAchievement, UserAchievementStatus.WON);
					removeFromInProgressUserAchieventsList(userAchievement);
				} else {
					updateUserAchievementsList(userAchievement, UserAchievementStatus.IN_PROGRESS);
				}

				return jsonUtil.toJson(userAchievement);
			});
		}
	}



    private void updateUserAchievementsList(UserAchievement userAchievement,
			UserAchievementStatus userAchievementStatus) {
		String key = primaryKeyUtil.createListPrimaryKey(userAchievement.getTenantId(),
				userAchievement.getUserId(), userAchievementStatus);
		createOrUpdateMapValueByKey(getUserAchievementListSet(), key, USER_ACHIEVEMENT_LIST_BIN_NAME,
				userAchievement.getAchievementId(), ((readResult, writePolicy) -> jsonUtil.toJson(userAchievement)));
	}

	private void removeFromInProgressUserAchieventsList(UserAchievement userAchievement) {
		String key = primaryKeyUtil.createListPrimaryKey(userAchievement.getTenantId(),
				userAchievement.getUserId(), UserAchievementStatus.IN_PROGRESS);

		deleteByKeyFromMapSilently(getUserAchievementListSet(), key, USER_ACHIEVEMENT_LIST_BIN_NAME,
				userAchievement.getAchievementId());
	}

	private String getUserAchievementListSet() {
		return config.getProperty(AchievementConfig.AEROSPIKE_USER_ACHIEVEMENT_LIST_SET);
	}

	private String getUserAchievementSet() {
		return config.getProperty(AchievementConfig.AEROSPIKE_USER_ACHIEVEMENT_SET);
	}
}
