package de.ascendro.f4m.service.analytics.module.statistic.model;

import de.ascendro.f4m.service.analytics.module.statistic.model.base.BaseStatisticTable;

public class Voucher extends BaseStatisticTable {
    @SuppressWarnings("unused")
    private static final String TABLE_NAME = "voucher";
    private static final String KEY_FIELD = "id";

    private static final String FIELD_WON = "stat_won";
    private static final String FIELD_ARCHIVED = "stat_archieved";
    private static final String FIELD_SPECIAL_PRIZES = "stat_specialPrizes";
    private static final String FIELD_INVENTORY_COUNT = "stat_inventoryCount";

    public void setId(long value) {
        this.setValue(KEY_FIELD, value);
    }

    public void setWon(long value) {
        this.setValue(FIELD_WON, value);
    }

    public void setArchived(long value) {
        this.setValue(FIELD_ARCHIVED, value);
    }

    public void setSpecialPrizes(long value) {
        this.setValue(FIELD_SPECIAL_PRIZES, value);
    }

    public void setInventoryCount(long value) {
        this.setValue(FIELD_INVENTORY_COUNT, value);
    }

    @Override
    public String toString() {
        return "Voucher{" +
                "valuesMap=" + valuesMap +
                "} " + super.toString();
    }
}
