package de.ascendro.f4m.server.achievement.dao;

import java.util.List;
import java.util.Map;

import de.ascendro.f4m.server.AerospikeOperateDao;
import de.ascendro.f4m.server.achievement.model.Badge;
import de.ascendro.f4m.server.achievement.model.BadgeType;
import de.ascendro.f4m.server.achievement.model.UserBadge;
import de.ascendro.f4m.service.json.model.ListResult;

public interface UserBadgeAerospikeDao extends AerospikeOperateDao {

	UserBadge getUserBadgeByBadgeId(String badgeId, String userId, String tenantId);

	ListResult<UserBadge> getUserBadgeList(String tenantId, String userId, long offset, int limit,
			BadgeType type);

	List<UserBadge> getUserBadgeInstanceList(String tenantId, long offset, int limit, String badgeId, String userId);

	/**
	 * Updates userBadge (or creates as necessary), marks badge as won if needed, creates badge instances and updates the lists
	 *
	 * @param badge
	 * @param countersToIncrement
	 * @param userId
	 * @param tenantId
	 * @return true if badge has been won, false if badge was not won
	 */
	boolean updateBadgeProgress(Badge badge, Map<Integer, Integer> countersToIncrement, String userId, String tenantId);

}
