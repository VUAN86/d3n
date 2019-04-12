package de.ascendro.f4m.server.achievement.dao;

import java.util.List;

import de.ascendro.f4m.server.AerospikeOperateDao;
import de.ascendro.f4m.server.achievement.model.Badge;
import de.ascendro.f4m.server.achievement.model.BadgeType;
import de.ascendro.f4m.service.json.model.ListResult;

public interface BadgeAerospikeDao extends AerospikeOperateDao {

	/**
	 * Get Badge By Id.
	 * 
	 * @param badgeId the Badge ID
	 * @return Badge
	 */
	Badge getBadgeById(String badgeId);

	/**
	 * Get all Badges for tenant
	 * @param tenantId 
	 * @return
	 */
	List<Badge> getAll(String tenantId);

	ListResult<Badge> getBadgeList(String tenantId, long offset, int limit, BadgeType type);
}
