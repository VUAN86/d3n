package de.ascendro.f4m.service.analytics.module.statistic.model;


import java.math.BigDecimal;

import de.ascendro.f4m.service.analytics.module.statistic.model.base.BaseStatisticTable;
import de.ascendro.f4m.service.analytics.module.statistic.model.base.Order;

public class ProfileStats extends BaseStatisticTable {
    @SuppressWarnings("unused")
    private static final String TABLE_NAME = "profile_has_stats";
    private static final String KEY_FIELD_1 = "profileId";
    private static final String KEY_FIELD_2 = "tenantId";

    private static final String FIELD_HANDICAP = "stat_handicap";
    private static final String FIELD_FRIENDS_INVITED = "stat_friendsInvited";
    private static final String FIELD_FRIENDS_BLOCKED = "stat_friendsBlocked";
    private static final String FIELD_SUBTRACT_FRIENDS_BLOCKED = "stat_friendsBlocked";
    private static final String FIELD_GAMES_PLAYED = "stat_gamesPlayed";
    private static final String FIELD_GAMES_INVITED_FROM_FRIENDS = "stat_gamesInvitedFromFriends";
    private static final String FIELD_GAMES_FRIENDS_INVITED_TOO = "stat_gamesFriendsInvitedToo";
    private static final String FIELD_GAMES_PLAYED_WITH_FRIENDS = "stat_gamesPlayedWithFriends";
    private static final String FIELD_GAMES_PLAYED_WITH_PUBLIC = "stat_gamesPlayedWithPublic";
    private static final String FIELD_GAMES_WON = "stat_gamesWon";
    private static final String FIELD_GAMES_LOST = "stat_gamesLost";
    private static final String FIELD_GAMES_DRAWN = "stat_gamesDrawn";
    @Order(value = 1)
    private static final String FIELD_RIGHT_ANSWERS = "stat_rightAnswers";
    @Order(value = 2)
    private static final String FIELD_WRONG_ANSWERS = "stat_wrongAnswers";
    @Order(value = 3)
    private static final String FIELD_AVERAGE_ANSWER_SPEED = "stat_averageAnswerSpeed";
    private static final String FIELD_SKIPPED_QUESTIONS = "stat_skippedQuestions";
    private static final String FIELD_ADS_VIEWED = "stat_adsViewed";
    private static final String FIELD_PAID_WINNING_COMPONENTS_PLAYED = "stat_paidWinningComponentsPlayed";
    private static final String FIELD_FREE_WINNING_COMPONENTS_PLAYED = "stat_freeWinningComponentsPlayed";
    private static final String FIELD_SKIPPED_WINNING_COMPONENTS = "stat_skippedWinningComponents";
    private static final String FIELD_VOUCHER_WON = "stat_voucherWon";
    private static final String FIELD_SUPER_PRIZES_WON = "stat_superPrizesWon";
    private static final String FIELD_BONUS_POINTS_WON = "stat_bonusPointsWon";
    private static final String FIELD_CREDITS_WON = "stat_creditsWon";
    private static final String FIELD_MONEY_PLAYED = "stat_moneyPlayed";
    private static final String FIELD_MONEY_WON = "stat_moneyWon";
    private static final String FIELD_TOTAL_CREDITS_PURCHASED = "stat_totalCreditsPurchased";
    private static final String FIELD_TOTAL_MONEY_CHARGED = "stat_totalMoneyCharged";

    static {
        getCustomFieldList(Profile.class).put(FIELD_AVERAGE_ANSWER_SPEED,
                "((" + getOldValueField(FIELD_RIGHT_ANSWERS) + " + " + getOldValueField(FIELD_WRONG_ANSWERS) + ")"
                        + "*" + FIELD_AVERAGE_ANSWER_SPEED +  "+"
                        + " ? * (" + FIELD_RIGHT_ANSWERS + " + " + FIELD_WRONG_ANSWERS + " - "
                        + getOldValueField(FIELD_RIGHT_ANSWERS) + " + " + getOldValueField(FIELD_WRONG_ANSWERS)  + ")) / "
                        + "(" + FIELD_RIGHT_ANSWERS + " + " + FIELD_WRONG_ANSWERS + ")"
        );

        getCustomFieldList(Profile.class).put(FIELD_HANDICAP, "?");
    }


