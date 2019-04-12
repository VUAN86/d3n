package de.ascendro.f4m.spark.analytics.model;

import static de.ascendro.f4m.server.analytics.model.VoucherCountEvent.VOUCHER_COUNT;
import static de.ascendro.f4m.server.analytics.model.VoucherCountEvent.VOUCHER_ID_PROPERTY;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;

public class VoucherCountMessage extends BaseEventMessage {

    private static final String TABLE_NAME_VOUCHER_COUNT = "voucher_count";

	public VoucherCountMessage(String format, String path) {
        super(format, path, "de.ascendro.f4m.server.analytics.model.VoucherCountEvent", TABLE_NAME_VOUCHER_COUNT);
    }

    @Override
    protected void initColumns() {
        addSelectColumn(VOUCHER_ID_PROPERTY);
        addSelectColumn(VOUCHER_COUNT);
    }

    @Override
    protected StructField[] getEventFields() {
        return new StructField[]{
                DataTypes.createStructField(VOUCHER_ID_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(VOUCHER_COUNT, DataTypes.LongType, true)
        };
    }
}
