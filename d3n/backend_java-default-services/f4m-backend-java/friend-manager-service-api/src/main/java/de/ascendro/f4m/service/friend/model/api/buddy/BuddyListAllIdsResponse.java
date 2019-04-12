package de.ascendro.f4m.service.friend.model.api.buddy;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class BuddyListAllIdsResponse implements JsonMessageContent {

	private String[] userIds;
	
	public BuddyListAllIdsResponse(String... userIds) {
		this.userIds = userIds;
	}
	
	public String[] getUserIds() {
		return userIds;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("userIds=").append(userIds);
		builder.append("]");
		return builder.toString();
	}

}
