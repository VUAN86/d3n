package de.ascendro.f4m.spark.analytics.model;

import static de.ascendro.f4m.server.analytics.model.InvoiceEvent.CURRENCY;
import static de.ascendro.f4m.server.analytics.model.InvoiceEvent.CURRENCY_TO;
import static de.ascendro.f4m.server.analytics.model.InvoiceEvent.EXCHANGE_RATE;
import static de.ascendro.f4m.server.analytics.model.InvoiceEvent.GAME_TYPE;
import static de.ascendro.f4m.server.analytics.model.InvoiceEvent.PAYMENT_AMOUNT;
import static de.ascendro.f4m.server.analytics.model.InvoiceEvent.PAYMENT_AMOUNT_TO;
import static de.ascendro.f4m.server.analytics.model.InvoiceEvent.PAYMENT_TYPE;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;

public class InvoiceMessage extends BaseEventMessage {

    private static final String TABLE_NAME_INVOICE = "invoice";

	public InvoiceMessage(String format, String path) {
        super(format, path, "de.ascendro.f4m.server.analytics.model.InvoiceEvent", TABLE_NAME_INVOICE);
    }

    @Override
    protected void initColumns() {
        addSelectColumn(PAYMENT_TYPE);
        addSelectColumn(PAYMENT_AMOUNT);
        addSelectColumn(GAME_TYPE);
        addSelectColumn(CURRENCY);
        addSelectColumn(CURRENCY_TO);
        addSelectColumn(EXCHANGE_RATE);
        addSelectColumn(PAYMENT_AMOUNT_TO);
    }

    @Override
    protected StructField[] getEventFields() {
        return new StructField[]{
                DataTypes.createStructField(PAYMENT_TYPE, DataTypes.StringType, true),
                DataTypes.createStructField(PAYMENT_AMOUNT, DataTypes.DoubleType, true),
                DataTypes.createStructField(GAME_TYPE, DataTypes.StringType, true),
                DataTypes.createStructField(CURRENCY, DataTypes.StringType, true),
                DataTypes.createStructField(CURRENCY_TO, DataTypes.StringType, true),
                DataTypes.createStructField(EXCHANGE_RATE, DataTypes.DoubleType, true),
                DataTypes.createStructField(PAYMENT_AMOUNT_TO, DataTypes.DoubleType, true)
        };
    }
}

