package de.ascendro.f4m.spark.analytics.model;

import static de.ascendro.f4m.server.analytics.model.VoucherUsedEvent.VOUCHER_ID_PROPERTY;
import static de.ascendro.f4m.server.analytics.model.VoucherUsedEvent.VOUCHER_INSTANCE_PROPERTY;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;

public class VoucherUsedMessage extends BaseEventMessage {

    private static final String TABLE_NAME_VOUCHER_USED = "voucher_used";

	public VoucherUsedMessage(String format, String path) {
        super(format, path, "de.ascendro.f4m.server.analytics.model.VoucherUsedEvent", TABLE_NAME_VOUCHER_USED);
    }

    @Override
    protected void initColumns() {
        addSelectColumn(VOUCHER_ID_PROPERTY);
        addSelectColumn(VOUCHER_INSTANCE_PROPERTY);
    }

    @Override
    protected StructField[] getEventFields() {
        return new StructField[]{
                DataTypes.createStructField(VOUCHER_ID_PROPERTY, DataTypes.LongType, true),
                DataTypes.createStructField(VOUCHER_INSTANCE_PROPERTY, DataTypes.StringType, true)
        };
    }
}
