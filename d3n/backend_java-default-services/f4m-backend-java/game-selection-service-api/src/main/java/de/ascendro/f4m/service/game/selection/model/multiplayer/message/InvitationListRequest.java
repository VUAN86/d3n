package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.ascendro.f4m.service.game.selection.model.multiplayer.CreatedBy;
import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.OrderBy;

/**
 * Content of invitationList request
 * 
 */
public class InvitationListRequest extends FilterCriteria implements JsonMessageContent {

	/** Maximum allowed requested list limit. */
	public static final int MAX_LIST_LIMIT = 100;
	
	/** @deprecated use statuses instead */
	@Deprecated
	private String status;
	private List<String> states;
	private CreatedBy createdBy;
	private Boolean isPending;
	private Boolean includeOpponents;

	public InvitationListRequest() {
		this(MAX_LIST_LIMIT, 0, null, null);
	}

	public InvitationListRequest(long offset) {
		this(MAX_LIST_LIMIT, offset, null, null);
	}

	public InvitationListRequest(int limit, long offset) {
		this(limit, offset, null, null);
	}

	public InvitationListRequest(long offset, List<OrderBy> orderBy) {
		this(MAX_LIST_LIMIT, offset, orderBy, null);
	}

	public InvitationListRequest(int limit, long offset, List<OrderBy> orderBy) {
		this(limit, offset, orderBy, null);
	}

	public InvitationListRequest(long offset, Map<String, String> searchBy) {
		this(MAX_LIST_LIMIT, offset, null, searchBy);
	}

	public InvitationListRequest(int limit, long offset, Map<String, String> searchBy) {
		this(limit, offset, null, searchBy);
	}

	public InvitationListRequest(int limit, long offset, List<OrderBy> orderBy, Map<String, String> searchBy) {
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

	@Deprecated
	public String getStatus() {
		return status;
	}

	@Deprecated
	public void setStatus(String status) {
		this.status = status;
	}

	public List<String> getStates() {
		// TODO: set states in schema as required when deprecated status is removed
		if (status != null) {
			addState(status);
		}
		return states;
	}

	public void setStates(List<String> states) {
		this.states = states;
	}
	
	public void addState(String state) {
		if (states == null) {
			states = new ArrayList<>(1);
		}
		states.add(state);
	}

	public CreatedBy getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(CreatedBy createdBy) {
		this.createdBy = createdBy;
	}

	public Boolean getIsPending() {
		return isPending;
	}

	public void setIsPending(Boolean isPending) {
		this.isPending = isPending;
	}

	public Boolean getIncludeOpponents() {
		return includeOpponents;
	}

	public void setIncludeOpponents(Boolean includeOpponents) {
		this.includeOpponents = includeOpponents;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InvitationListRequest [status=");
		builder.append(status);
		builder.append(", states=");
		builder.append(states);
		builder.append(", createdBy=");
		builder.append(createdBy);
		builder.append(", isPending=");
		builder.append(isPending);
		builder.append(", includeOpponents=");
		builder.append(includeOpponents);
		builder.append("]");
		return builder.toString();
	}

}
