package de.ascendro.f4m.service.analytics.module.statistic.model;

import java.math.BigDecimal;

import de.ascendro.f4m.service.analytics.module.statistic.model.base.BaseStatisticTable;
import de.ascendro.f4m.service.analytics.module.statistic.model.base.Order;

public class Game extends BaseStatisticTable {
    @SuppressWarnings("unused")
    private static final String TABLE_NAME = "game";
    private static final String KEY_FIELD = "id";

    private static final String FIELD_PLAYERS = "stat_players";
    private static final String FIELD_GAMES_PLAYED = "stat_gamesPlayed";
    private static final String FIELD_ADS_VIEWED = "stat_adsViewed";
    private static final String FIELD_CREDITS_PAYED = "stat_creditsPayed";
    private static final String FIELD_FIELD_MONEY_PAYED = "stat_moneyPayed";
    private static final String FIELD_BONUS_POINTS_PAID = "stat_bonusPointsPaid";
    private static final String FIELD_VOUCHER_WON = "stat_voucherWon";
    private static final String FIELD_BONUS_POINTS_WON = "stat_bonusPointsWon";
    private static final String FIELD_CREDITS_WON = "stat_creditsWon";
    private static final String FIELD_MONEY_WON = "stat_moneyWon";
    @Order(value = 1)
    private static final String FIELD_QUESTIONS_ANSWERED = "stat_questionsAnswered";
    @Order(value = 2)
    private static final String FIELD_QUESTIONS_ANSWERED_RIGHT = "stat_questionsAnsweredRight";
    @Order(value = 3)
    private static final String FIELD_QUESTIONS_ANSWERED_WRONG = "stat_questionsAnsweredWrong";
    @Order(value = 4)
    private static final String FIELD_AVERAGE_ANSWER_SPEED = "stat_averageAnswerSpeed";

    static {
        getCustomFieldList(Game.class).put(FIELD_AVERAGE_ANSWER_SPEED,
                "(" + getOldValueField(FIELD_QUESTIONS_ANSWERED) + "*" + FIELD_AVERAGE_ANSWER_SPEED +  "+"
                + " ? * (" + FIELD_QUESTIONS_ANSWERED + " - " + getOldValueField(FIELD_QUESTIONS_ANSWERED) + ")) / "
                + FIELD_QUESTIONS_ANSWERED
        );
    }

    public void setId(long value) {
        this.setValue(KEY_FIELD, value);
    }

    public void setPlayers(long value) {
        this.setValue(FIELD_PLAYERS, value);
    }

    public void setGamesPlayed(long value) {
        this.setValue(FIELD_GAMES_PLAYED, value);
    }

    public void setAdsViewed(long value) {
        this.setValue(FIELD_ADS_VIEWED, value);
    }

    public void setCreditsPayed(long value) {
        this.setValue(FIELD_CREDITS_PAYED, value);
    }

    public void setMoneyPayed(BigDecimal value) {
        this.setValue(FIELD_FIELD_MONEY_PAYED, value);
    }

    public void setBonusPointsPaid(long value) {
        this.setValue(FIELD_BONUS_POINTS_PAID, value);
    }

    public void setVoucherWon(long value) {
        this.setValue(FIELD_VOUCHER_WON, value);
    }

    public void setBonusPointsWon(long value) {
        this.setValue(FIELD_BONUS_POINTS_WON, value);
    }

    public void setCreditsWon(long value) {
        this.setValue(FIELD_CREDITS_WON, value);
    }

    public void setMoneyWon(BigDecimal value) {
        this.setValue(FIELD_MONEY_WON, value);
    }

    public void setQuestionsAnswered(long value) {
        this.setValue(FIELD_QUESTIONS_ANSWERED, value);
    }

    public void setQuestionsAnsweredRight(long value) {
        this.setValue(FIELD_QUESTIONS_ANSWERED_RIGHT, value);
    }

    public void setQuestionsAnsweredWrong(long value) {
        this.setValue(FIELD_QUESTIONS_ANSWERED_WRONG, value);
    }

    public void setAverageAnswerSpeed(long value) {
        this.setValue(FIELD_AVERAGE_ANSWER_SPEED, value);
    }

    @Override
    public String toString() {
        return "Game{" +
                "valuesMap=" + valuesMap +
                "} " + super.toString();
    }
}

