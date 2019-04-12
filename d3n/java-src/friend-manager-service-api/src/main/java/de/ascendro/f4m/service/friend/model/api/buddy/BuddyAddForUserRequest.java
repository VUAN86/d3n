package de.ascendro.f4m.service.friend.model.api.buddy;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class BuddyAddForUserRequest implements JsonMessageContent {

	private String userId;
	private String[] userIds;
	private Boolean favorite;
	
	public BuddyAddForUserRequest() {
		// Initialize empty object
	}

	public BuddyAddForUserRequest(String userId, Boolean favorite, String... userIds) {
		this.userId = userId;
		this.userIds = userIds;
		this.favorite = favorite;
	}
	
	public String getUserId() {
		return userId;
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
		builder.append(", userId=").append(userId);
		builder.append(", userIds=").append(userIds);
		builder.append("]");
		return builder.toString();
	}

}
