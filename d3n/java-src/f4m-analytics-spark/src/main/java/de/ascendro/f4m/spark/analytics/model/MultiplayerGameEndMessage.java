package de.ascendro.f4m.spark.analytics.model;

import static de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent.GAME_DRAW;
import static de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent.GAME_INSTANCE_ID;
import static de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent.GAME_LOST;
import static de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent.GAME_WON;
import static de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent.LOST_DUEL_GAME;
import static de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent.LOST_TOURNAMENT_GAME;
import static de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent.OPPONENT_ID;
import static de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent.PLACEMENT;
import static de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent.WON_DUEL_GAME;
import static de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent.WON_TOURNAMENT_GAME;
import static de.ascendro.f4m.server.analytics.model.base.GameBaseEvent.GAME_ID_PROPERTY;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;

public class MultiplayerGameEndMessage extends BaseEventMessage {

    private static final String TABLE_NAME_MULTIPLAYER_GAME_END = "multiplayer_game_end";

	public MultiplayerGameEndMessage(String format, String path) {
        super(format, path, "de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent", TABLE_NAME_MULTIPLAYER_GAME_END);
    }

    @Override
    protected void initColumns() {
        addSelectColumn(GAME_ID_PROPERTY);
        addSelectColumn(PLACEMENT);
        addSelectColumn(GAME_INSTANCE_ID);
        addSelectColumn(OPPONENT_ID);
        addSelectColumn(GAME_WON);
        addSelectColumn(GAME_LOST);
        addSelectColumn(GAME_DRAW);
        addSelectColumn(WON_DUEL_GAME);
        addSelectColumn(LOST_DUEL_GAME);
        addSelectColumn(WON_TOURNAMENT_GAME);
        addSelectColumn(LOST_TOURNAMENT_GAME);
    }

    @Override
    protected StructField[] getEventFields() {
        return new StructField[]{
                DataTypes.createStructField(GAME_ID_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(PLACEMENT, DataTypes.LongType, true),
                DataTypes.createStructField(GAME_INSTANCE_ID, DataTypes.StringType, true),
                DataTypes.createStructField(OPPONENT_ID, DataTypes.StringType, true),
                DataTypes.createStructField(GAME_WON, DataTypes.BooleanType, true),
                DataTypes.createStructField(GAME_LOST, DataTypes.BooleanType, true),
                DataTypes.createStructField(GAME_DRAW, DataTypes.BooleanType, true),
                DataTypes.createStructField(WON_DUEL_GAME, DataTypes.BooleanType, true),
                DataTypes.createStructField(LOST_DUEL_GAME, DataTypes.BooleanType, true),
                DataTypes.createStructField(WON_TOURNAMENT_GAME, DataTypes.BooleanType, true),
                DataTypes.createStructField(LOST_TOURNAMENT_GAME, DataTypes.BooleanType, true)
        };
    }


}
