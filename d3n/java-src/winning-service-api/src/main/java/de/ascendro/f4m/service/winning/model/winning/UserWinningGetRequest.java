package de.ascendro.f4m.service.winning.model.winning;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserWinningGetRequest implements JsonMessageContent {

	private String userWinningId;

	public UserWinningGetRequest() {
		// Initialize empty request
	}

	public UserWinningGetRequest(String userWinningId) {
		this.userWinningId = userWinningId;
	}
	
	public String getUserWinningId() {
		return userWinningId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserWinningGetRequest [");
		builder.append(" userWinningId=").append(userWinningId);
		builder.append("]");
		return builder.toString();
	}

}
