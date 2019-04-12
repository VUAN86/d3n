package de.ascendro.f4m.server.analytics.model;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.achievement.model.AchievementRule;
import de.ascendro.f4m.server.analytics.model.base.GameBaseEvent;
import de.ascendro.f4m.service.game.selection.model.game.GameType;


public class PlayerGameEndEvent extends GameBaseEvent {

    public static final String TOTAL_QUESTIONS_PROPERTY = "totalQuestions";
    public static final String SKIPPED_QUESTIONS_PROPERTY = "skippedQuestions";
    public static final String TOTAL_CORRECT_QUESTIONS_PROPERTY = "totalCorrectQuestions";
    public static final String TOTAL_INCORRECT_QUESTIONS_PROPERTY = "totalIncorrectQuestions";
    public static final String PLAYED_WITH_FRIEND_PROPERTY = "playedWithFriend";
    public static final String PLAYED_WITH_PUBLIC_PROPERTY = "playedWithPublic";
    public static final String PAID_COMPONENT_PROPERTY = "paidComponent";
    public static final String FREE_COMPONENT_PROPERTY = "freeComponent";
    public static final String PAID_COMPONENT_SKIPPED_PROPERTY = "paidComponentSkipped";
    public static final String AVERAGE_ANSWER_SPEED_PROPERTY = "averageAnswerSpeed";
    public static final String QUESTIONS_PROPERTY = "questions";
    public static final String NEW_GAME_PLAYER = "newGamePlayer";
    public static final String NEW_APP_PLAYER = "newAppPlayer";
    public static final String HANDICAP = "playerHandicap";

    public static final String SECONDS_PLAYED_PROPERTY = "secondsPlayed";
    public static final String NUM_OF_CORRECT_QUESTIONS_FAST_PROPERTY = "numOfCorrectQuestionsFastAnswered";

    public static final String IS_GAME_TYPE_DUEL_PROPERTY = "isGameTypeDuel";
    public static final String IS_GAME_TYPE_TOURNAMENT_PROPERTY = "isGameTypeTournament";
    public static final String IS_GAME_TYPE_QUICK_QUIZZ_PROPERTY = "isGameTypeQuizz";


    public static final String ANALYTICS_USER2APP_SET_NAME = "analytics_user2app";
    public static final String ANALYTICS_USER2GAME_SET_NAME = "analytics_user2game";


    private static Map<AchievementRule, Function<PlayerGameEndEvent,Integer>> achievementRulesToProperties;
    static
    {
        // @TODO : #9922 - this needs to increment userBadge for the question creator, not the current user
        achievementRulesToProperties = new HashMap<>();
        achievementRulesToProperties.put(AchievementRule.RULE_GAME_PLAYED, e -> 1);

        achievementRulesToProperties.put(AchievementRule.RULE_GAME_PLAYED_DUEL, e -> e.isGameTypeDuel() ? 1 : 0);
        achievementRulesToProperties.put(AchievementRule.RULE_GAME_PLAYED_TOURNAMENT, e -> e.isGameTypeTournament() ? 1 : 0);
        achievementRulesToProperties.put(AchievementRule.RULE_GAME_PLAYED_QUICK_QUIZZ, e -> e.isGameTypeQuizz() ? 1 : 0);

        achievementRulesToProperties.put(AchievementRule.RULE_GAME_MINUTES_PLAYED, e -> e.getSecondsPlayed().intValue());

        achievementRulesToProperties.put(AchievementRule.RULE_GAME_CORRECT_QUESTIONS_FAST, e -> e.getNumOfCorrectQuestionsFastAnswered().intValue());

    }

    public PlayerGameEndEvent() {
        setQuestionMap(new JsonArray());
    }

    public PlayerGameEndEvent(JsonObject gameEndJsonObject) {
        super(gameEndJsonObject);
    }

    public void setTotalQuestions(Integer totalQuestions) {
        setProperty(TOTAL_QUESTIONS_PROPERTY, totalQuestions);
    }

    public Integer getTotalQuestions() {
        return getPropertyAsInteger(TOTAL_QUESTIONS_PROPERTY);
    }

    public void setSkippedQuestions(Integer skippedQuestions) {
        setProperty(SKIPPED_QUESTIONS_PROPERTY, skippedQuestions);
    }

    public Integer getSkippedQuestions() {
        return getPropertyAsInteger(SKIPPED_QUESTIONS_PROPERTY);
    }

    public void setTotalCorrectQuestions(Integer totalCorrectQuestions) {
        setProperty(TOTAL_CORRECT_QUESTIONS_PROPERTY, totalCorrectQuestions);
    }

    public Integer getTotalCorrectQuestions() {
        return getPropertyAsInteger(TOTAL_CORRECT_QUESTIONS_PROPERTY);
    }

    public void setTotalIncorrectQuestions(Integer totalIncorrectQuestions) {
        setProperty(TOTAL_INCORRECT_QUESTIONS_PROPERTY, totalIncorrectQuestions);
    }

    public Integer getTotalIncorrectQuestions() {
        return getPropertyAsInteger(TOTAL_INCORRECT_QUESTIONS_PROPERTY);
    }

    public void setPlayedWithFriend(Boolean playedWithFriend) {
        setProperty(PLAYED_WITH_FRIEND_PROPERTY, playedWithFriend);
    }

    public Boolean isPlayedWithFriend() {
        return getPropertyAsBoolean(PLAYED_WITH_FRIEND_PROPERTY);
    }

