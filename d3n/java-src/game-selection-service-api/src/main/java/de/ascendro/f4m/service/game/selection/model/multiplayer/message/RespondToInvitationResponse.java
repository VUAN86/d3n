package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonRequiredNullable;

public class RespondToInvitationResponse implements JsonMessageContent {

	@JsonRequiredNullable
	private String gameInstanceId;

	public RespondToInvitationResponse() {
		// Empty constructor
	}
	
	public RespondToInvitationResponse(String gameInstanceId) {
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
		builder.append("RespondToInvitationResponse [gameInstanceId=");
		builder.append(gameInstanceId);
		builder.append("]");

		return builder.toString();
	}

}
