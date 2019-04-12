package de.ascendro.f4m.service.analytics.module.achievement.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;

import de.ascendro.f4m.server.achievement.dao.AchievementAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.BadgeAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.TenantsWithAchievementsAerospikeDao;
import de.ascendro.f4m.server.achievement.model.Achievement;
import de.ascendro.f4m.server.achievement.model.AchievementRule;
import de.ascendro.f4m.server.achievement.model.Badge;
import de.ascendro.f4m.server.achievement.model.ExecutionRule;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;

/**
 * 
 * loads data from aerospike and constructs TenantData structure for each tenant.
 *
 */
public class AchievementsLoader {

	@InjectLogger
	private static Logger LOGGER;

	private EventProcessor eventProcessor;
	private AchievementAerospikeDao achievementAerospikeDao;
	private BadgeAerospikeDao badgeAerospikeDao;
	private TenantsWithAchievementsAerospikeDao tenantsWithAchievementsAerospikeDao;

	@Inject
	public AchievementsLoader(EventProcessor eventProcessor, TenantsWithAchievementsAerospikeDao tenantsWithAchievementsAerospikeDao, AchievementAerospikeDao achievementAerospikeDao, BadgeAerospikeDao badgeAerospikeDao) {
		this.eventProcessor = eventProcessor;
		this.tenantsWithAchievementsAerospikeDao = tenantsWithAchievementsAerospikeDao;
		this.achievementAerospikeDao = achievementAerospikeDao;
        this.badgeAerospikeDao = badgeAerospikeDao;	        
	}

	public void loadTenants() {
		List<AchievementTenantData> tenantList = new ArrayList<>();
		List<String> tenantIdList = tenantsWithAchievementsAerospikeDao.getTenantIdList();
		tenantIdList.forEach(tenantId -> loadTenant(tenantId, tenantList));
		eventProcessor.setTenantsData(tenantList);
		LOGGER.info("achievements and rules mappings refreshed.");
	}

	private void loadTenant(String tenantId, List<AchievementTenantData> tenantList) {
		LOGGER.debug("achievement rule engine: loading achievements+badges for tenant {}", tenantId);
		AchievementTenantData tenantData = loadTenantData(tenantId);
		tenantList.add(tenantData);
	}

	private AchievementTenantData loadTenantData(String tenantId) {
		Map<AchievementRule, List<Badge>> ruleToBadgesMap = loadBadges(tenantId);		
		Map<String, List<Achievement>> badgeToAchievementsMap = loadAchievements(tenantId);		
		return new AchievementTenantData(tenantId, ruleToBadgesMap, badgeToAchievementsMap);
	}

	private Map<String, List<Achievement>> loadAchievements(String tenantId) {
		LOGGER.debug("achievement rule engine: loading achievements for tenant {}", tenantId);
		List<Achievement> allAchievements = achievementAerospikeDao.getAll(tenantId);
		Map<String, List<Achievement>> badgeToAchievementsMap = new HashMap<>();
		for (Achievement achievement : allAchievements) {
			for (String badgeId : achievement.getBadges()) {
				addAchievementToBadge(achievement, badgeId, badgeToAchievementsMap);
			}
		}
		return badgeToAchievementsMap;
	}

	private void addAchievementToBadge(Achievement achievement, String badgeId,
			Map<String, List<Achievement>> badgeToAchievementsMap) {
		List<Achievement> list = badgeToAchievementsMap.get(badgeId);
		if (Objects.isNull(list)) {
			list = new ArrayList<>();
			badgeToAchievementsMap.put(badgeId, list);
		}
		LOGGER.debug("achievement rule engine: attach achievement {} to badge {} for tenant {}", achievement.getId(), 
				badgeId, achievement.getTenantId());
		list.add(achievement);
	}

	private Map<AchievementRule, List<Badge>> loadBadges(String tenantId) {
		LOGGER.debug("achievement rule engine: loading badges for tenant {}", tenantId);
		Map<AchievementRule, List<Badge>> ruleToBadgesMap = new HashMap<>();
		List<Badge> allBadges = badgeAerospikeDao.getAll(tenantId);
		for (Badge badge : allBadges) {
			for (ExecutionRule rule : badge.getRules()) {
				addBadgeToRule(badge, rule, ruleToBadgesMap);
			}
		}
		return ruleToBadgesMap;
	}

	private void addBadgeToRule(Badge badge, ExecutionRule rule,
			Map<AchievementRule, List<Badge>> ruleToBadgesMap) {
		List<Badge> list = ruleToBadgesMap.get(rule);
		if (Objects.isNull(list)) {
			list = new ArrayList<>();
			ruleToBadgesMap.put(rule.getRule(), list);
		}
		LOGGER.debug("achievement rule engine: attach badge {} to rule {} for tenant {}", badge.getId(), 
				rule.getRule(), badge.getTenantId());
		list.add(badge);
	}
	
}