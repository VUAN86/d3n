package de.ascendro.f4m.service.winning.model.component;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.winning.model.WinningOption;

public class UserWinningComponentStartResponse implements JsonMessageContent {

	private ApiWinningOptionWon winningOption;

	public UserWinningComponentStartResponse() {
		// Initialize empty object
	}

	public UserWinningComponentStartResponse(WinningOption winningOption) {
		this.winningOption = winningOption == null ? null : new ApiWinningOptionWon(winningOption);
	}

	public ApiWinningOptionWon getWinningOption() {
		return winningOption;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("UserWinningComponentStartResponse [");
		builder.append("winningOption=").append(winningOption);
		builder.append("]");
		return builder.toString();
	}

}
