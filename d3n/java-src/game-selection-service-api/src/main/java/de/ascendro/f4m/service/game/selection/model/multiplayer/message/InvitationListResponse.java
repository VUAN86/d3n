package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.service.game.selection.model.multiplayer.Invitation;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

/**
 * Content of invitationList response
 * 
 */
public class InvitationListResponse extends ListResult<Invitation> implements JsonMessageContent {

	public InvitationListResponse() {
		super(0, 0, 0, Collections.emptyList());
	}

	public InvitationListResponse(int limit, long offset, long total, List<Invitation> items) {
		super(limit, offset, total, items);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InvitationListResponse [");
		builder.append("items=").append(getItems());
		builder.append("]");

		return builder.toString();
	}

}
