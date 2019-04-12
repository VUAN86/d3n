package de.ascendro.f4m.service.game.engine.model.start.game;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class StartGameRequest implements JsonMessageContent, GameInstanceRequest  {

	private String gameInstanceId;
	private String userLanguage;
	private String multiplayerGameInstanceId;

	@Override
	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	public String getUserLanguage() {
		return userLanguage;
	}

	public void setUserLanguage(String userLanguage) {
		this.userLanguage = userLanguage;
	}

	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}

	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("StartGameRequest [gameInstanceId=");
		builder.append(gameInstanceId);
		builder.append(", userLanguage=");
		builder.append(userLanguage);
		builder.append(", multiplayerGameInstanceId=");
		builder.append(multiplayerGameInstanceId != null ? multiplayerGameInstanceId : "");
		builder.append("]");
		return builder.toString();
	}
}
