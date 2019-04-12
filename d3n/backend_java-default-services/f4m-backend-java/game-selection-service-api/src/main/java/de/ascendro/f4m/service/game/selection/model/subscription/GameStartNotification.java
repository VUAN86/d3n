package de.ascendro.f4m.service.game.selection.model.subscription;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GameStartNotification implements JsonMessageContent {

	private String multiplayerGameInstanceId;

	public GameStartNotification(String rsvpId) {
		this.multiplayerGameInstanceId = rsvpId;
	}

	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}

	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("GameStartNotification");
		builder.append(" [multiplayerGameInstanceId=").append(multiplayerGameInstanceId);
		builder.append("]");
		return builder.toString();
	}

}
