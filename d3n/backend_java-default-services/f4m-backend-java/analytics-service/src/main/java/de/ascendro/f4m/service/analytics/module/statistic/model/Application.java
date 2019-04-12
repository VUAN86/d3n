package de.ascendro.f4m.service.analytics.module.statistic.model;


import java.math.BigDecimal;

import de.ascendro.f4m.service.analytics.module.statistic.model.base.BaseStatisticTable;

public class Application extends BaseStatisticTable {
    @SuppressWarnings("unused")
    private static final String TABLE_NAME = "application";
    private static final String KEY_FIELD = "id";

    private static final String FIELD_GAME_PLAYED = "stat_gamesPlayed";
    private static final String FIELD_PLAYERS = "stat_players";
    private static final String FIELD_FRIENDS_INVITED = "stat_friendsInvited";
    private static final String FIELD_MONEY_CHARGED = "stat_moneyCharged";
    private static final String FIELD_CREDITS_PURCHASED = "stat_creditsPurchased";
    private static final String FIELD_MONEY_WON = "stat_moneyWon";
    private static final String FIELD_BONUS_POINTS_WON = "stat_bonusPointsWon";
    private static final String FIELD_CREDITS_WON = "stat_creditsWon";
    private static final String FIELD_VOUCHER_WON = "stat_voucherWon";
    private static final String FIELD_ADS_VIEWED = "stat_adsViewed";


    public void setId(long value) {
        this.setValue(KEY_FIELD, value);
    }

    public void setGamePlayed(long value) {
        this.setValue(FIELD_GAME_PLAYED, value);
    }

    public void setPlayers(long value) {
        this.setValue(FIELD_PLAYERS, value);
    }

    public void setFriendsInvited(long value) {
        this.setValue(FIELD_FRIENDS_INVITED, value);
    }

    public void setMoneyCharged(BigDecimal value) {
        this.setValue(FIELD_MONEY_CHARGED, value);
    }

    public void setCreditsPurchased(long value) {
        this.setValue(FIELD_CREDITS_PURCHASED, value);
    }

    public void setMoneyWon(BigDecimal value) {
        this.setValue(FIELD_MONEY_WON, value);
    }

    public void setBonusPointsWon(long value) {
        this.setValue(FIELD_BONUS_POINTS_WON, value);
    }

    public void setCreditsWon(long value) {
        this.setValue(FIELD_CREDITS_WON, value);
    }

    public void setVoucherWon(long value) {
        this.setValue(FIELD_VOUCHER_WON, value);
    }

    public void setAdsViewed(long value) {
        this.setValue(FIELD_ADS_VIEWED, value);
    }

    @Override
    public String toString() {
        return "Application{" +
                "valuesMap=" + valuesMap +
                "} " + super.toString();
    }
}
