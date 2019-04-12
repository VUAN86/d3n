package de.ascendro.f4m.spark.analytics.model;

import static de.ascendro.f4m.server.analytics.model.PaymentEvent.BONUS_POINTS_GIVEN_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PaymentEvent.BONUS_POINTS_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PaymentEvent.CREDIT_GIVEN_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PaymentEvent.CREDIT_PAID_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PaymentEvent.CREDIT_PURCHASED_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PaymentEvent.MONEY_CHARGED_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PaymentEvent.MONEY_GIVEN_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PaymentEvent.MONEY_PAID_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.PaymentEvent.PAYMENT_AMOUNT;
import static de.ascendro.f4m.server.analytics.model.PaymentEvent.PAYMENT_DETAIL_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.base.GameBaseEvent.GAME_ID_PROPERTY;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;

public class PaymentMessage extends BaseEventMessage {

    private static final String TABLE_NAME_PAYMENT = "payment";

	public PaymentMessage(String format, String path) {
        super(format, path, "de.ascendro.f4m.server.analytics.model.PaymentEvent", TABLE_NAME_PAYMENT);
    }

    @Override
    protected void initColumns() {
        addSelectColumn(GAME_ID_PROPERTY);

        addSelectColumn(CREDIT_PAID_PROPERTY);
        addSelectColumn(MONEY_PAID_PROPERTY);
        addSelectColumn(BONUS_POINTS_PROPERTY);
        addSelectColumn(CREDIT_PURCHASED_PROPERTY);
        addSelectColumn(MONEY_CHARGED_PROPERTY);
        addSelectColumn(CREDIT_GIVEN_PROPERTY);
        addSelectColumn(MONEY_GIVEN_PROPERTY);
        addSelectColumn(BONUS_POINTS_GIVEN_PROPERTY);

        addSelectColumn(PAYMENT_DETAIL_PROPERTY);
        addSelectColumn(PAYMENT_AMOUNT);
    }

    @Override
    protected StructField[] getEventFields() {
        return new StructField[]{
                DataTypes.createStructField(GAME_ID_PROPERTY, DataTypes.LongType, true),

                DataTypes.createStructField(CREDIT_PAID_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(MONEY_PAID_PROPERTY, DataTypes.DoubleType, true),
                DataTypes.createStructField(BONUS_POINTS_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(CREDIT_PURCHASED_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(MONEY_CHARGED_PROPERTY, DataTypes.DoubleType, true),
                DataTypes.createStructField(CREDIT_GIVEN_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(MONEY_GIVEN_PROPERTY, DataTypes.DoubleType, true),
                DataTypes.createStructField(BONUS_POINTS_GIVEN_PROPERTY, DataTypes.LongType, true),

                DataTypes.createStructField(PAYMENT_DETAIL_PROPERTY, DataTypes.StringType, true),
                DataTypes.createStructField(PAYMENT_AMOUNT, DataTypes.DoubleType, true)
        };
    }
}
