package de.ascendro.f4m.service.voucher.model.uservoucher;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserVoucherListByUserRequest extends FilterCriteria implements JsonMessageContent {

    /** Maximum allowed requested list limit. */
    public static final int MAX_LIST_LIMIT = 100;

    private String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
