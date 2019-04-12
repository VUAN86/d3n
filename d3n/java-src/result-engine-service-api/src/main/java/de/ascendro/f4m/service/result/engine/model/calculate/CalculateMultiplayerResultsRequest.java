package de.ascendro.f4m.service.result.engine.model.calculate;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class CalculateMultiplayerResultsRequest implements JsonMessageContent {

	private String multiplayerGameInstanceId;
    private Boolean forceRecalculate;

    public CalculateMultiplayerResultsRequest() {
    	// Initialize empty request
    }

    public String getMultiplayerGameInstanceId() {
    	return multiplayerGameInstanceId;
    }
    
    public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
    	this.multiplayerGameInstanceId = multiplayerGameInstanceId;
    }
    
    public Boolean isForceRecalculate() {
    	return forceRecalculate;
    }
    
    public void setForceRecalculate(Boolean forceRecalculate) {
    	this.forceRecalculate = forceRecalculate;
    }

}
