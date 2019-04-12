package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class RespondToInvitationRequest implements JsonMessageContent {

	private String multiplayerGameInstanceId;
	private boolean accept;

	public RespondToInvitationRequest() {
		// Empty constructor
	}

	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}

	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
	}

	public boolean isAccept() {
		return accept;
	}

	public void setAccept(boolean accept) {
		this.accept = accept;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RespondToInvitationRequest [multiplayerGameInstanceId=");
		builder.append(multiplayerGameInstanceId);
		builder.append(", accept=");
		builder.append(accept);
		builder.append("]");

		return builder.toString();
	}

}
