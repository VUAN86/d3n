package de.ascendro.f4m.service.tombola.model.get;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class TombolaListRequest extends FilterCriteria implements JsonMessageContent {

	public static final int MAX_LIST_LIMIT = 100;

	public TombolaListRequest() {
		this(MAX_LIST_LIMIT, 0);
	}

	public TombolaListRequest(int limit, long offset) {
		setLimit(limit);
		setOffset(offset);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TombolaListRequest [");
		builder.append("limit=").append(Integer.toString(getLimit()));
		builder.append(", offset=").append(Long.toString(getOffset()));
		builder.append(", orderBy=").append(getOrderBy());
		builder.append(", searchBy=").append(getSearchBy());
		builder.append("]");
		return builder.toString();
	}
}
