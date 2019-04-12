package de.ascendro.f4m.service.tombola.model.get;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.tombola.model.TombolaBuyer;

public class TombolaBuyerListResponse extends ListResult<TombolaBuyer> implements JsonMessageContent {

	public TombolaBuyerListResponse(int limit, long offset) {
		super(limit, offset, 0, Collections.emptyList());
	}

	public TombolaBuyerListResponse(int limit, long offset, long total) {
		super(limit, offset, total, new ArrayList<>());
	}

	public TombolaBuyerListResponse(int limit, long offset, long total, List<TombolaBuyer> items) {
		super(limit, offset, total, items);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TombolaBuyerListResponse [");
		builder.append("items=").append(getItems());
		builder.append("]");
		return builder.toString();
	}
}
