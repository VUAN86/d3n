package de.ascendro.f4m.service.friend.model.api.group;


import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GroupGetRequest implements JsonMessageContent {

	private String userId;
	private String tenantId;
	private String groupId;
	
	public GroupGetRequest() {
		// Initialize empty object
	}

	public GroupGetRequest(String groupId) {
		this(null, null, groupId);
	}

	public GroupGetRequest(String userId, String tenantId, String groupId) {
		this.userId = userId;
		this.tenantId = tenantId;
		this.groupId = groupId;
	}

	public String getUserId() {
		return userId;
	}
	
	public String getTenantId() {
		return tenantId;
	}
	
	public String getGroupId() {
		return groupId;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("userId=").append(userId);
		builder.append(", tenantId=").append(tenantId);
		builder.append(", groupId=").append(groupId);
		builder.append("]");
		return builder.toString();
	}

}
