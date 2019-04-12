package de.ascendro.f4m.spark.analytics.model;

import static de.ascendro.f4m.server.analytics.model.AdEvent.AD_KEY_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.AdEvent.FIRST_APP_USAGE;
import static de.ascendro.f4m.server.analytics.model.AdEvent.FIRST_GAME_USAGE;
import static de.ascendro.f4m.server.analytics.model.base.GameBaseEvent.GAME_ID_PROPERTY;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;

public class AdMessage extends BaseEventMessage {

    private static final String TABLE_NAME_ADVERTISEMENT = "advertisement";

	public AdMessage(String format, String path) {
        super(format, path, "de.ascendro.f4m.server.analytics.model.AdEvent", TABLE_NAME_ADVERTISEMENT);
    }

    @Override
    protected void initColumns() {
        addSelectColumn(GAME_ID_PROPERTY);
        addSelectColumn(AD_KEY_PROPERTY);
        addSelectColumn(FIRST_APP_USAGE);
        addSelectColumn(FIRST_GAME_USAGE);
    }

    @Override
    protected StructField[] getEventFields() {
        return new StructField[]{
                DataTypes.createStructField(GAME_ID_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(AD_KEY_PROPERTY, DataTypes.StringType, true),
                DataTypes.createStructField(FIRST_APP_USAGE, DataTypes.BooleanType, true),
                DataTypes.createStructField(FIRST_GAME_USAGE, DataTypes.BooleanType, true)
        };
    }
}
