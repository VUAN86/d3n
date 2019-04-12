package de.ascendro.f4m.service.friend.model.api.group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.service.friend.model.api.ApiGroup;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

public class GroupListResponse extends ListResult<ApiGroup> implements JsonMessageContent {
	
	public GroupListResponse(int limit, long offset) {
		super(limit, offset, 0, Collections.emptyList());
	}
	
	public GroupListResponse(int limit, long offset, long total) {
		super(limit, offset, total, new ArrayList<>());
	}
	
	public GroupListResponse(int limit, long offset, long total, List<ApiGroup> items) {
		super(limit, offset, total, items);
	}

}
