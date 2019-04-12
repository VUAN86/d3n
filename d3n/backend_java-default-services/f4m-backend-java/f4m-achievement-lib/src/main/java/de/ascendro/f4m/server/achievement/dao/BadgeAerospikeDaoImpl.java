package de.ascendro.f4m.server.achievement.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeOperateDaoImpl;
import de.ascendro.f4m.server.achievement.config.AchievementConfig;
import de.ascendro.f4m.server.achievement.model.Badge;
import de.ascendro.f4m.server.achievement.model.BadgeType;
import de.ascendro.f4m.server.achievement.util.BadgePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.json.model.ListResult;

public class BadgeAerospikeDaoImpl extends AerospikeOperateDaoImpl<BadgePrimaryKeyUtil> implements BadgeAerospikeDao {

	private static final String BADGE_BLOB_BIN_NAME = "badge";
	private static final String BADGE_LIST_BIN_NAME = "badgeList";

	@Inject
	public BadgeAerospikeDaoImpl(Config config, BadgePrimaryKeyUtil achievementPrimaryKeyUtil,
			AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil) {
		super(config, achievementPrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public ListResult<Badge> getBadgeList(String tenantId, long offset, int limit, BadgeType type) {
		String badgeListPrimaryKey = primaryKeyUtil.createListPrimaryKey(tenantId, type);
		List<Badge> badges = getBadges(badgeListPrimaryKey);

		long total = badges.size();
		badges = removeExtraItems(badges, offset, limit);

		return new ListResult<>(limit, offset, total, badges);
	}

	@Override
	public Badge getBadgeById(String badgeId) {
		final String badgePK = primaryKeyUtil.createPrimaryKey(badgeId);
		final String badgetJson = readJson(getBadgeSet(), badgePK, BADGE_BLOB_BIN_NAME);
		return StringUtils.isNotBlank(badgetJson) ? jsonUtil.fromJson(badgetJson, Badge.class) : null;
	}

	@Override
	public List<Badge> getAll(String tenantId) {
		List<Badge> list = new ArrayList<>();
		String gameListKey = primaryKeyUtil.createListPrimaryKey(tenantId, BadgeType.GAME);
		addBadges(gameListKey, list);
		String communityListKey = primaryKeyUtil.createListPrimaryKey(tenantId, BadgeType.COMMUNITY);
		addBadges(communityListKey, list);
		String battleListKey = primaryKeyUtil.createListPrimaryKey(tenantId, BadgeType.BATTLE);
		addBadges(battleListKey, list);
		return list;
	}

	private void addBadges(String badgeListPrimaryKey, List<Badge> list) {
		String set = getBadgeListSet();
		if (exists(set, badgeListPrimaryKey)) {
			List<Badge> badges = getBadges(badgeListPrimaryKey);
			list.addAll(badges);
		}
	}

	private List<Badge> getBadges(String badgeListPrimaryKey) {
		List<Badge> badges = getAllMap(getBadgeListSet(), badgeListPrimaryKey,
				BADGE_LIST_BIN_NAME).entrySet().stream().map(e -> jsonUtil.fromJson((String) e.getValue(), Badge.class))
						.collect(Collectors.toList());
		return badges;
	}

	private String getBadgeSet() {
		return config.getProperty(AchievementConfig.AEROSPIKE_BADGE_SET);
	}

	private String getBadgeListSet() {
		return config.getProperty(AchievementConfig.AEROSPIKE_BADGE_LIST_SET);
	}
}
