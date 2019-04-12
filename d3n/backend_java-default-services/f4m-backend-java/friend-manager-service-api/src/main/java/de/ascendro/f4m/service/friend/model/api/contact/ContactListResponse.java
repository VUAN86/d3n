package de.ascendro.f4m.service.friend.model.api.contact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

public class ContactListResponse extends ListResult<ApiContactListResult> implements JsonMessageContent {
	
	public ContactListResponse(int limit, long offset) {
		super(limit, offset, 0, Collections.emptyList());
	}
	
	public ContactListResponse(int limit, long offset, long total) {
		super(limit, offset, total, new ArrayList<>());
	}
	
	public ContactListResponse(int limit, long offset, long total, List<ApiContactListResult> items) {
		super(limit, offset, total, items);
	}

}
