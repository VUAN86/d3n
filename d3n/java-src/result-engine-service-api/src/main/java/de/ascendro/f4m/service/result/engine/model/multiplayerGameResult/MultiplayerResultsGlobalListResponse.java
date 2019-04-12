package de.ascendro.f4m.service.result.engine.model.multiplayerGameResult;

import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.result.engine.model.ApiProfileWithResults;

public class MultiplayerResultsGlobalListResponse extends ListResult<ApiProfileWithResults> implements JsonMessageContent {
	private MultiplayerResultsStatus status;
	
	public MultiplayerResultsGlobalListResponse(int limit, long offset, long total, List<ApiProfileWithResults> items, MultiplayerResultsStatus status) {
		super(limit, offset, total, items);
		this.status = status;
	}

	public MultiplayerResultsStatus getStatus() {
		return status;
	}
}
