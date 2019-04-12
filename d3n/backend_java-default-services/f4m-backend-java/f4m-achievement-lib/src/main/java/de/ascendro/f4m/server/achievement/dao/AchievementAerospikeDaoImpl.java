package de.ascendro.f4m.server.achievement.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.achievement.config.AchievementConfig;
import de.ascendro.f4m.server.achievement.model.Achievement;
import de.ascendro.f4m.server.achievement.util.AchievementPrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.json.model.ListResult;

public class AchievementAerospikeDaoImpl extends AerospikeOperateDaoImpl<AchievementPrimaryKeyUtil>
		implements AchievementAerospikeDao {

	private static final String BLOB_BIN_NAME = "achievement";
	private static final String ACHIEVEMENT_LIST_BIN_NAME = "achieveList";

	@Inject
	public AchievementAerospikeDaoImpl(Config config, AchievementPrimaryKeyUtil achievementPrimaryKeyUtil,
			AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil) {
		super(config, achievementPrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public Achievement getAchievementById(String achievementId) {
		final String achievementPK = primaryKeyUtil.createPrimaryKey(achievementId);
		final String achievementJson = readJson(getAchievementSet(), achievementPK, BLOB_BIN_NAME);
		return StringUtils.isNotBlank(achievementJson) ? jsonUtil.fromJson(achievementJson, Achievement.class) : null;
	}

	@Override
	public List<Achievement> getAll(String tenantId) {
		List<Achievement> list = new ArrayList<>();
		String achievementListKey = primaryKeyUtil.createListPrimaryKey(tenantId);
		String set = getAchievementListSet();
		if (exists(set, achievementListKey)) {
			List<Achievement> achievementList = getAchievements(achievementListKey);
			list.addAll(achievementList);
		}
		return list;
	}

	@Override
	public ListResult<Achievement> getAchievementList(String tenantId, long offset, int limit) {

		String achievementListPrimaryKey = primaryKeyUtil.createListPrimaryKey(tenantId);
		List<Achievement> achievements = getAchievements(achievementListPrimaryKey);

		long total = achievements.size();
		achievements = removeExtraItems(achievements, offset, limit);

		return new ListResult<>(limit, offset, total, achievements);
	}

	private List<Achievement> getAchievements(String achievementListPrimaryKey) {
		List<Achievement> achievements = getAllMap(getAchievementListSet(),
				achievementListPrimaryKey, ACHIEVEMENT_LIST_BIN_NAME).entrySet()
						.stream().map(e -> jsonUtil.fromJson((String) e.getValue(), Achievement.class))
						.collect(Collectors.toList());
		return achievements;
	}

	private String getAchievementSet() {
		return config.getProperty(AchievementConfig.AEROSPIKE_ACHIEVEMENT_SET);
	}

	private String getAchievementListSet() {
		return config.getProperty(AchievementConfig.AEROSPIKE_ACHIEVEMENT_LIST_SET);
	}

}
