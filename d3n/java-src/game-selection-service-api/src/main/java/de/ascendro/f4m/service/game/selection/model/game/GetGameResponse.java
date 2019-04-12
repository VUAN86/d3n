package de.ascendro.f4m.service.game.selection.model.game;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Content of getGame response
 * 
 */
public class GetGameResponse implements JsonMessageContent {

	private Game game;

	public GetGameResponse() {
		// Empty constructor of getGame response
	}

	public GetGameResponse(Game game) {
		this.game = game;
	}

	public Game getGame() {
		return game;
	}

	public void setGame(Game game) {
		this.game = game;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetGameResponse [game=");
		builder.append(game);
		builder.append("]");
		return builder.toString();
	}

}
