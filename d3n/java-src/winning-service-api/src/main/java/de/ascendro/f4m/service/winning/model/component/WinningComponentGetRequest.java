package de.ascendro.f4m.service.winning.model.component;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class WinningComponentGetRequest implements JsonMessageContent {

	private String winningComponentId;
	private String gameId;
	private String gameInstanceId;

	public WinningComponentGetRequest() {
		// Initialize empty request
	}

	public String getWinningComponentId() {
		return winningComponentId;
	}

	public void setWinningComponentId(String winningComponentId) {
		this.winningComponentId = winningComponentId;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("WinningComponentGetRequest [");
		builder.append("winningComponentId=").append(winningComponentId);
		builder.append("gameId=").append(gameId);
		builder.append("gameInstanceId=").append(gameInstanceId);
		builder.append("]");
		return builder.toString();
	}

}
