package de.ascendro.f4m.service.analytics.module.achievement.processor;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import de.ascendro.f4m.server.achievement.model.Achievement;
import de.ascendro.f4m.server.achievement.model.AchievementRule;
import de.ascendro.f4m.server.achievement.model.Badge;
import de.ascendro.f4m.service.analytics.module.achievement.processor.AchievementTenantData;

public class AchievementTenantDataTest {
	
	private static final String TENANT_00 = "tenant-00";
	private static final String COMMUNITY_ACHIEVE_1 = "comm-achieve-1";
	private static final String COMMUNITY_BADGE_1 = "comm-badge-1";
	private static final String GAME_ACHIEVE_1 = "game-achieve-1";
	private static final String GAME_BADGE_2 = "game-badge-2";
	private static final String GAME_BADGE_1 = "game-badge-1";
	
	private AchievementTenantData service;

	@Before
	public void setUp() throws Exception {
		service = createTenant(TENANT_00);
	}

	@Test
	public void testGetBadgesFor() {
		List<Badge> badges = service.getBadgesFor(Arrays.asList(AchievementRule.RULE_GAME_PLAYED));
		assertEquals(2, badges.size());
	}

	@Test
	public void testGetBadgesForMany() {
		List<Badge> badges = service.getBadgesFor(Arrays.asList(AchievementRule.RULE_GAME_PLAYED, AchievementRule.RULE_QUESTION_USED));
		assertEquals(3, badges.size());
	}

	@Test
	public void testGetBadgesForNone() {
		List<Badge> badges = service.getBadgesFor(Arrays.asList(AchievementRule.RULE_GAME_ANSWERED_QUESTIONS, AchievementRule.RULE_GAME_WON_VOUCHERS));
		assertTrue(badges.isEmpty());
	}

	@Test
	public void testGetAchievementsFor() {
		List<Achievement> achievementsFor = service.getAchievementsFor(createBadge(GAME_BADGE_1));
		assertEquals(1, achievementsFor.size());
	}

	@Test
	public void testGetAchievementsForNone() {
		List<Achievement> achievementsFor = service.getAchievementsFor(createBadge(GAME_BADGE_2));
		assertTrue(achievementsFor.isEmpty());
	}

	@Test
	public void testGetAchievementsForList() {
		List<Achievement> achievementsFor = service.getAchievementsFor(Arrays.asList(createBadge(GAME_BADGE_1), createBadge(COMMUNITY_BADGE_1)));
		assertEquals(2, achievementsFor.size());
	}

	@Test
	public void testGetAchievementsForListNone() {
		List<Achievement> achievementsFor = service.getAchievementsFor(Arrays.asList(createBadge(GAME_BADGE_2)));
		assertTrue(achievementsFor.isEmpty());
	}

	private AchievementTenantData createTenant(String id) {
		Map<AchievementRule, List<Badge>> ruleToBadgesMap = createRuleToBadges();
		Map<String, List<Achievement>> badgeToAchievementsMap = createBadgeToAchievements();
		AchievementTenantData tenant1 = new AchievementTenantData(id, ruleToBadgesMap, badgeToAchievementsMap);
		return tenant1;
	}

	private Map<String, List<Achievement>> createBadgeToAchievements() {
		Map<String, List<Achievement>> badgeToAchievementsMap = new HashMap<>();
		badgeToAchievementsMap.put(GAME_BADGE_1, Arrays.asList(createAchievement(GAME_ACHIEVE_1)));
		badgeToAchievementsMap.put(COMMUNITY_BADGE_1, Arrays.asList(createAchievement(COMMUNITY_ACHIEVE_1)));
		return badgeToAchievementsMap;
	}

	private Map<AchievementRule, List<Badge>> createRuleToBadges() {
		Map<AchievementRule, List<Badge>> ruleToBadgesMap = new HashMap<>();
		ruleToBadgesMap.put(AchievementRule.RULE_GAME_PLAYED, Arrays.asList(createBadge(GAME_BADGE_1), createBadge(GAME_BADGE_2)));
		ruleToBadgesMap.put(AchievementRule.RULE_QUESTION_USED, Arrays.asList(createBadge(COMMUNITY_BADGE_1)));
		return ruleToBadgesMap;
	}

	private Badge createBadge(String id) {
		Badge badge = new Badge();
		badge.setId(id);
		return badge;
	}

	private Achievement createAchievement(String id) {
		Achievement achievement = new Achievement();
		achievement.setId(id);
		return achievement;
	}

}
