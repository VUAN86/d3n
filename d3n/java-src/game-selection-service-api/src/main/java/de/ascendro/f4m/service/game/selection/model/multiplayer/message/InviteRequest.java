package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

import com.google.gson.annotations.SerializedName;

import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerGameParameters;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonRequiredNullable;

public abstract class InviteRequest implements JsonMessageContent {

	// when inviting for existing MGI
	@JsonRequiredNullable
	private String multiplayerGameInstanceId;

	// when inviting for new configuration
	@JsonRequiredNullable
	@SerializedName(value = "customGameConfig")
	private MultiplayerGameParameters multiplayerGameParameters;

	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}

	public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
		this.multiplayerGameInstanceId = multiplayerGameInstanceId;
	}

	public MultiplayerGameParameters getMultiplayerGameParameters() {
		return multiplayerGameParameters;
	}

	public void setMultiplayerGameParameters(MultiplayerGameParameters multiplayerGameParameters) {
		this.multiplayerGameParameters = multiplayerGameParameters;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InviteRequest");
		builder.append(" [multiplayerGameInstanceId=").append(multiplayerGameInstanceId);
		builder.append(", multiplayerGameParameters=").append(multiplayerGameParameters);
		builder.append("]");

		return builder.toString();
	}

}
