package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class JoinMultiplayerGameResponse implements JsonMessageContent {

	private String gameInstanceId;

	public JoinMultiplayerGameResponse(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JoinMultiplayerGameResponse");
		builder.append(" [gameInstanceId=").append(gameInstanceId);
		builder.append("]");

		return builder.toString();
	}

}
