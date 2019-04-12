package de.ascendro.f4m.service.analytics.module.statistic.model;

import de.ascendro.f4m.service.analytics.module.statistic.model.base.BaseStatisticTable;

public class QuestionTranslation extends BaseStatisticTable{
    @SuppressWarnings("unused")
    private static final String TABLE_NAME = "question_translation";
    private static final String KEY_FIELD = "id";

    private static final String FIELD_ASSOCIATED_POOLS = "stat_associatedPools";
    private static final String FIELD_ASSOCIATED_GAMES = "stat_associatedGames";
    private static final String FIELD_GAMES_PLAYED = "stat_gamesPlayed";
    private static final String FIELD_RIGHT_ANSWERS = "stat_rightAnswers";
    private static final String FIELD_WRONG_ANSWERS = "stat_wrongAnswers";
    private static final String FIELD_AVERAGE_ANSWER_SPEED = "stat_averageAnswerSpeed";
    private static final String FIELD_RATING = "stat_rating";

    public void setId(long value) {
        this.setValue(KEY_FIELD, value);
    }

    public void setAssociatedPools(long value) {
        this.setValue(FIELD_ASSOCIATED_POOLS, value);
    }

    public void setAssociatedGames(long value) {
        this.setValue(FIELD_ASSOCIATED_GAMES, value);
    }

    public void setGamesPlayed(long value) {
        this.setValue(FIELD_GAMES_PLAYED, value);
    }

    public void setRightAnswers(long value) {
        this.setValue(FIELD_RIGHT_ANSWERS, value);
    }

    public void setWrongAnswers(long value) {
        this.setValue(FIELD_WRONG_ANSWERS, value);
    }

    public void setAverageAnswerSpeed(long value) {
        this.setValue(FIELD_AVERAGE_ANSWER_SPEED, value);
    }

    public void setRating(long value) {
        this.setValue(FIELD_RATING, value);
    }
}
