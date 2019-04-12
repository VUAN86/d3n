package de.ascendro.f4m.service.winning.model.winning;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.OrderBy;

public class UserWinningListRequest extends FilterCriteria implements JsonMessageContent {

	/** Maximum allowed requested list limit. */
	public static final int MAX_LIST_LIMIT = 100;

	public UserWinningListRequest() {
		this(MAX_LIST_LIMIT, 0, null, null);
	}

	public UserWinningListRequest(long offset) {
		this(MAX_LIST_LIMIT, offset, null, null);
	}

	public UserWinningListRequest(int limit, long offset) {
		this(limit, offset, null, null);
	}

	public UserWinningListRequest(long offset, List<OrderBy> orderBy) {
		this(MAX_LIST_LIMIT, offset, orderBy, null);
	}

	public UserWinningListRequest(int limit, long offset, List<OrderBy> orderBy) {
		this(limit, offset, orderBy, null);
	}

	public UserWinningListRequest(long offset, Map<String, String> searchBy) {
		this(MAX_LIST_LIMIT, offset, null, searchBy);
	}

	public UserWinningListRequest(int limit, long offset, Map<String, String> searchBy) {
		this(limit, offset, null, searchBy);
	}

	public UserWinningListRequest(int limit, long offset, List<OrderBy> orderBy, Map<String, String> searchBy) {
		setLimit(limit);
		setOffset(offset);
		setOrderBy(orderBy);
		if (searchBy != null) {
			if (searchBy instanceof LinkedHashMap) {
				setSearchBy((LinkedHashMap<String, String>) searchBy);
			} else {
				setSearchBy(new LinkedHashMap<>(searchBy));
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserWinningListRequest [");
		builder.append("limit=").append(getLimit());
		builder.append(", offset=").append(getOffset());
		builder.append(", orderBy=").append(getOrderBy());
		builder.append(", searchBy=").append(getSearchBy());
		builder.append("]");
		return builder.toString();
	}

}
