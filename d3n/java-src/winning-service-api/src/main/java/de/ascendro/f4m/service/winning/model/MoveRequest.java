package de.ascendro.f4m.service.winning.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public abstract class MoveRequest implements JsonMessageContent {

	private String sourceUserId;
	private String targetUserId;

	public MoveRequest() {
		// Initialize empty request
	}

	public MoveRequest(String sourceUserId, String targetUserId) {
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
