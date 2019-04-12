package de.ascendro.f4m.server.achievement.dao;

import java.util.List;

import de.ascendro.f4m.server.AerospikeOperateDao;
import de.ascendro.f4m.server.achievement.model.Achievement;
import de.ascendro.f4m.server.achievement.model.UserAchievement;
import de.ascendro.f4m.server.achievement.model.UserAchievementStatus;
import de.ascendro.f4m.service.json.model.ListResult;

public interface UserAchievementAerospikeDao extends AerospikeOperateDao {

	/**
	 * Get UserAchievement by achievementId
	 * @param userId
	 * @param achievementId
	 *
	 * @return UserAchievement
	 */
	UserAchievement getUserAchievementByAchievementId(String userId, String achievementId);

	/**
	 * Get user achievements by tenantId and user-achievement-status
	 *
	 * @param tenantId
	 * @param userId
	 *@param offset
	 * @param limit
	 * @param status
	 * @return List<UserAchievement>
	 */
	ListResult<UserAchievement> getUserAchievementList(String tenantId, String userId, long offset, int limit,
			UserAchievementStatus status);

	/**
	 * Updates achievements marking progress for given badgeId, and updates the userBadge object and lists
	 *
	 *
	 * @param badgeId
	 * @param achievements
	 * @param userId
	 */
	void updateAchievementsProgress(String badgeId, List<Achievement> achievements, String userId);
}
