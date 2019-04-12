package de.ascendro.f4m.service.result.engine.model.multiplayerGameResult;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class MultiplayerResultsBuddiesListRequest extends FilterCriteria implements JsonMessageContent {

	/** Maximum allowed requested list limit. */
	public static final int MAX_LIST_LIMIT = 100;

	private String gameInstanceId;
	private boolean amongAllHandicapRanges;

    public MultiplayerResultsBuddiesListRequest() {
    	// Initialize empty request
    }

    public MultiplayerResultsBuddiesListRequest(String gameInstanceId) {
        this.gameInstanceId = gameInstanceId;
    }

    public String getGameInstanceId() {
    	return gameInstanceId;
    }
    
    public boolean isAmongAllHandicapRanges() {
    	return amongAllHandicapRanges;
    }
    
    public void setAmongAllHandicapRanges(boolean amongAllHandicapRanges) {
    	this.amongAllHandicapRanges = amongAllHandicapRanges;
    }

}
