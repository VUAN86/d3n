package de.ascendro.f4m.service.analytics.module.statistic.model;

import de.ascendro.f4m.service.analytics.module.statistic.model.base.BaseStatisticTable;

public class Advertisement extends BaseStatisticTable {
    @SuppressWarnings("unused")
	private static final String TABLE_NAME = "advertisement";
    private static final String KEY_FIELD = "id";

    private static final String FIELD_APP_USED = "stat_appsUsed";
    private static final String FIELD_GAME_USED = "stat_gamesUsed";
    private static final String FIELD_AD_VIEWS = "stat_views";
    private static final String FIELD_EARNED_CREDITS = "stat_earnedCredits";


    public void setId(long value) {
        this.setValue(KEY_FIELD, value);
    }

    public void setAppUsed(long value) {
        this.setValue(FIELD_APP_USED, value);
    }

    public void setGameUsed(long value) {
        this.setValue(FIELD_GAME_USED, value);
    }

    public void setAdViews(long value) {
        this.setValue(FIELD_AD_VIEWS, value);
    }

    public void setEarnedCredits(long value) {
        this.setValue(FIELD_EARNED_CREDITS, value);
    }
}
