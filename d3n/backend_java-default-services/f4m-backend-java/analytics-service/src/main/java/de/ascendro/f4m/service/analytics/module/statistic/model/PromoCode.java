package de.ascendro.f4m.service.analytics.module.statistic.model;

import java.math.BigDecimal;

import de.ascendro.f4m.service.analytics.module.statistic.model.base.BaseStatisticTable;

public class PromoCode extends BaseStatisticTable {
    @SuppressWarnings("unused")
    private static final String TABLE_NAME = "promocode_campaign";
    private static final String KEY_FIELD = "id";

    private static final String FIELD_USED = "stat_used";
    private static final String FIELD_CREDITS_PAID = "stat_creditsPaid";
    private static final String FIELD_BONUS_POINTS_PAID = "stat_bonusPointsPaid";
    private static final String FIELD_MONEY_PAID = "stat_moneyPaid";

    public void setId(long value) {
        this.setValue(KEY_FIELD, value);
    }

    public void setPromocodesUsed(long value) {
        this.setValue(FIELD_USED, value);
    }

    public void setBonusPointsPaid(long value) {
        this.setValue(FIELD_BONUS_POINTS_PAID, value);
    }

    public void setCreditsPaid(long value) {
        this.setValue(FIELD_CREDITS_PAID, value);
    }

    public void setMoneyPayed(BigDecimal value) {
        this.setValue(FIELD_MONEY_PAID, value);
    }
}
