package de.ascendro.f4m.server.achievement.dao;

import java.util.List;

import de.ascendro.f4m.server.AerospikeOperateDao;
import de.ascendro.f4m.server.achievement.model.Achievement;
import de.ascendro.f4m.service.json.model.ListResult;

public interface AchievementAerospikeDao extends AerospikeOperateDao {

	/**
	 * Get Achievement By Id.
	 *
	 * @param achievementId the Achievement ID
	 * @return Achievement
	 */
	Achievement getAchievementById(String achievementId);

	/**
	 * Get all Achievement for tenant.
	 * @param tenantId 
	 * @return ListResult<Achievement>
	 */
	List<Achievement> getAll(String tenantId);

	/**
	 * Get achievements list by tenantId and achievementType
	 *
	 * @param tenantId
	 * @param offset
	 * @param limit
	 * @return AchievementListResponse
	 */
	ListResult<Achievement> getAchievementList(String tenantId, long offset, int limit);
}
