package de.ascendro.f4m.service.friend.model.api.group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import de.ascendro.f4m.service.friend.model.api.ApiProfile;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

public class GroupListPlayersResponse extends ListResult<ApiProfile> implements JsonMessageContent {
	
	public GroupListPlayersResponse(int limit, long offset) {
		super(limit, offset, 0, Collections.emptyList());
	}
	
	public GroupListPlayersResponse(int limit, long offset, long total) {
		super(limit, offset, total, new ArrayList<>());
	}
	
	public GroupListPlayersResponse(int limit, long offset, long total, List<ApiProfile> items) {
		super(limit, offset, total, items);
	}

	public List<String> getUserIds() {
		List<String> userIds;
		if (getItems() != null) {
			userIds = getItems().stream()
					.map(ApiProfile::getUserId)
					.collect(Collectors.toList());
		} else {
			userIds = Collections.emptyList();
		}
		return userIds;
	}

}