    public void setPlayedWithPublic(Boolean playedWithPublic) {
        setProperty(PLAYED_WITH_PUBLIC_PROPERTY, playedWithPublic);
    }

    public Boolean isPlayedWithPublic() {
        return getPropertyAsBoolean(PLAYED_WITH_PUBLIC_PROPERTY);
    }

    public void setPaidComponent(Boolean paidComponent) {
        setProperty(PAID_COMPONENT_PROPERTY, paidComponent);
    }

    public Boolean isPaidComponent() {
        return getPropertyAsBoolean(PAID_COMPONENT_PROPERTY);
    }

    public void setFreeComponent(Boolean freeComponent) {
        setProperty(FREE_COMPONENT_PROPERTY, freeComponent);
    }

    public Boolean isFreeComponent() {
        return getPropertyAsBoolean(FREE_COMPONENT_PROPERTY);
    }

    public void setPaidComponentSkipped(Boolean paidComponentSkipped) {
        setProperty(PAID_COMPONENT_SKIPPED_PROPERTY, paidComponentSkipped);
    }

    public Boolean isPaidComponentSkipped() {
        return getPropertyAsBoolean(PAID_COMPONENT_SKIPPED_PROPERTY);
    }

    public void setAverageAnswerSpeed(Long averageAnswerSpeed) {
        setProperty(AVERAGE_ANSWER_SPEED_PROPERTY, averageAnswerSpeed);
    }

    public Long getAverageAnswerSpeed() {
        return getPropertyAsLong(AVERAGE_ANSWER_SPEED_PROPERTY);
    }

    public void setQuestionMap(JsonArray questionsMap) {
        jsonObject.add(QUESTIONS_PROPERTY, questionsMap);
    }

    public JsonArray getQuestionsMap() {
        return getPropertyAsJsonArray(QUESTIONS_PROPERTY);
    }

    public void setNewGamePlayer(Boolean newGamePlayer) {
        setProperty(NEW_GAME_PLAYER, newGamePlayer);
    }

    public Boolean isNewGamePlayer() {
        return getPropertyAsBoolean(NEW_GAME_PLAYER);
    }

    public void setNewAppPlayer(Boolean newAppPlayer) {
        setProperty(NEW_APP_PLAYER, newAppPlayer);
    }

    public Boolean isNewAppPlayer() {
        return getPropertyAsBoolean(NEW_APP_PLAYER);
    }

    public void setPlayerHandicap(Double playerHandicap) {
        setProperty(HANDICAP, playerHandicap);
    }

    public Double getPlayerHandicap() {
        return getPropertyAsDouble(HANDICAP);
    }

    public void setGameType(GameType gameType) {
        if (gameType.isTournament()) {
            setProperty(IS_GAME_TYPE_TOURNAMENT_PROPERTY, true);
        } else if (gameType.isDuel()) {
            setProperty(IS_GAME_TYPE_DUEL_PROPERTY, true);
        } else if (GameType.QUIZ24.equals(gameType)) {
            setProperty(IS_GAME_TYPE_QUICK_QUIZZ_PROPERTY, true);
        }
    }
    public Boolean isGameTypeDuel() {
        return getPropertyAsBoolean(IS_GAME_TYPE_DUEL_PROPERTY);
    }

    public Boolean isGameTypeTournament() {
        return getPropertyAsBoolean(IS_GAME_TYPE_TOURNAMENT_PROPERTY);
    }

    public Boolean isGameTypeQuizz() {
        return getPropertyAsBoolean(IS_GAME_TYPE_QUICK_QUIZZ_PROPERTY);
    }

    public void setSecondsPlayed(Long minutesPlayed) {
        setProperty(SECONDS_PLAYED_PROPERTY, minutesPlayed);
    }

    public Long getSecondsPlayed() {
        return getPropertyAsLong(SECONDS_PLAYED_PROPERTY);
    }

    public void setNumOfCorrectQuestionsFastAnswered(Long numOfCorrectQuestionsFastAnswered) {
        setProperty(NUM_OF_CORRECT_QUESTIONS_FAST_PROPERTY, numOfCorrectQuestionsFastAnswered);
    }

    public Long getNumOfCorrectQuestionsFastAnswered() {
        return getPropertyAsLong(NUM_OF_CORRECT_QUESTIONS_FAST_PROPERTY);
    }

    public void setQuestions(Question... questions) {
        final JsonArray questionsMap = new JsonArray();

        for (int i = 0; i < questions.length; i++) {
            questionsMap.add(questions[i].getJsonObject());
        }

        setQuestionMap(questionsMap);
    }

    public void addQuestion(Question question) {
        final JsonArray questionsMap = getPropertyAsJsonArray(QUESTIONS_PROPERTY);
        questionsMap.add(question.getJsonObject());
   }

    public Question getQuestion(int index) {
        JsonObject question = null;

        final JsonArray questionsMap = getPropertyAsJsonArray(QUESTIONS_PROPERTY);
        if (questionsMap != null) {
            final JsonElement questionElement = questionsMap.get(index);
            if (questionElement != null) {
                question = questionElement.getAsJsonObject();
            }
        }

        return question != null ? new Question(question) : null;
    }

	@Override
	public Integer getAchievementIncrementingCounter(AchievementRule rule) {
		Function<PlayerGameEndEvent, Integer> function = achievementRulesToProperties.get(rule);
		return function.apply(this);
    }
}
