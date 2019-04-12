package de.ascendro.f4m.service.voucher.model.uservoucher;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserVoucherListByVoucherRequest extends FilterCriteria implements JsonMessageContent {

    /** Maximum allowed requested list limit. */
    public static final int MAX_LIST_LIMIT = 100;

    private String voucherId;
    private String type;

    public String getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(String voucherId) {
        this.voucherId = voucherId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
