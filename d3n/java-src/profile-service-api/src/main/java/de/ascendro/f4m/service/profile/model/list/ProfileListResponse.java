package de.ascendro.f4m.service.profile.model.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

/** Response for getting profile list. */
public class ProfileListResponse extends ListResult<JsonObject> implements JsonMessageContent {

	public ProfileListResponse(int limit, long offset) {
		super(limit, offset, 0, Collections.emptyList());
	}
	
	public ProfileListResponse(int limit, long offset, long total) {
		super(limit, offset, total, new ArrayList<>());
	}
	
	public ProfileListResponse(int limit, long offset, long total, List<JsonObject> items) {
		super(limit, offset, total, items);
	}
	
}
