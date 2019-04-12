package de.ascendro.f4m.service.game.engine.model.register;

import com.google.gson.annotations.SerializedName;

import de.ascendro.f4m.service.game.selection.model.singleplayer.SinglePlayerGameParameters;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class RegisterRequest implements JsonMessageContent {
	
	private String gameId;
	
	private String mgiId;
	
	@SerializedName(value = "singlePlayerGameConfig")
	private SinglePlayerGameParameters singlePlayerGameParameters;

	public RegisterRequest() {
	}

	public RegisterRequest(String mgiId) {
		this.mgiId = mgiId;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public String getMgiId() {
		return mgiId;
	}

	public void setMgiId(String mgiId) {
		this.mgiId = mgiId;
	}

	public SinglePlayerGameParameters getSinglePlayerGameConfig() {
		return singlePlayerGameParameters;
	}

	public void setSinglePlayerGameConfig(SinglePlayerGameParameters singlePlayerGameConfig) {
		this.singlePlayerGameParameters = singlePlayerGameConfig;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("RegisterRequest [gameId=").append(gameId);
		builder.append(", mgiId=").append(mgiId);
		builder.append(", singlePlayerGameConfig=").append(singlePlayerGameParameters);
		builder.append("]");
		return builder.toString();
	}
	
}
