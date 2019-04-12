package de.ascendro.f4m.service.winning.model.component;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class WinningComponentListRequest extends FilterCriteria implements JsonMessageContent {

	/** Maximum allowed requested list limit. */
	public static final int MAX_LIST_LIMIT = 100;

	private String gameId;
	private String gameInstanceId;

	public WinningComponentListRequest() {
		// Initialize empty request
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public void setGameInstanceId(String gameInstanceId) {
		this.gameInstanceId = gameInstanceId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("WinningComponentListRequest [");
		builder.append("gameId=").append(gameId);
		builder.append(", gameInstanceId=").append(gameInstanceId);
		builder.append(", limit=").append(getLimit());
		builder.append(", offset=").append(getOffset());
		builder.append("]");
		return builder.toString();
	}

}
