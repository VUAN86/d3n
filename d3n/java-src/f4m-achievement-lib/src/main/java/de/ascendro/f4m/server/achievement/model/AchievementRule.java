package de.ascendro.f4m.server.achievement.model;

public enum AchievementRule {
	
	RULE_QUESTION_CREATED("Create a number of questions"),
	RULE_QUESTION_TRANSLATED("Translate a number of questions"),
	RULE_QUESTION_REVIEWED("Reviewed a number of questions"),
	RULE_QUESTION_REVIEW_APPROVED("Approved a number of questions"),
	RULE_QUESTION_RATED("Provide a number of ratings"),
	RULE_QUESTION_SPENT_TIME("Spent x minutes on the Question Factory"),
	RULE_QUESTION_USED("Number of questions used games"),
	
	RULE_GAME_INVITED("Invited a number of players"),
	RULE_GAME_PLAYED("Played a number of games"),
	RULE_GAME_WON_GAMES("Won a number of games"),
	RULE_GAME_WON_BONUS("Won x bonus points in total"),
	RULE_GAME_WON_CREDITS("Won x credits in total"),
	RULE_GAME_WON_MONEY("Won x money in total"),
	RULE_GAME_WON_VOUCHERS("Won x vouchers"),
	RULE_GAME_MINUTES_PLAYED("Played x minutes of the game"),
	RULE_GAME_PLAYED_QUICK_QUIZZ("Played x Quick Quizes"),
	RULE_GAME_PLAYED_DUEL("Played x Duels"),
	RULE_GAME_PLAYED_TOURNAMENT("Played x Tournaments"),
	RULE_GAME_ANSWERED_QUESTIONS("Answered x questions"),
	RULE_GAME_CORRECT_QUESTIONS("Had x number of questions correct"),
	RULE_GAME_CORRECT_QUESTIONS_FAST("Had answered question right in speed");
	
	private String description;
	
	private AchievementRule(String description) {
		this.description = description;
	}
	
	public static AchievementRule getByDescription(String description) {
		for (AchievementRule rule : values()) {
			if (rule.description.equals(description)) {
				return rule;
			}
		}
		throw new IllegalArgumentException(description);
	}

}
