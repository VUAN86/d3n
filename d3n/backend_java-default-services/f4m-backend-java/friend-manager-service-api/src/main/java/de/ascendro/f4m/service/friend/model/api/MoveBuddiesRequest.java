package de.ascendro.f4m.service.friend.model.api;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class MoveBuddiesRequest implements JsonMessageContent {

	private String sourceUserId;
	private String targetUserId;

	public MoveBuddiesRequest() {
		// Initialize empty request
	}

	public MoveBuddiesRequest(String sourceUserId, String targetUserId) {
		this.sourceUserId = sourceUserId;
		this.targetUserId = targetUserId;
	}
	
	public String getSourceUserId() {
		return sourceUserId;
	}

	public String getTargetUserId() {
		return targetUserId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName() + " [");
		builder.append("sourceUserId=").append(sourceUserId);
		builder.append(", targetUserId=").append(targetUserId);
		builder.append("]");
		return builder.toString();
	}

}
