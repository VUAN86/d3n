package de.ascendro.f4m.service.winning.model.component;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserWinningComponentLoadRequest implements JsonMessageContent {

	private String userWinningComponentId;

	public UserWinningComponentLoadRequest() {
		// Initialize empty object
	}

	public UserWinningComponentLoadRequest(String userWinningComponentId) {
		this.userWinningComponentId = userWinningComponentId;
	}

	public String getUserWinningComponentId() {
		return userWinningComponentId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserWinningComponentLoadRequest [");
		builder.append("userWinningComponentId=").append(userWinningComponentId);
		builder.append("]");
		return builder.toString();
	}

}
