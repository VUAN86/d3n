package de.ascendro.f4m.service.winning.model.component;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.winning.model.UserWinningComponent;

public class UserWinningComponentAssignResponse implements JsonMessageContent {

	private String answer;

	private ApiUserWinningComponent userWinningComponent;

	public UserWinningComponentAssignResponse() {
		// Initialize empty object
	}

	public UserWinningComponentAssignResponse(String answer) {
		this.answer = answer;
	}

	public UserWinningComponentAssignResponse(UserWinningComponent userWinningComponent) {
		this.userWinningComponent = new ApiUserWinningComponent(userWinningComponent);
	}

	public ApiUserWinningComponent getUserWinningComponent() {
		return userWinningComponent;
	}

	public String getReason() {
		return answer;
	}

	public void setReason(String answer) {
		this.answer = answer;
	}

	@Override
	public String toString() {
		return "UserWinningComponentAssignResponse{" + "answer='" + answer + '\'' + ", userWinningComponent=" + userWinningComponent + '}';
	}
}
