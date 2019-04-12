package de.ascendro.f4m.service.friend.model.api.group;


import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GroupUpdateRequest implements JsonMessageContent {

	private String userId;
	private String tenantId;
	private String groupId;
	private String name;
	private String type;
	private String image;
	private String[] userIdsToRemove;
	
	public GroupUpdateRequest() {
		// Initialize empty object
	}

	public GroupUpdateRequest(String groupId, String name, String type, String image, String[] userIdsToRemove) {
		this(null, null, groupId, name, type, image, userIdsToRemove);
	}

	public GroupUpdateRequest(String userId, String tenantId, String groupId, String name, String type, String image, String... userIdsToRemove) {
		this.userId = userId;
		this.tenantId = tenantId;
		this.groupId = groupId;
		this.name = name;
		this.type = type;
		this.image = image;
		this.userIdsToRemove = userIdsToRemove;
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
	
	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getImage() {
		return image;
	}
	
	public String[] getUserIdsToRemove() {
		return userIdsToRemove;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("userId=").append(userId);
		builder.append(", tenantId=").append(tenantId);
		builder.append(", groupId=").append(groupId);
		builder.append(", name=").append(name);
		builder.append(", type=").append(type);
		builder.append(", image=").append(image);
		builder.append(", userIdsToRemove=").append(userIdsToRemove);
		builder.append("]");
		return builder.toString();
	}

}
