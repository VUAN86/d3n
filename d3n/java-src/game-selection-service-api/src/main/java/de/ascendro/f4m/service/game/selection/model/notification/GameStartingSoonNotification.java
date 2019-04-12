package de.ascendro.f4m.service.game.selection.model.notification;

import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;

public class GameStartingSoonNotification extends GameStartNotification {

	public GameStartingSoonNotification(String gameInstanceId, String mgiId, String gameId) {
		this.gameInstanceId = gameInstanceId;
		this.mgiId = mgiId;
		this.gameId = gameId;
	}

	@Override
	public WebsocketMessageType getType() {
		return WebsocketMessageType.GAME_STARTING_SOON_NOTIFICATION;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GameStartingSoonNotification [gameInstanceId=");
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
