package de.ascendro.f4m.service.game.selection.model.subscription;

public class StartGameEventNotificationContent {

	private String gameId;

	public StartGameEventNotificationContent(String gameId) {
		this.gameId = gameId;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("StartGameEventNotificationContent [gameId=");
		builder.append(gameId);
		builder.append("]");
		return builder.toString();
	}

}
