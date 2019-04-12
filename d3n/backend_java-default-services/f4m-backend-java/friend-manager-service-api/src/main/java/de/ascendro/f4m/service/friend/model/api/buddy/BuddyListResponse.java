package de.ascendro.f4m.service.friend.model.api.buddy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

public class BuddyListResponse extends ListResult<ApiBuddyListResult> implements JsonMessageContent {
	
	public BuddyListResponse(int limit, long offset) {
		super(limit, offset, 0, Collections.emptyList());
	}
	
	public BuddyListResponse(int limit, long offset, long total) {
		super(limit, offset, total, new ArrayList<>());
	}
	
	public BuddyListResponse(int limit, long offset, long total, List<ApiBuddyListResult> items) {
		super(limit, offset, total, items);
	}

}
