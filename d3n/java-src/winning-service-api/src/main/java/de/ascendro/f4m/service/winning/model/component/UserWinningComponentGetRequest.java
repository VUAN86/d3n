package de.ascendro.f4m.service.winning.model.component;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserWinningComponentGetRequest implements JsonMessageContent {

	private String userWinningComponentId;

	public UserWinningComponentGetRequest() {
		// Initialize empty object
	}

	public UserWinningComponentGetRequest(String userWinningComponentId) {
		this.userWinningComponentId = userWinningComponentId;
	}

	public String getUserWinningComponentId() {
		return userWinningComponentId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserWinningComponentGetRequest [");
		builder.append("userWinningComponentId=").append(userWinningComponentId);
		builder.append("]");
		return builder.toString();
	}

}