    public void setUserId(String value) {
        this.setValue(KEY_FIELD_1, value);
    }

    public void setTenantId(String value) {
        this.setValue(KEY_FIELD_2, value);
    }

    public void setHandicap(double value) {
        this.setValue(FIELD_HANDICAP, value);
    }

    public void setFriendsInvited(long value) {
        this.setValue(FIELD_FRIENDS_INVITED, value);
    }

    public void setFriendsBlocked(long value) {
        this.setValue(FIELD_FRIENDS_BLOCKED, value);
    }

    public void setFriendsUnblocked(long value) {
        this.setValue(FIELD_SUBTRACT_FRIENDS_BLOCKED, value);
    }

    public void setGamesPlayed(long value) {
        this.setValue(FIELD_GAMES_PLAYED, value);
    }

    public void setGamesInvitedFromFriends(long value) {
        this.setValue(FIELD_GAMES_INVITED_FROM_FRIENDS, value);
    }

    public void setGamesFriendsInvitedToo(long value) {
        this.setValue(FIELD_GAMES_FRIENDS_INVITED_TOO, value);
    }

    public void setGamesPlayedWithFriends(long value) {
        this.setValue(FIELD_GAMES_PLAYED_WITH_FRIENDS, value);
    }

    public void setGamesPlayedWithPublic(long value) {
        this.setValue(FIELD_GAMES_PLAYED_WITH_PUBLIC, value);
    }

    public void setGamesWon(long value) {
        this.setValue(FIELD_GAMES_WON, value);
    }

    public void setGamesLost(long value) {
        this.setValue(FIELD_GAMES_LOST, value);
    }

    public void setGamesDrawn(long value) {
        this.setValue(FIELD_GAMES_DRAWN, value);
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

    public void setSkippedQuestions(long value) {
        this.setValue(FIELD_SKIPPED_QUESTIONS, value);
    }

    public void setAdsViewed(long value) {
        this.setValue(FIELD_ADS_VIEWED, value);
    }

    public void setPaidWinningComponentsPlayed(long value) {
        this.setValue(FIELD_PAID_WINNING_COMPONENTS_PLAYED, value);
    }

    public void setFreeWinningComponentsPlayed(long value) {
        this.setValue(FIELD_FREE_WINNING_COMPONENTS_PLAYED, value);
    }

    public void setSkippedWinningComponents(long value) {
        this.setValue(FIELD_SKIPPED_WINNING_COMPONENTS, value);
    }

    public void setVoucherWon(long value) {
        this.setValue(FIELD_VOUCHER_WON, value);
    }

    public void setSuperPrizesWon(long value) {
        this.setValue(FIELD_SUPER_PRIZES_WON, value);
    }

    public void setBonusPointsWon(long value) {
        this.setValue(FIELD_BONUS_POINTS_WON, value);
    }

    public void setCreditsWon(long value) {
        this.setValue(FIELD_CREDITS_WON, value);
    }

    public void setMoneyPlayed(BigDecimal value) {
        this.setValue(FIELD_MONEY_PLAYED, value);
    }

    public void setMoneyWon(BigDecimal value) {
        this.setValue(FIELD_MONEY_WON, value);
    }

    public void setTotalCreditsPurchased(long value) {
        this.setValue(FIELD_TOTAL_CREDITS_PURCHASED, value);
    }

    public void setTotalMoneyCharged(BigDecimal value) {
        this.setValue(FIELD_TOTAL_MONEY_CHARGED, value);
    }

    @Override
    public String toString() {
        return "ProfileStats{" +
                "valuesMap=" + valuesMap +
                "} " + super.toString();
    }
}
