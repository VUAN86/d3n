package de.ascendro.f4m.service.winning.model.component;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.winning.model.UserWinningComponent;

public class UserWinningComponentGetResponse implements JsonMessageContent {

	private ApiUserWinningComponent userWinningComponent;

	public UserWinningComponentGetResponse() {
		// Initialize empty object
	}

	public UserWinningComponentGetResponse(UserWinningComponent userWinningComponent) {
		this.userWinningComponent = new ApiUserWinningComponent(userWinningComponent);
	}

	public ApiUserWinningComponent getUserWinningComponent() {
		return userWinningComponent;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("UserWinningComponentAssignResponse [");
		builder.append("userWinningComponent=").append(userWinningComponent);
		builder.append("]");
		return builder.toString();
	}

}
