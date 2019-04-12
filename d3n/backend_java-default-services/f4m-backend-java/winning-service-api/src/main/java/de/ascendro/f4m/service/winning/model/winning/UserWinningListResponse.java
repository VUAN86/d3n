package de.ascendro.f4m.service.winning.model.winning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

public class UserWinningListResponse extends ListResult<JsonObject> implements JsonMessageContent {

	public UserWinningListResponse() {
		super(0, 0, 0, Collections.emptyList());
	}

	public UserWinningListResponse(int limit, long offset) {
		super(limit, offset, 0, Collections.emptyList());
	}

	public UserWinningListResponse(int limit, long offset, long total) {
		super(limit, offset, total, new ArrayList<>());
	}

	public UserWinningListResponse(int limit, long offset, long total, List<JsonObject> items) {
		super(limit, offset, total, items);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserWinningListResponse [");
		builder.append("items=").append(getItems());
		builder.append("]");
		return builder.toString();
	}

}
