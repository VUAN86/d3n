package de.ascendro.f4m.service.result.engine.model.notification;

import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;
import de.ascendro.f4m.service.usermessage.notification.MobilePushJsonNotification;

public class GameEndNotification extends MobilePushJsonNotification {
	private GameType gameType;
	private String multiplayerGameInstanceId;
	private String gameInstanceId;

	public GameEndNotification(GameType gameType, String multiplayerGameInstanceId, String gameInstanceId) {
		this.gameType = gameType;
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
		this.gameInstanceId = gameInstanceId;
	}

	@Override
	public WebsocketMessageType getType() {
		return WebsocketMessageType.GAME_END;
	}

	public GameType getGameType() {
		return gameType;
	}

	public void setGameType(GameType gameType) {
		this.gameType = gameType;
	}

	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}

	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
	}
	
	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((gameInstanceId == null) ? 0 : gameInstanceId.hashCode());
		result = prime * result + ((gameType == null) ? 0 : gameType.hashCode());
		result = prime * result + ((multiplayerGameInstanceId == null) ? 0 : multiplayerGameInstanceId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof GameEndNotification))
			return false;
		GameEndNotification other = (GameEndNotification) obj;
		if (gameInstanceId == null) {
			if (other.gameInstanceId != null)
				return false;
		} else if (!gameInstanceId.equals(other.gameInstanceId))
			return false;
		if (gameType != other.gameType)
			return false;
		if (multiplayerGameInstanceId == null) {
			if (other.multiplayerGameInstanceId != null)
				return false;
		} else if (!multiplayerGameInstanceId.equals(other.multiplayerGameInstanceId))
			return false;
		return super.equals(obj);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder("GameEndNotification [gameType=");
		builder.append(gameType);
		builder.append(", multiplayerGameInstanceId=");
		builder.append(multiplayerGameInstanceId);
		builder.append(", gameInstanceId=");
		builder.append(gameInstanceId);
		builder.append("]");
		return builder.toString();
	}
}
