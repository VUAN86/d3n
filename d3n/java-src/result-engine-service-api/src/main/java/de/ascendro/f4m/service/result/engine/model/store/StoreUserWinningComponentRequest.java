package de.ascendro.f4m.service.result.engine.model.store;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class StoreUserWinningComponentRequest implements JsonMessageContent {

	private String gameInstanceId;
	private String userWinningComponentId;
	private boolean paid;

    public StoreUserWinningComponentRequest() {
    	// Initialize empty request
    }

    public StoreUserWinningComponentRequest(String gameInstanceId, String userWinningComponentId, boolean paid) {
    	this.gameInstanceId = gameInstanceId;
        this.userWinningComponentId = userWinningComponentId;
        this.paid = paid;
    }

    public String getGameInstanceId() {
    	return gameInstanceId;
    }
    
    public String getUserWinningComponentId() {
    	return userWinningComponentId;
    }
    
    public boolean isPaid() {
    	return paid;
    }

}
