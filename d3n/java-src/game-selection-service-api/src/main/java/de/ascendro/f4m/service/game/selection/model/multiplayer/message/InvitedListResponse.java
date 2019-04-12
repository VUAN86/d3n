package de.ascendro.f4m.service.game.selection.model.multiplayer.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.service.game.selection.model.multiplayer.InvitedUser;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

/**
 * Content of invitedList response
 * 
 */
public class InvitedListResponse extends ListResult<InvitedUser> implements JsonMessageContent {

	public InvitedListResponse() {
		super(0, 0, 0, Collections.emptyList());
	}

	public InvitedListResponse(int limit, long offset) {
		super(limit, offset, 0, Collections.emptyList());
	}

	public InvitedListResponse(int limit, long offset, long total) {
		super(limit, offset, total, new ArrayList<>());
	}

	public InvitedListResponse(int limit, long offset, long total, List<InvitedUser> items) {
		super(limit, offset, total, items);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InvitedListResponse [");
		builder.append("items=").append(getItems());
		builder.append("]");

		return builder.toString();
	}

}
