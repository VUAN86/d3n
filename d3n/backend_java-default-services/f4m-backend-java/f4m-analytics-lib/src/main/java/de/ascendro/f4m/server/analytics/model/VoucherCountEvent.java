package de.ascendro.f4m.server.analytics.model;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.model.base.BaseEvent;

public class VoucherCountEvent  extends BaseEvent {
    public static final String VOUCHER_ID_PROPERTY = "voucherId";
    public static final String VOUCHER_COUNT = "voucherCount";

    public VoucherCountEvent() {
        //default constructor
    }

    public VoucherCountEvent(JsonObject baseGameJsonObject) {
        super(baseGameJsonObject);
    }

    public void setVoucherId(Long voucherId) {
        setProperty(VOUCHER_ID_PROPERTY, voucherId);
    }

    public Long getVoucherId() {
        return getPropertyAsLong(VOUCHER_ID_PROPERTY);
    }

    public void setVoucherCount(Long voucherCount) {
        setProperty(VOUCHER_COUNT, voucherCount);
    }

    public Long getVoucherCount() {
        return getPropertyAsLong(VOUCHER_COUNT);
    }

    @Override
    public String toString() {
        return "VoucherCountEvent{" +
                "jsonObject=" + jsonObject +
                "} " + super.toString();
    }
}
