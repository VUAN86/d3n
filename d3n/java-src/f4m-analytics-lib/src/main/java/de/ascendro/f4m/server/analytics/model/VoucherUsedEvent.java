package de.ascendro.f4m.server.analytics.model;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.model.base.BaseEvent;

public class VoucherUsedEvent extends BaseEvent {
    public static final String VOUCHER_ID_PROPERTY = "voucherId";
    public static final String VOUCHER_INSTANCE_PROPERTY = "voucherInstanceId";

    public VoucherUsedEvent() {
        //default constructor
    }

    public VoucherUsedEvent(JsonObject baseGameJsonObject) {
        super(baseGameJsonObject);
    }

    @Override
    public boolean isUserInfoRequired() {
        return false;
    }

    public void setVoucherId(Long voucherId) {
        setProperty(VOUCHER_ID_PROPERTY, voucherId);
    }

    public Long getVoucherId() {
        return getPropertyAsLong(VOUCHER_ID_PROPERTY);
    }

    public void setVoucherInstanceId(String voucherInstanceId) {
        setProperty(VOUCHER_INSTANCE_PROPERTY, voucherInstanceId);
    }

    public String getVoucherInstanceId() {
        return getPropertyAsString(VOUCHER_INSTANCE_PROPERTY);
    }
}
