package de.ascendro.f4m.service.friend.model.api.player;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class PlayerBlockRequest implements JsonMessageContent {

	private String[] userIds;
	
	public PlayerBlockRequest() {
		// Initialize empty object
	}

	public PlayerBlockRequest(String... userIds) {
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
