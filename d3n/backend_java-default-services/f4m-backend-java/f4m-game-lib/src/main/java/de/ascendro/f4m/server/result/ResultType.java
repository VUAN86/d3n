package de.ascendro.f4m.server.result;

import java.util.HashMap;
import java.util.Map;

public enum ResultType {

    // Overall items
    TOTAL_QUESTIONS("totalQuestions"),
    CORRECT_ANSWERS("correctAnswers"),
    ANSWER_TIME("answerTime"),
    ELIGIBLE_TO_WINNINGS("eligibleToWinnings"),

    // Game points
    GAME_POINTS_FOR_SPEED("gamePointsForSpeed"),
    GAME_POINTS_FOR_CORRECT_ANSWER("gamePointsForCorrectAnswer"),
    TOTAL_GAME_POINTS("totalGamePoints"),
    TOTAL_GAME_POINTS_WITH_BONUS("totalGamePointsWithBonus"),
    MAXIMUM_GAME_POINTS("maximumGamePoints"),

    // Handicap points
    HANDICAP_POINTS("handicapPoints"),

    EXTRA_GAME_POINT("extraGamePoint"),
    OLD_HANDICAP("oldHandicap"),
    NEW_HANDICAP("newHandicap"),

    // Bonus points
    BONUS_POINTS("bonusPoints"),
    TOTAL_BONUS_POINTS("totalBonusPoints"),

    // Multiplayer game results
    PLACE("place"),

    // Statistics
    ANSWERED_CORRECTLY_PERCENT("answeredCorrectlyPercent"),
    ;

    private static Map<String, ResultType> values = new HashMap<>(values().length);

    static {
        for (ResultType value : values()) {
            values.put(value.getValue(), value);
        }
    }

    private String value;

    private ResultType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ResultType fromString(String value) {
        return values.get(value);
    }

}
