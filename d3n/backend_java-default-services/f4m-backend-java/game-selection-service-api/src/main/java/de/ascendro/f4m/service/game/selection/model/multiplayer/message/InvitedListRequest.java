package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.OrderBy;

/**
 * Content of invitedList request
 * 
 */
public class InvitedListRequest extends FilterCriteria implements JsonMessageContent {

	/** Maximum allowed requested list limit. */
	public static final int MAX_LIST_LIMIT = 100;

	private String multiplayerGameInstanceId;

	public InvitedListRequest() {
		this(MAX_LIST_LIMIT, 0, null, null);
	}

	public InvitedListRequest(long offset) {
		this(MAX_LIST_LIMIT, offset, null, null);
	}

	public InvitedListRequest(int limit, long offset) {
		this(limit, offset, null, null);
	}

	public InvitedListRequest(long offset, List<OrderBy> orderBy) {
		this(MAX_LIST_LIMIT, offset, orderBy, null);
	}

	public InvitedListRequest(int limit, long offset, List<OrderBy> orderBy) {
		this(limit, offset, orderBy, null);
	}

	public InvitedListRequest(long offset, Map<String, String> searchBy) {
		this(MAX_LIST_LIMIT, offset, null, searchBy);
	}

	public InvitedListRequest(int limit, long offset, Map<String, String> searchBy) {
		this(limit, offset, null, searchBy);
	}

	public InvitedListRequest(int limit, long offset, List<OrderBy> orderBy, Map<String, String> searchBy) {
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

	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}

	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InvitedListRequest [");
		builder.append(" multiplayerGameInstanceId=").append(multiplayerGameInstanceId);
		builder.append(", limit=").append(getLimit());
		builder.append(", offset=").append(getOffset());
		builder.append(", orderBy=").append(getOrderBy());
		builder.append(", searchBy=").append(getSearchBy());
		builder.append("]");

		return builder.toString();
	}

}
