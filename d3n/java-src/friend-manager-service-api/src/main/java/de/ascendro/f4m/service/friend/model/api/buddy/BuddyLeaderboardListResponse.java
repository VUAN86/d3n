package de.ascendro.f4m.service.friend.model.api.buddy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.service.friend.model.api.ApiProfile;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

public class BuddyLeaderboardListResponse extends ListResult<ApiProfile> implements JsonMessageContent {
	
	public BuddyLeaderboardListResponse(int limit, long offset) {
		super(limit, offset, 0, Collections.emptyList());
	}
	
	public BuddyLeaderboardListResponse(int limit, long offset, long total) {
		super(limit, offset, total, new ArrayList<>());
	}
	
	public BuddyLeaderboardListResponse(int limit, long offset, long total, List<ApiProfile> items) {
		super(limit, offset, total, items);
	}

}
