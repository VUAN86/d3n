package de.ascendro.f4m.spark.analytics.model;

import static de.ascendro.f4m.server.analytics.model.TombolaEndEvent.TOMBOLA_ID;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;

public class TombolaEndMessage extends BaseEventMessage {

    private static final String TABLE_NAME_TOMBOLA_END = "tombola_end";

	public TombolaEndMessage(String format, String path) {
        super(format, path, "de.ascendro.f4m.server.analytics.model.TombolaEndEvent", TABLE_NAME_TOMBOLA_END);
    }

    @Override
    protected void initColumns() {
        addSelectColumn(TOMBOLA_ID);
    }

    @Override
    protected StructField[] getEventFields() {
        return new StructField[]{
                DataTypes.createStructField(TOMBOLA_ID, DataTypes.LongType, true)
        };
    }
}
