package de.ascendro.f4m.service.game.selection.model.notification;

import de.ascendro.f4m.service.usermessage.model.WebsocketMessageType;
import de.ascendro.f4m.service.usermessage.notification.MobilePushJsonNotification;

public abstract class GameStartNotification extends MobilePushJsonNotification {

	protected String gameInstanceId;
	protected String mgiId;
	protected String gameId;
	protected long millisToPlayDateTime;

	@Override
	public WebsocketMessageType getType() {
		return null;
	}

	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	public String getMgiId() {
		return mgiId;
	}

	public void setMgiId(String mgiId) {
		this.mgiId = mgiId;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public long getMillisToPlayDateTime() {
		return millisToPlayDateTime;
	}

	public void setMillisToPlayDateTime(long millisToPlayDateTime) {
		this.millisToPlayDateTime = millisToPlayDateTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((gameId == null) ? 0 : gameId.hashCode());
		result = prime * result + ((gameInstanceId == null) ? 0 : gameInstanceId.hashCode());
		result = prime * result + ((mgiId == null) ? 0 : mgiId.hashCode());
		result = prime * result + (int) (millisToPlayDateTime ^ (millisToPlayDateTime >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		GameStartNotification other = (GameStartNotification) obj;
		if (gameId == null) {
			if (other.gameId != null)
				return false;
		} else if (!gameId.equals(other.gameId))
			return false;
		if (gameInstanceId == null) {
			if (other.gameInstanceId != null)
				return false;
		} else if (!gameInstanceId.equals(other.gameInstanceId))
			return false;
		if (mgiId == null) {
			if (other.mgiId != null)
				return false;
		} else if (!mgiId.equals(other.mgiId))
			return false;
		if (millisToPlayDateTime != other.millisToPlayDateTime)
			return false;
		return true;
	}

}
