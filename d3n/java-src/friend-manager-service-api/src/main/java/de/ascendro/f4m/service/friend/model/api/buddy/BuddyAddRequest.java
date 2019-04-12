package de.ascendro.f4m.service.friend.model.api.buddy;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class BuddyAddRequest implements JsonMessageContent {

	private String[] userIds;
	private Boolean favorite;
	
	public BuddyAddRequest() {
		// Initialize empty object
	}

	public BuddyAddRequest(Boolean favorite, String... userIds) {
		this.favorite = favorite;
		this.userIds = userIds;
	}
	
	public Boolean isFavorite() {
		return favorite;
	}
	
	public String[] getUserIds() {
		return userIds;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("favorite=").append(favorite);
		builder.append(", userIds=").append(userIds);
		builder.append("]");
		return builder.toString();
	}

}
