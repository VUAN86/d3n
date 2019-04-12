package de.ascendro.f4m.service.friend.model.api.group;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GroupUnblockRequest implements JsonMessageContent {

	private String[] groupIds;
	
	public GroupUnblockRequest() {
		// Initialize empty object
	}

	public GroupUnblockRequest(String... groupIds) {
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
