package de.ascendro.f4m.service.result.engine.model.calculate;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class CalculateResultsRequest implements JsonMessageContent {

    private JsonObject gameInstance;
    private Boolean forceRecalculate;
    
    public CalculateResultsRequest() {
    	// Initialize empty request
    }

    public CalculateResultsRequest(JsonObject gameInstance, boolean forceRecalculate) {
        this.gameInstance = gameInstance;
        this.forceRecalculate = forceRecalculate;
    }

    public CalculateResultsRequest(JsonObject gameInstance) {
    	this.gameInstance = gameInstance;
    }

    public JsonObject getGameInstance() {
        return gameInstance;
    }

    public void setGameInstance(JsonObject gameInstance) {
        this.gameInstance = gameInstance;
    }
    
    public Boolean isForceRecalculate() {
    	return forceRecalculate;
    }
    
    public void setForceRecalculate(Boolean forceRecalculate) {
    	this.forceRecalculate = forceRecalculate;
    }
    
}
