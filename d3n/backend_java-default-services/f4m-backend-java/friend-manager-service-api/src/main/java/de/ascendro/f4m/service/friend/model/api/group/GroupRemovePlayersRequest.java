package de.ascendro.f4m.service.friend.model.api.group;


import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GroupRemovePlayersRequest implements JsonMessageContent {

	private String groupId;
	private String[] userIds;
	
	public GroupRemovePlayersRequest() {
		// Initialize empty object
	}

	public GroupRemovePlayersRequest(String groupId, String... userIds) {
		this.groupId = groupId;
		this.userIds = userIds;
	}

	public String getGroupId() {
		return groupId;
	}
	
	public String[] getUserIds() {
		return userIds;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("groupId=").append(groupId);
		builder.append(", userIds=").append(userIds);
		builder.append("]");
		return builder.toString();
	}

}
