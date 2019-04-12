package de.ascendro.f4m.spark.analytics.model;

import static de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent.AVERAGE_ANSWER_SPEED_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent.FREE_COMPONENT_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent.HANDICAP;
import static de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent.NEW_APP_PLAYER;
import static de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent.NEW_GAME_PLAYER;
import static de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent.PAID_COMPONENT_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent.PAID_COMPONENT_SKIPPED_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent.PLAYED_WITH_FRIEND_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent.PLAYED_WITH_PUBLIC_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent.QUESTIONS_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent.SKIPPED_QUESTIONS_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent.TOTAL_CORRECT_QUESTIONS_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent.TOTAL_INCORRECT_QUESTIONS_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent.TOTAL_QUESTIONS_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.Question.ANSWER_CORRECT_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.Question.ANSWER_SPEED_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.Question.QUESTION_ID_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.base.GameBaseEvent.GAME_ID_PROPERTY;

import org.apache.spark.sql.types.ArrayType;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;

public class PlayerGameEndMessage extends BaseEventMessage {

    private static final String TABLE_NAME_PLAYER_GAME_END = "player_game_end";

	public PlayerGameEndMessage(String format, String path) {
        super(format, path, "de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent", TABLE_NAME_PLAYER_GAME_END);
    }

    @Override
    protected void initColumns() {
        addSelectColumn(GAME_ID_PROPERTY);
        addSelectColumn(TOTAL_QUESTIONS_PROPERTY);
        addSelectColumn(SKIPPED_QUESTIONS_PROPERTY);
        addSelectColumn(TOTAL_CORRECT_QUESTIONS_PROPERTY);
        addSelectColumn(TOTAL_INCORRECT_QUESTIONS_PROPERTY);
        addSelectColumn(PLAYED_WITH_FRIEND_PROPERTY);
        addSelectColumn(PLAYED_WITH_PUBLIC_PROPERTY);
        addSelectColumn(PAID_COMPONENT_PROPERTY);
        addSelectColumn(FREE_COMPONENT_PROPERTY);
        addSelectColumn(PAID_COMPONENT_SKIPPED_PROPERTY);
        addSelectColumn(AVERAGE_ANSWER_SPEED_PROPERTY);
        addSelectColumn(QUESTIONS_PROPERTY);
        addSelectColumn(NEW_GAME_PLAYER);
        addSelectColumn(NEW_APP_PLAYER);
        addSelectColumn(HANDICAP);
    }

    @Override
    protected StructField[] getEventFields() {
        ArrayType questionDataType =  DataTypes.createArrayType(DataType.fromJson(DataTypes.createStructType(new StructField[]{
                DataTypes.createStructField(QUESTION_ID_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(ANSWER_CORRECT_PROPERTY, DataTypes.BooleanType, true),
                DataTypes.createStructField(ANSWER_SPEED_PROPERTY, DataTypes.LongType, true),
        }).json()));

        return new StructField[]{
                DataTypes.createStructField(GAME_ID_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(TOTAL_QUESTIONS_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(SKIPPED_QUESTIONS_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(TOTAL_CORRECT_QUESTIONS_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(TOTAL_INCORRECT_QUESTIONS_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(PLAYED_WITH_FRIEND_PROPERTY, DataTypes.BooleanType, true),
                DataTypes.createStructField(PLAYED_WITH_PUBLIC_PROPERTY, DataTypes.BooleanType, true),
                DataTypes.createStructField(PAID_COMPONENT_PROPERTY, DataTypes.BooleanType, true),
                DataTypes.createStructField(FREE_COMPONENT_PROPERTY, DataTypes.BooleanType, true),
                DataTypes.createStructField(PAID_COMPONENT_SKIPPED_PROPERTY, DataTypes.BooleanType, true),
                DataTypes.createStructField(AVERAGE_ANSWER_SPEED_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(QUESTIONS_PROPERTY, questionDataType, true),
                DataTypes.createStructField(NEW_GAME_PLAYER, DataTypes.BooleanType, true),
                DataTypes.createStructField(NEW_APP_PLAYER, DataTypes.BooleanType, true),
                DataTypes.createStructField(HANDICAP, DataTypes.DoubleType, true)
        };
    }
}
