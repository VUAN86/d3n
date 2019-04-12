package de.ascendro.f4m.service.game.engine.model.register;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class RegisterResponse implements JsonMessageContent {
	private String gameInstanceId;

	public RegisterResponse(String gameInstanceId) {
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
		builder.append("RegisterResponse [gameInstanceId=");
		builder.append(gameInstanceId);
		builder.append("]");
		return builder.toString();
	}
}
