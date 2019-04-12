package de.ascendro.f4m.service.friend.model.api;

import de.ascendro.f4m.service.friend.model.Group;

public class ApiGroup {

	private String ownerUserId;
    private String groupId;
    private String name;
    private String type;
    private String image;
    private int buddyCount;
    
	public ApiGroup(Group group) {
		ownerUserId = group.getUserId();
		groupId = group.getGroupId();
		name = group.getName();
		type = group.getType();
		image = group.getImage();
		buddyCount = group.getBuddyCount();
	}

	public String getOwnerUserId() {
		return ownerUserId;
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
	
	public int getBuddyCount() {
		return buddyCount;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("ownerUserId=").append(ownerUserId);
		builder.append(", groupId=").append(groupId);
		builder.append(", name=").append(name);
		builder.append(", type=").append(type);
		builder.append(", image=").append(image);
		builder.append(", buddyCount=").append(buddyCount);
		builder.append("]");
		return builder.toString();
	}
    
}
