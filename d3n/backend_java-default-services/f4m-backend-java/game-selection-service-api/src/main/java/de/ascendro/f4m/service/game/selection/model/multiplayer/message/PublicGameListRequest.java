package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

import de.ascendro.f4m.service.game.selection.model.multiplayer.PublicGameFilter;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Content of publicGameList request
 */
public class PublicGameListRequest extends PublicGameFilter implements JsonMessageContent {

	private int limit;

	public PublicGameListRequest(String appId) {
		super(appId);
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PublicGameListRequest [limit=");
		builder.append(limit);
		builder.append(", ");
		builder.append(super.toString());
		builder.append("]");
		return builder.toString();
	}

}
