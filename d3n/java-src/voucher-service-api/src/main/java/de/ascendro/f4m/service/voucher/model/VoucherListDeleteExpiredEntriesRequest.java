package de.ascendro.f4m.service.voucher.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class VoucherListDeleteExpiredEntriesRequest implements JsonMessageContent {

    private String tenantId;

    public VoucherListDeleteExpiredEntriesRequest() {
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName() + " [");
        builder.append("tenantId=").append(tenantId);
        builder.append("]");
        return builder.toString();
    }

}