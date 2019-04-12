package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class RejectInvitationRequest implements JsonMessageContent {

	private String multiplayerGameInstanceId;

	public RejectInvitationRequest() {
		// empty constructor
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
		builder.append("RejectInvitationRequest [multiplayerGameInstanceId=");
		builder.append(multiplayerGameInstanceId);
		builder.append("]");
		return builder.toString();
	}

}
