package de.ascendro.f4m.spark.analytics.model;

import static de.ascendro.f4m.server.analytics.model.GameEndEvent.GAME_ID_PROPERTY;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;


public class GameEndMessage extends BaseEventMessage {

    private static final String TABLE_NAME_GAME_END = "game_end";

	public GameEndMessage(String format, String path) {
        super(format, path, "de.ascendro.f4m.server.analytics.model.GameEndEvent", TABLE_NAME_GAME_END);
    }

    @Override
    protected void initColumns() {
        addSelectColumn(GAME_ID_PROPERTY);
    }

    @Override
    protected StructField[] getEventFields() {
        return new StructField[]{
                DataTypes.createStructField(GAME_ID_PROPERTY, DataTypes.LongType, true)
        };
    }
}
