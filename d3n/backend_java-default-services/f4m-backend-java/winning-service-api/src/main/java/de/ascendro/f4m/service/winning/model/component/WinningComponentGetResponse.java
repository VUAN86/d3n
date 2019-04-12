package de.ascendro.f4m.service.winning.model.component;

import de.ascendro.f4m.service.game.selection.model.game.GameWinningComponentListItem;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.winning.model.WinningComponent;

public class WinningComponentGetResponse implements JsonMessageContent {

	private ApiWinningComponent winningComponent;

	public WinningComponentGetResponse() {
		// Initialize empty object
	}

	public WinningComponentGetResponse(WinningComponent winningComponent, GameWinningComponentListItem winningComponentConfiguration) {
		this.winningComponent = new ApiWinningComponent(winningComponent, winningComponentConfiguration);
	}

	public ApiWinningComponent getWinningComponent() {
		return winningComponent;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("WinningComponentGetResponse [");
		builder.append("winningComponent=").append(winningComponent);
		builder.append("]");
		return builder.toString();
	}

}
