package de.ascendro.f4m.service.friend.model.api.group;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GroupListPlayersRequest extends FilterCriteria implements JsonMessageContent {

	/** Maximum allowed requested list limit. */
	public static final int MAX_LIST_LIMIT = 100;

	private String groupId;
	private Boolean includeProfileInfo;
	private Boolean excludeGroupOwner;
	
	public GroupListPlayersRequest(String groupId) {
		this.groupId = groupId;
		setLimit(MAX_LIST_LIMIT);
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public boolean isIncludeProfileInfo() {
		return includeProfileInfo == null ? false : includeProfileInfo;
	}

	public void setIncludeProfileInfo(boolean includeProfileInfo) {
		this.includeProfileInfo = includeProfileInfo;
	}

	public Boolean isExcludeGroupOwner() {
		return excludeGroupOwner == null ? false : excludeGroupOwner;
	}

	public void setExcludeGroupOwner(Boolean excludeGroupOwner) {
		this.excludeGroupOwner = excludeGroupOwner;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("groupId=").append(groupId);
		builder.append(", includeProfileInfo=").append(includeProfileInfo);
        builder.append(", excludeGroupOwner=").append(excludeGroupOwner);
		builder.append(", limit=").append(getLimit());
		builder.append(", offset=").append(getOffset());
		builder.append("]");
		return builder.toString();
	}

}
