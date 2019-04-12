package de.ascendro.f4m.service.friend.model.api.group;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GroupBlockRequest implements JsonMessageContent {

	private String[] groupIds;
	
	public GroupBlockRequest() {
		// Initialize empty object
	}

	public GroupBlockRequest(String... groupIds) {
		this.groupIds = groupIds;
	}
	
	public String[] getGroupIds() {
		return groupIds;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("groupIds=").append(groupIds);
		builder.append("]");
		return builder.toString();
	}

}
