package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

import java.util.List;

import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Content of publicGameList response
 */
public class PublicGameListResponse implements JsonMessageContent {

	private List<Invitation> items;

	public PublicGameListResponse(List<Invitation> items) {
		this.items = items;
	}

	public List<Invitation> getItems() {
		return items;
	}

	public void setItems(List<Invitation> items) {
		this.items = items;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PublicGameListResponse [items=");
		builder.append(items);
		builder.append("]");
		return builder.toString();
	}

}
