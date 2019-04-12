package de.ascendro.f4m.service.result.engine.model.get;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GetMultiplayerResultsRequest implements JsonMessageContent {

	private String gameInstanceId;

    public GetMultiplayerResultsRequest() {
    	// Initialize empty request
    }

    public GetMultiplayerResultsRequest(String gameInstanceId) {
        this.gameInstanceId = gameInstanceId;
    }

    public String getGameInstanceId() {
    	return gameInstanceId;
    }
    
}
