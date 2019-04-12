package de.ascendro.f4m.server.analytics.model;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.achievement.model.AchievementRule;
import de.ascendro.f4m.server.analytics.model.base.BaseEvent;

public class QuestionFactoryEvent extends BaseEvent {

    public static final String NUM_OF_QUESTIONS_CREATED = "questionsCreated";
    public static final String NUM_OF_QUESTIONS_TRANSLATED = "questionsTranslated";
    public static final String NUM_OF_QUESTIONS_REVIEWED = "questionsReviewed";
    public static final String NUM_OF_QUESTIONS_REVIEWED_APPROVED = "questionsApprovedReviewed";
    public static final String NUM_OF_QUESTIONS_RATED = "questionsRated";
    public static final String TIME_SPENT = "timeSpentInSeconds";

    private static Map<AchievementRule, String> achievementRulesToProperties;
	static
	{
		achievementRulesToProperties = new HashMap<>();
		achievementRulesToProperties.put(AchievementRule.RULE_QUESTION_CREATED, NUM_OF_QUESTIONS_CREATED);
		achievementRulesToProperties.put(AchievementRule.RULE_QUESTION_TRANSLATED, NUM_OF_QUESTIONS_TRANSLATED);
		achievementRulesToProperties.put(AchievementRule.RULE_QUESTION_REVIEWED, NUM_OF_QUESTIONS_REVIEWED);
		achievementRulesToProperties.put(AchievementRule.RULE_QUESTION_REVIEW_APPROVED, NUM_OF_QUESTIONS_REVIEWED_APPROVED);
		achievementRulesToProperties.put(AchievementRule.RULE_QUESTION_RATED, NUM_OF_QUESTIONS_RATED);
		achievementRulesToProperties.put(AchievementRule.RULE_QUESTION_SPENT_TIME, TIME_SPENT);
	}

	public QuestionFactoryEvent() {
	}

	public QuestionFactoryEvent(JsonObject jsonObject) {
		super(jsonObject);
	}

	public Long getQuestionsCreated() {
		return getPropertyAsLong(NUM_OF_QUESTIONS_CREATED);
	}
	
	public void setQuestionsCreated(Long numOfQuestions) {
		setProperty(NUM_OF_QUESTIONS_CREATED, numOfQuestions);
	}

	public Long getQuestionsTranslated() {
		return getPropertyAsLong(NUM_OF_QUESTIONS_TRANSLATED);
	}
	
	public void setQuestionsTranslated(Long numOfQuestions) {
		setProperty(NUM_OF_QUESTIONS_TRANSLATED, numOfQuestions);
	}

	public Long getQuestionsReviewed() {
		return getPropertyAsLong(NUM_OF_QUESTIONS_REVIEWED);
	}
	
	public void setQuestionsReviewed(Long numOfQuestions) {
		setProperty(NUM_OF_QUESTIONS_REVIEWED, numOfQuestions);
	}

	public Long getQuestionsReviewedApproved() {
		return getPropertyAsLong(NUM_OF_QUESTIONS_REVIEWED_APPROVED);
	}
	
	public void setQuestionsReviewedApproved(Long numOfQuestions) {
		setProperty(NUM_OF_QUESTIONS_REVIEWED_APPROVED, numOfQuestions);
	}

	public Long getQuestionsRated() {
		return getPropertyAsLong(NUM_OF_QUESTIONS_RATED);
	}
	
	public void setQuestionsRated(Long numOfQuestions) {
		setProperty(NUM_OF_QUESTIONS_RATED, numOfQuestions);
	}

	public Long getTimeSpentInSeconds() {
		return getPropertyAsLong(TIME_SPENT);
	}
	
	public void setTimeSpentInSeconds(Long numOfQuestions) {
		setProperty(TIME_SPENT, numOfQuestions);
	}

	@Override
	public Integer getAchievementIncrementingCounter(AchievementRule rule) {
		String propertyName = achievementRulesToProperties.get(rule);
		return getPropertyAsInt(propertyName);
	}
}
