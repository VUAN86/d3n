package de.ascendro.f4m.service.friend.model.api.group;


import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GroupCreateRequest implements JsonMessageContent {

	private String name;
	private String type;
	private String image;
	private String[] userIds;
	
	public GroupCreateRequest() {
		// Initialize empty object
	}

	public GroupCreateRequest(String name, String type, String image, String... userIds) {
		this.name = name;
		this.type = type;
		this.image = image;
		this.userIds = userIds;
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
	
	public String[] getUserIds() {
		return userIds;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("name=").append(name);
		builder.append("type=").append(type);
		builder.append(", image=").append(image);
		builder.append(", userIds=").append(userIds);
		builder.append("]");
		return builder.toString();
	}

}
