package de.ascendro.f4m.service.game.selection.model.game;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Content of getGame request
 * 
 */
public class GetGameRequest implements JsonMessageContent {

	private String gameId;

	public GetGameRequest() {
		// Empty constructor of getGame request
	}

	/**
	 * @param gameId
	 *            {@link String}
	 */
	public GetGameRequest(String gameId) {
		this.gameId = gameId;
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetGameRequest [gameId=");
		builder.append(gameId);
		builder.append("]");
		return builder.toString();
	}

}
