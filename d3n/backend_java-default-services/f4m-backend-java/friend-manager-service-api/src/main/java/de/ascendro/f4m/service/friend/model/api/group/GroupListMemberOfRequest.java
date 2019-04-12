package de.ascendro.f4m.service.friend.model.api.group;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GroupListMemberOfRequest extends FilterCriteria implements JsonMessageContent {

	/** Maximum allowed requested list limit. */
	public static final int MAX_LIST_LIMIT = 100;

	private Boolean blocked;
	
	public GroupListMemberOfRequest(Boolean blocked) {
		this.blocked = blocked;
	}

	public Boolean getBlocked() {
		return blocked;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("blocked=").append(blocked);
		builder.append(", limit=").append(getLimit());
		builder.append(", offset=").append(getOffset());
		builder.append("]");
		return builder.toString();
	}

}
