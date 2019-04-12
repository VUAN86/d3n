package de.ascendro.f4m.service.voucher.model.voucher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

public class VoucherListResponse extends ListResult<JsonObject> implements JsonMessageContent {

	public VoucherListResponse(int limit, long offset) {
		super(limit, offset, 0, Collections.emptyList());
	}
	
	public VoucherListResponse(int limit, long offset, long total) {
		super(limit, offset, total, new ArrayList<>());
	}
	
	public VoucherListResponse(int limit, long offset, long total, List<JsonObject> items) {
		super(limit, offset, total, items);
	}


	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("VoucherListResponse [");
		builder.append("items=").append(getItems());
		builder.append("]");
		return builder.toString();
	}

}
