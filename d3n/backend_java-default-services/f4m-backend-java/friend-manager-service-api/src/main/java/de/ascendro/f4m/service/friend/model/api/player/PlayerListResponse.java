package de.ascendro.f4m.service.friend.model.api.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

public class PlayerListResponse extends ListResult<ApiPlayerListResult> implements JsonMessageContent {
	
	public PlayerListResponse(int limit, long offset) {
		super(limit, offset, 0, Collections.emptyList());
	}
	
	public PlayerListResponse(int limit, long offset, long total) {
		super(limit, offset, total, new ArrayList<>());
	}
	
	public PlayerListResponse(int limit, long offset, long total, List<ApiPlayerListResult> items) {
		super(limit, offset, total, items);
	}

}
