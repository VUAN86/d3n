package de.ascendro.f4m.service.analytics.module.achievement.processor;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import de.ascendro.f4m.server.achievement.model.Achievement;
import de.ascendro.f4m.server.achievement.model.AchievementRule;
import de.ascendro.f4m.server.achievement.model.Badge;

/**
 * 
 * Data structure to hold configured badges and achievements for a tentant.
 *
 */
class AchievementTenantData {
	
	private String tenantId;
	private final EnumMap<AchievementRule, List<Badge>> ruleToBadgesMap = new EnumMap<>(AchievementRule.class);
	private final Map<String, List<Achievement>> badgeToAchievementsMap = new HashMap<>();

	public AchievementTenantData(String tenantId, Map<AchievementRule, List<Badge>> ruleToBadgesMap, Map<String, List<Achievement>> badgeToAchievementsMap) {
		super();
		this.tenantId = tenantId;
		this.ruleToBadgesMap.putAll(ruleToBadgesMap);
		this.badgeToAchievementsMap.putAll(badgeToAchievementsMap);
	}

	public String getTenantId() {
		return tenantId;
	}

	public List<Badge> getBadgesFor(List<AchievementRule> rules) {
		return ruleToBadgesMap.entrySet().parallelStream()
				.filter(entry -> isRuleInList(entry, rules))
				.map(Entry::getValue)
				.flatMap(List::stream)
				.distinct()
				.collect(Collectors.toList());
	}

	public List<Achievement> getAchievementsFor(Badge badge) {
		List<Achievement> list = badgeToAchievementsMap.get(badge.getId());
		return Objects.isNull(list) ? Collections.emptyList() : list;
	}

	public List<Achievement> getAchievementsFor(List<Badge> badges) {
		return badgeToAchievementsMap.entrySet().parallelStream()
				.filter(entry -> isBadgeInList(entry, badges))
				.map(entry -> entry.getValue())
				.flatMap(List::stream)
				.distinct()
				.collect(Collectors.toList());
	}

	private boolean isBadgeInList(Entry<String, List<Achievement>> entry, List<Badge> badges) {
		return badges.stream().anyMatch(b -> b.getId().equals(entry.getKey()));
	}

	private boolean isRuleInList(Entry<AchievementRule, List<Badge>> entry, List<AchievementRule> rules) {
		return rules.contains(entry.getKey());
	}

}