package de.ascendro.f4m.service.voucher.model.uservoucher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

public class UserVoucherListResponse extends ListResult<JsonObject> implements JsonMessageContent {
	
	public UserVoucherListResponse(int limit, long offset) {
		super(limit, offset, 0, Collections.emptyList());
	}
	
	public UserVoucherListResponse(int limit, long offset, long total) {
		super(limit, offset, total, new ArrayList<>());
	}
	
	public UserVoucherListResponse(int limit, long offset, long total, List<JsonObject> items) {
		super(limit, offset, total, items);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserVoucherListResponse [");
		builder.append("items=").append(getItems());
		builder.append("]");
		return builder.toString();
	}


}