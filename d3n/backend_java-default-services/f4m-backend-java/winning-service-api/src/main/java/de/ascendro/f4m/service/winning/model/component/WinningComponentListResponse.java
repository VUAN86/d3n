package de.ascendro.f4m.service.winning.model.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

public class WinningComponentListResponse extends ListResult<ApiWinningComponent> implements JsonMessageContent {

	public WinningComponentListResponse(int limit, long offset) {
		super(limit, offset, 0, Collections.emptyList());
	}
	
	public WinningComponentListResponse(int limit, long offset, long total) {
		super(limit, offset, total, new ArrayList<>());
	}
	
	public WinningComponentListResponse(int limit, long offset, long total, List<ApiWinningComponent> items) {
		super(limit, offset, total, items);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("WinningComponentListResponse [");
		builder.append("items=").append(getItems());
		builder.append("]");
		return builder.toString();
	}

}
