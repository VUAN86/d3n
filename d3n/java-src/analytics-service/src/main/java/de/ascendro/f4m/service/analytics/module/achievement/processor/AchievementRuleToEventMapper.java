package de.ascendro.f4m.service.analytics.module.achievement.processor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.ascendro.f4m.server.achievement.model.AchievementRule;
import de.ascendro.f4m.server.analytics.model.InviteEvent;
import de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.QuestionFactoryEvent;
import de.ascendro.f4m.server.analytics.model.RewardEvent;

public class AchievementRuleToEventMapper {
	
	private static final Map<String, List<AchievementRule>> mapper = new HashMap<>();
	
	static {
		mapper.put(QuestionFactoryEvent.class.getCanonicalName(),
				Arrays.asList(AchievementRule.RULE_GAME_ANSWERED_QUESTIONS, 
					AchievementRule.RULE_QUESTION_CREATED, 
					AchievementRule.RULE_QUESTION_TRANSLATED, 
					AchievementRule.RULE_QUESTION_REVIEWED, 
					AchievementRule.RULE_QUESTION_RATED, 
					AchievementRule.RULE_QUESTION_SPENT_TIME));
		mapper.put(PlayerGameEndEvent.class.getCanonicalName(),
				Arrays.asList(AchievementRule.RULE_QUESTION_USED, 
					AchievementRule.RULE_GAME_PLAYED, 
					AchievementRule.RULE_GAME_MINUTES_PLAYED, 
					AchievementRule.RULE_GAME_PLAYED_QUICK_QUIZZ, 
					AchievementRule.RULE_GAME_PLAYED_DUEL, 
					AchievementRule.RULE_GAME_PLAYED_TOURNAMENT, 
					AchievementRule.RULE_GAME_ANSWERED_QUESTIONS, 
					AchievementRule.RULE_GAME_CORRECT_QUESTIONS, 
					AchievementRule.RULE_GAME_CORRECT_QUESTIONS_FAST));
		mapper.put(InviteEvent.class.getCanonicalName(),
				Arrays.asList(AchievementRule.RULE_GAME_INVITED));
		mapper.put(MultiplayerGameEndEvent.class.getCanonicalName(),
				Arrays.asList(AchievementRule.RULE_GAME_WON_GAMES));
		mapper.put(RewardEvent.class.getCanonicalName(),
				Arrays.asList(AchievementRule.RULE_GAME_WON_BONUS, 
					AchievementRule.RULE_GAME_WON_CREDITS, 
					AchievementRule.RULE_GAME_WON_MONEY, 
					AchievementRule.RULE_GAME_WON_VOUCHERS));
	}
	
	public static List<AchievementRule> getAchievementsForEvent(String type) {
		return mapper.get(type);
	}

}
