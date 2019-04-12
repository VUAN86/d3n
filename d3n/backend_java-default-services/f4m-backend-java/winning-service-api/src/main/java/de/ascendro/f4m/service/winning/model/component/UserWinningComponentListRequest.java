package de.ascendro.f4m.service.winning.model.component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.OrderBy;

public class UserWinningComponentListRequest extends FilterCriteria implements JsonMessageContent {

	/** Maximum allowed requested list limit. */
	public static final int MAX_LIST_LIMIT = 100;
	
	public UserWinningComponentListRequest() {
		this(MAX_LIST_LIMIT, 0, null, null);
	}

	public UserWinningComponentListRequest(long offset) {
		this(MAX_LIST_LIMIT, offset, null, null);
	}

	public UserWinningComponentListRequest(int limit, long offset) {
		this(limit, offset, null, null);
	}

	public UserWinningComponentListRequest(long offset, List<OrderBy> orderBy) {
		this(MAX_LIST_LIMIT, offset, orderBy, null);
	}
	
	public UserWinningComponentListRequest(int limit, long offset, List<OrderBy> orderBy) {
		this(limit, offset, orderBy, null);
	}
	
	public UserWinningComponentListRequest(long offset, Map<String, String> searchBy) {
		this(MAX_LIST_LIMIT, offset, null, searchBy);
	}
	
	public UserWinningComponentListRequest(int limit, long offset, Map<String, String> searchBy) {
		this(limit, offset, null, searchBy);
	}
	
	public UserWinningComponentListRequest(int limit, long offset, List<OrderBy> orderBy, Map<String, String> searchBy) {
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
	
}
