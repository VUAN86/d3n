package de.ascendro.f4m.spark.analytics.model;


import static de.ascendro.f4m.server.analytics.model.PromoCodeEvent.BONUS_POINTS_PAID_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PromoCodeEvent.CREDITS_PAID_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PromoCodeEvent.MONEY_PAID_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PromoCodeEvent.PROMO_CODE;
import static de.ascendro.f4m.server.analytics.model.PromoCodeEvent.PROMO_CODE_CAMPAIGN_ID_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PromoCodeEvent.PROMO_CODE_USED_PROPERTY;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;


public class PromoCodeMessage extends BaseEventMessage {

    private static final String TABLE_NAME_PROMOCODE = "promocode";


	public PromoCodeMessage(String format, String path) {
        super(format, path, "de.ascendro.f4m.server.analytics.model.PromoCodeEvent", TABLE_NAME_PROMOCODE);
    }

    @Override
    protected void initColumns() {
        addSelectColumn(PROMO_CODE);
        addSelectColumn(PROMO_CODE_CAMPAIGN_ID_PROPERTY);
        addSelectColumn(PROMO_CODE_USED_PROPERTY);
        addSelectColumn(BONUS_POINTS_PAID_PROPERTY);
        addSelectColumn(CREDITS_PAID_PROPERTY);
        addSelectColumn(MONEY_PAID_PROPERTY);
    }


    @Override
    protected StructField[] getEventFields() {
        return new StructField[]{
                DataTypes.createStructField(PROMO_CODE, DataTypes.StringType, true),
                DataTypes.createStructField(PROMO_CODE_CAMPAIGN_ID_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(PROMO_CODE_USED_PROPERTY, DataTypes.BooleanType, true),
                DataTypes.createStructField(BONUS_POINTS_PAID_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(CREDITS_PAID_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(MONEY_PAID_PROPERTY, DataTypes.DoubleType, true)
        };
    }
}
