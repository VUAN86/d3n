package de.ascendro.f4m.service.result.engine.model.get;

import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.result.engine.model.CompletedGameHistoryInfo;

/** Response for getting game history list. */
public class CompletedGameListResponse extends ListResult<CompletedGameHistoryInfo> implements JsonMessageContent {

	public CompletedGameListResponse(int limit, long offset) {
		super(limit, offset, 0, Collections.emptyList());
	}

	public CompletedGameListResponse(int limit, long offset, long total, List<CompletedGameHistoryInfo> items) {
		super(limit, offset, total, items);
	}
}
