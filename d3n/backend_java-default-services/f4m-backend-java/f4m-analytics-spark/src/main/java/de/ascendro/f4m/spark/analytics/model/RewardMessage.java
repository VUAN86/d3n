package de.ascendro.f4m.spark.analytics.model;

import static de.ascendro.f4m.server.analytics.model.RewardEvent.BONUS_POINTS_WON_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.RewardEvent.CREDIT_WON_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.RewardEvent.FREE_WINNING_COMPONENTS_PLAYED;
import static de.ascendro.f4m.server.analytics.model.RewardEvent.GAME_INSTANCE_ID;
import static de.ascendro.f4m.server.analytics.model.RewardEvent.MONEY_WON_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.RewardEvent.PAID_WINNING_COMPONENTS_PLAYED;
import static de.ascendro.f4m.server.analytics.model.RewardEvent.SUPER_PRIZE_WON_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.RewardEvent.TOMBOLA_ID_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.RewardEvent.VOUCHER_ID_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.RewardEvent.VOUCHER_WON_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.base.GameBaseEvent.GAME_ID_PROPERTY;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;

public class RewardMessage extends BaseEventMessage {

    private static final String TABLE_NAME_REWARD = "reward";

	public RewardMessage(String format, String path) {
        super(format, path, "de.ascendro.f4m.server.analytics.model.RewardEvent", TABLE_NAME_REWARD);
    }

    @Override
    protected void initColumns() {
        addSelectColumn(GAME_ID_PROPERTY);
        addSelectColumn(VOUCHER_ID_PROPERTY);
        addSelectColumn(VOUCHER_WON_PROPERTY);
        addSelectColumn(SUPER_PRIZE_WON_PROPERTY);
        addSelectColumn(BONUS_POINTS_WON_PROPERTY);
        addSelectColumn(CREDIT_WON_PROPERTY);
        addSelectColumn(MONEY_WON_PROPERTY);
        addSelectColumn(PAID_WINNING_COMPONENTS_PLAYED);
        addSelectColumn(FREE_WINNING_COMPONENTS_PLAYED);
        addSelectColumn(TOMBOLA_ID_PROPERTY);
        addSelectColumn(GAME_INSTANCE_ID);
    }

    @Override
    protected StructField[] getEventFields() {
        return new StructField[]{
                DataTypes.createStructField(GAME_ID_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(VOUCHER_ID_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(VOUCHER_WON_PROPERTY, DataTypes.BooleanType, true),
                DataTypes.createStructField(SUPER_PRIZE_WON_PROPERTY, DataTypes.BooleanType, true),
                DataTypes.createStructField(BONUS_POINTS_WON_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(CREDIT_WON_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(MONEY_WON_PROPERTY, DataTypes.DoubleType, true),
                DataTypes.createStructField(PAID_WINNING_COMPONENTS_PLAYED, DataTypes.BooleanType, true),
                DataTypes.createStructField(FREE_WINNING_COMPONENTS_PLAYED, DataTypes.BooleanType, true),
                DataTypes.createStructField(TOMBOLA_ID_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(GAME_INSTANCE_ID, DataTypes.StringType, true)
        };
    }

}
