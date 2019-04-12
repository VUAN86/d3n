package de.ascendro.f4m.service.game.selection.model.game;

import com.google.gson.JsonArray;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Content of getGameList response
 * 
 */
public class GetGameListResponse implements JsonMessageContent {

	private JsonArray games;

	public GetGameListResponse() {
		// Empty constructor of getGameList response
	}

	public JsonArray getGames() {
		return games;
	}

	public void setGames(JsonArray games) {
		this.games = games;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetGameListResponse [games=");
		builder.append(games.toString());
		builder.append("]");

		return builder.toString();
	}

}
