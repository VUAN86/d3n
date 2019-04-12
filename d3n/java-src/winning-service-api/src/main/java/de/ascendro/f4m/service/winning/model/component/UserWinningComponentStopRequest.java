package de.ascendro.f4m.service.winning.model.component;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserWinningComponentStopRequest implements JsonMessageContent {

	private String userWinningComponentId;

	public UserWinningComponentStopRequest() {
		// Initialize empty object
	}

	public UserWinningComponentStopRequest(String userWinningComponentId) {
		this.userWinningComponentId = userWinningComponentId;
	}

	public String getUserWinningComponentId() {
		return userWinningComponentId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserWinningComponentStopRequest [");
		builder.append("userWinningComponentId=").append(userWinningComponentId);
		builder.append("]");
		return builder.toString();
	}

}
