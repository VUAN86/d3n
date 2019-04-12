package de.ascendro.f4m.service.friend.model.api.group;


import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GroupDeleteRequest implements JsonMessageContent {

	private String groupId;
	
	public GroupDeleteRequest() {
		// Initialize empty object
	}

	public GroupDeleteRequest(String groupId) {
		this.groupId = groupId;
	}

	public String getGroupId() {
		return groupId;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("groupId=").append(groupId);
		builder.append("]");
		return builder.toString();
	}

}
