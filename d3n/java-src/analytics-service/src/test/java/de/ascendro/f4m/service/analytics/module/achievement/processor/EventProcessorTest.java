package de.ascendro.f4m.service.analytics.module.achievement.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import de.ascendro.f4m.server.achievement.model.Achievement;
import de.ascendro.f4m.server.achievement.model.AchievementRule;
import de.ascendro.f4m.server.achievement.model.Badge;
import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.model.GameEndEvent;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.QuestionFactoryEvent;
import de.ascendro.f4m.service.analytics.module.achievement.processor.AchievementTenantData;
import de.ascendro.f4m.service.analytics.module.achievement.processor.EventProcessor;

@RunWith(MockitoJUnitRunner.class)
public class EventProcessorTest {
	
	private static final String COMMUNITY_ACHIEVE_1 = "comm-achieve-1";
	private static final String COMMUNITY_BADGE_1 = "comm-badge-1";
	private static final String GAME_ACHIEVE_1 = "game-achieve-1";
	private static final String GAME_BADGE_2 = "game-badge-2";
	private static final String GAME_BADGE_1 = "game-badge-1";
	private static final String TENANT_2 = "tenant-2";
	private static final String TENANT_1 = "tenant-1";
	private static final String TENANT_11 = "tenant-11";
	
	private EventProcessor service = new EventProcessor();

	@Before
	public void setUp() throws Exception {
		List<AchievementTenantData> tenantList = new ArrayList<>();
		AchievementTenantData tenant1 = createTenant(TENANT_1);
		tenantList.add(tenant1);
		AchievementTenantData tenant2 = createTenant(TENANT_2);
		tenantList.add(tenant2);
		
		service.setTenantsData(tenantList);
	}

	@Test
	public void testGetAssociatedBadgesNoRule() {		
		EventContent eventContent = new EventContent();
		eventContent.setEventType(GameEndEvent.class.getCanonicalName());
		eventContent.setEventData(new GameEndEvent());
		eventContent.setTenantId(TENANT_1);
		
		List<Badge> associatedBadges = service.getAssociatedBadges(eventContent);
		assertEquals(0, associatedBadges.size());
	}

	@Test
	public void testGetAssociatedBadges() {		
		EventContent eventContent = new EventContent();
		eventContent.setEventType(PlayerGameEndEvent.class.getCanonicalName());
		eventContent.setEventData(new PlayerGameEndEvent());
		eventContent.setTenantId(TENANT_1);
		
		List<Badge> associatedBadges = service.getAssociatedBadges(eventContent);
		assertEquals(3, associatedBadges.size());
	}

	@Test
	public void testGetAssociatedBadgesNone() {		
		EventContent eventContent = new EventContent();
		eventContent.setEventType(QuestionFactoryEvent.class.getCanonicalName());
		eventContent.setEventData(new QuestionFactoryEvent());
		eventContent.setTenantId(TENANT_1);
		
		List<Badge> associatedBadges = service.getAssociatedBadges(eventContent);
		assertTrue(associatedBadges.isEmpty());
	}

	@Test
	public void testGetAssociatedAchievements() {
		List<Achievement> associatedAchievements = service.getAssociatedAchievements(TENANT_1, createBadge(GAME_BADGE_1));
		assertEquals(1, associatedAchievements.size());
	}

	@Test
	public void testGetAssociatedAchievementsManyBadges() {
		List<Achievement> associatedAchievements = service.getAssociatedAchievements(TENANT_1, Arrays.asList(createBadge(GAME_BADGE_1)));
		assertEquals(1, associatedAchievements.size());
	}

	@Test
	public void testGetAssociatedAchievementsNone() {
		List<Achievement> associatedAchievements = service.getAssociatedAchievements(TENANT_1, createBadge(GAME_BADGE_2));
		assertTrue(associatedAchievements.isEmpty());
	}

	@Test
	public void testGetAssociatedAchievementsManyBadgesNone() {
		List<Achievement> associatedAchievements = service.getAssociatedAchievements(TENANT_1, Arrays.asList(createBadge(GAME_BADGE_2)));		
		assertTrue(associatedAchievements.isEmpty());
	}

	@Test
	public void testGetAssociatedAchievementsException() {
		List<Achievement> associatedAchievements = service.getAssociatedAchievements(TENANT_11, createBadge(GAME_BADGE_2));
		assertTrue(associatedAchievements.isEmpty());
	}

	@Test
	public void testSetTenantsData() {
		List<AchievementTenantData> tenantList = new ArrayList<>();
		AchievementTenantData tenant2 = createTenant(TENANT_11);
		tenantList.add(tenant2);
		
		service.setTenantsData(tenantList);
		
		List<Achievement> associatedAchievements = service.getAssociatedAchievements(TENANT_11, createBadge(GAME_BADGE_1));
		assertEquals(1, associatedAchievements.size());
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
