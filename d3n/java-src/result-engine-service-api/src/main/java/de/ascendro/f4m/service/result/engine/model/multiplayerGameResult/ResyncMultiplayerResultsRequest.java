package de.ascendro.f4m.service.result.engine.model.multiplayerGameResult;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class ResyncMultiplayerResultsRequest implements JsonMessageContent {

	private String multiplayerGameInstanceId;

    public ResyncMultiplayerResultsRequest() {
    	// Initialize empty request
    }

    public String getMultiplayerGameInstanceId() {
    	return multiplayerGameInstanceId;
    }
    
    public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
    	this.multiplayerGameInstanceId = multiplayerGameInstanceId;
    }
    
}
