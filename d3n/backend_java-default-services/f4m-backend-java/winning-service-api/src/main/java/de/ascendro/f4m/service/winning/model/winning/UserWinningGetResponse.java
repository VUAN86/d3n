package de.ascendro.f4m.service.winning.model.winning;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.winning.model.UserWinning;

public class UserWinningGetResponse implements JsonMessageContent {

	private JsonObject userWinning;

	public UserWinningGetResponse() {
		// Initialize empty object
	}

	public UserWinningGetResponse(UserWinning userWinning) {
		this.userWinning = userWinning == null ? null : userWinning.getJsonObject();
	}

	public UserWinning getUserWinning() {
		return new UserWinning(userWinning);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("UserWinningGetResponse [");
		builder.append(" userWinning=").append(userWinning.getAsString());
		builder.append("]");
		return builder.toString();
	}

}
