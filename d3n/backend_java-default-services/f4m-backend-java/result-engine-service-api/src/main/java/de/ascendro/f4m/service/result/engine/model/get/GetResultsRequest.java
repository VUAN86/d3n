package de.ascendro.f4m.service.result.engine.model.get;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GetResultsRequest implements JsonMessageContent {

    private String gameInstanceId;

    public GetResultsRequest() {
    	// Initialize empty request
    }

    public GetResultsRequest(String gameInstanceId) {
        this.gameInstanceId = gameInstanceId;
    }

    public String getGameInstanceId() {
        return gameInstanceId;
    }

    public void setGameInstanceId(String gameInstanceId) {
        this.gameInstanceId = gameInstanceId;
    }

}
