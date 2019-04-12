package de.ascendro.f4m.service.winning.model.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

public class UserWinningComponentListResponse extends ListResult<JsonObject> implements JsonMessageContent {

	public UserWinningComponentListResponse() {
		super(0, 0, 0, Collections.emptyList());
	}

	public UserWinningComponentListResponse(int limit, long offset) {
		super(limit, offset, 0, Collections.emptyList());
	}

	public UserWinningComponentListResponse(int limit, long offset, long total) {
		super(limit, offset, total, new ArrayList<>());
	}

	public UserWinningComponentListResponse(int limit, long offset, long total, List<JsonObject> items) {
		super(limit, offset, total, items);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserWinningComponentListResponse [");
		builder.append("items=").append(getItems());
		builder.append("]");
		return builder.toString();
	}

}
