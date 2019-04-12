package de.ascendro.f4m.service.game.selection.model.notification;

import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;

public class GamePlayerReadinessNotification extends GameStartNotification {

	public GamePlayerReadinessNotification(String gameInstanceId, String mgiId, String gameId) {
		this.gameInstanceId = gameInstanceId;
		this.mgiId = mgiId;
		this.gameId = gameId;
	}

	@Override
	public WebsocketMessageType getType() {
		return WebsocketMessageType.GAME_PLAYER_READINESS_NOTIFICATION;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GamePlayerReadinessNotification [gameInstanceId=");
		builder.append(gameInstanceId);
		builder.append(", mgiId=");
		builder.append(mgiId);
		builder.append(", gameId=");
		builder.append(gameId);
		builder.append(", millisToPlayDateTime=");
		builder.append(millisToPlayDateTime);
		builder.append("]");
		return builder.toString();
	}

}
