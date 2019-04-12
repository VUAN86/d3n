package de.ascendro.f4m.service.analytics.module.achievement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;

import de.ascendro.f4m.server.achievement.dao.UserAchievementAerospikeDao;
import de.ascendro.f4m.server.achievement.dao.UserBadgeAerospikeDao;
import de.ascendro.f4m.server.achievement.model.Achievement;
import de.ascendro.f4m.server.achievement.model.AchievementRule;
import de.ascendro.f4m.server.achievement.model.Badge;
import de.ascendro.f4m.server.achievement.model.ExecutionRule;
import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.Question;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.module.achievement.processor.AchievementRuleToEventMapper;
import de.ascendro.f4m.service.analytics.module.jobs.IAchievementProcessor;
import de.ascendro.f4m.service.analytics.util.AnalyticServiceUtil;

public class AchievementProcessor implements IAchievementProcessor {

	@InjectLogger
	private static Logger LOGGER;
	private IEventProcessor eventProcessor;
	private UserBadgeAerospikeDao userBadgeAerospikeDao;
	private UserAchievementAerospikeDao userAchievementAerospikeDao;
    private AnalyticServiceUtil analyticServiceUtil;

	@Inject
	public AchievementProcessor(IEventProcessor eventProcessor, UserBadgeAerospikeDao badgeAerospikeDao, 
			UserAchievementAerospikeDao achievementAerospikeDao,
			AnalyticServiceUtil analyticServiceUtil) {
		this.eventProcessor = eventProcessor;
		this.userBadgeAerospikeDao = badgeAerospikeDao;
		this.userAchievementAerospikeDao = achievementAerospikeDao;
		this.analyticServiceUtil = analyticServiceUtil;
	}

	@Override
	public void handleEvent(EventContent content) {
		try {
			processEvent(content);
		} catch (Exception e) {
			LOGGER.error("Error on event handle", e);
		}
	}

	@Override
	public void processEvent(EventContent content) throws Exception {
		LOGGER.info("achievements rule engine: process event content with type {} for tenant {} ", content.getEventType(), content.getTenantId());
		List<Badge> badgeList = eventProcessor.getAssociatedBadges(content);
		if (badgeList != null) {
			for (Badge badge : badgeList) {
				processBadge(badge, content);
			}
		}

	}

	private void processBadge(Badge badge, EventContent content) {
		LOGGER.debug("process event content with type {} for tenant {} for badge {}", content.getEventType(), content.getTenantId(), badge.getId());
		List<AchievementRule> rules = AchievementRuleToEventMapper
				.getAchievementsForEvent(content.getEventType());
		Map<Integer, Integer> countersToIncrement = new HashMap<>();
		for (int i = 0; i < badge.getRules().size(); i++) {
			processBadgeRule(badge, content, rules, countersToIncrement, i);
		}
		updateProgress(badge, countersToIncrement, content.getTenantId(), content.getUserId());
	}

	private void processBadgeRule(Badge badge, EventContent content, List<AchievementRule> rules,
			Map<Integer, Integer> countersToIncrement, int ruleIndex) {
		LOGGER.debug("process event content with type {} for tenant {} for badge {} and rule {}", content.getEventType(), 
				content.getTenantId(), badge.getId(), badge.getRules().get(ruleIndex).getRule());
		AchievementRule rule = badge.getRules().get(ruleIndex).getRule();
		if (rules.contains(rule)) {
			if (AchievementRule.RULE_QUESTION_USED.equals(rule) && content.isOfType(PlayerGameEndEvent.class)) {
				PlayerGameEndEvent event = content.getEventData();
				for (int j = 0; j < event.getTotalQuestions(); j++) {
					processQuestion(badge, content, ruleIndex, event, j);
				}
			} else {
				Integer incrementingCounter = getIncrementingCounter(content, badge.getRules().get(ruleIndex));
				if (incrementingCounter != 0) {
					countersToIncrement.put(ruleIndex, incrementingCounter);
				}
			}
		}
	}

	private void processQuestion(Badge badge, EventContent content, int ruleIndex, PlayerGameEndEvent event, int questionIndex) {
		Question question = event.getQuestion(questionIndex);
		LOGGER.debug("process PlayerGameEndEvent event content for RULE_QUESTION_USED for tenant {} for badge {} and user {}", 
				content.getTenantId(), badge.getId(), question.getOwnerId());
		String userId = question.getOwnerId();
		Map<Integer, Integer> progress = new HashMap<>();
		progress.put(ruleIndex, 1);
		updateProgress(badge, progress, userId, content.getTenantId());
	}

	private Integer getIncrementingCounter(EventContent content, ExecutionRule rule) {
		return content.getEventData().getAchievementIncrementingCounter(rule.getRule());
	}

	public void updateProgress(Badge badge, Map<Integer, Integer> countersToIncrement, String tenantId, String userId) {

		if(countersToIncrement.isEmpty()) {
			return;
		}
		boolean wonNewBadge = userBadgeAerospikeDao.updateBadgeProgress(badge, countersToIncrement, userId,
				tenantId);

		if(wonNewBadge) {
			LOGGER.info("user {} won a new badge {} in tenant {}", userId, badge.getId(), tenantId);
			analyticServiceUtil.addBadge(userId, tenantId, badge.getType());
			List<Achievement> achievements = eventProcessor.getAssociatedAchievements(tenantId, badge);
			userAchievementAerospikeDao.updateAchievementsProgress(badge.getId(), achievements, userId);

		}
	}

}
