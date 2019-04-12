package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonRequiredNullable;

public abstract class InviteResponse implements JsonMessageContent {

	@JsonRequiredNullable
	private String gameInstanceId;
	private String multiplayerGameInstanceId;
	
	public InviteResponse() {
		// Empty invite response
	}
	
	public InviteResponse(String gameInstanceId, String multiplayerGameInstanceId) {
		this.gameInstanceId = gameInstanceId;
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
	}

	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
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
		builder.append("InviteResponse");
		builder.append(" [gameInstanceId=").append(gameInstanceId);
		builder.append(", multiplayerGameInstanceId=").append(multiplayerGameInstanceId);
		builder.append("]");

		return builder.toString();
	}

}
