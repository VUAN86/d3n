package de.ascendro.f4m.service.tombola.model.get;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.tombola.model.Tombola;

public class TombolaListResponse extends ListResult<Tombola> implements JsonMessageContent {

	public TombolaListResponse(int limit, long offset) {
		super(limit, offset, 0, Collections.emptyList());
	}

	public TombolaListResponse(int limit, long offset, long total) {
		super(limit, offset, total, new ArrayList<>());
	}

	public TombolaListResponse(int limit, long offset, long total, List<Tombola> items) {
		super(limit, offset, total, items);
	}


	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TombolaListResponse [");
		builder.append("items=").append(getItems());
		builder.append("]");
		return builder.toString();
	}

}
