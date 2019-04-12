package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

import com.google.gson.annotations.SerializedName;

import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerGameParameters;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Content of createPublicGame request
 * 
 */
public class CreatePublicGameRequest implements JsonMessageContent {

	@SerializedName(value = "customGameConfig")
	private MultiplayerGameParameters multiplayerGameParameters;

	public MultiplayerGameParameters getMultiplayerGameParameters() {
		return multiplayerGameParameters;
	}

	public void setMultiplayerGameParameters(MultiplayerGameParameters multiplayerGameParameters) {
		this.multiplayerGameParameters = multiplayerGameParameters;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CreatePublicGameRequest [");
		builder.append("multiplayerGameParameters=").append(multiplayerGameParameters);
		builder.append("]");

		return builder.toString();
	}

}
