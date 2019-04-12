package de.ascendro.f4m.service.winning.model.component;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserWinningComponentStartRequest implements JsonMessageContent {

	private String userWinningComponentId;

	public UserWinningComponentStartRequest() {
		// Initialize empty object
	}

	public UserWinningComponentStartRequest(String userWinningComponentId) {
		this.userWinningComponentId = userWinningComponentId;
	}

	public String getUserWinningComponentId() {
		return userWinningComponentId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserWinningComponentStartRequest [");
		builder.append("userWinningComponentId=").append(userWinningComponentId);
		builder.append("]");
		return builder.toString();
	}

}
