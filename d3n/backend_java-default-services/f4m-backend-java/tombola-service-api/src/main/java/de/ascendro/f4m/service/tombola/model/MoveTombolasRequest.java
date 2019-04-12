package de.ascendro.f4m.service.tombola.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class MoveTombolasRequest implements JsonMessageContent {

	private String sourceUserId;
	private String targetUserId;

	public MoveTombolasRequest() {
		// Initialize empty request
	}

	public MoveTombolasRequest(String sourceUserId, String targetUserId) {
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
