package de.ascendro.f4m.service.voucher.model.uservoucher;

import java.util.Collections;
import java.util.List;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

public class UserVoucherListByVoucherResponse extends ListResult<JsonObject> implements JsonMessageContent {
    public UserVoucherListByVoucherResponse(int limit, long offset) {
        super(limit, offset, 0, Collections.emptyList());
    }

    public UserVoucherListByVoucherResponse(int limit, long offset, long total, List<JsonObject> items) {
        super(limit, offset, total, items);
    }
}
