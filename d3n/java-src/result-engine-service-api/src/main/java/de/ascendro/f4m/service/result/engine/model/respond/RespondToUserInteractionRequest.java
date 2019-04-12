package de.ascendro.f4m.service.result.engine.model.respond;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import de.ascendro.f4m.server.result.UserInteractionType;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class RespondToUserInteractionRequest implements JsonMessageContent {

	private String gameInstanceId;
	private String userInteractionType;
	private JsonElement response;

    public RespondToUserInteractionRequest() {
    	// Initialize empty request
    }

    public RespondToUserInteractionRequest(String gameInstanceId, UserInteractionType userInteractionType, boolean response) {
    	this(gameInstanceId, userInteractionType, new JsonPrimitive(response));
    }

    public RespondToUserInteractionRequest(String gameInstanceId, UserInteractionType userInteractionType, JsonElement response) {
    	this.gameInstanceId = gameInstanceId;
        this.userInteractionType = userInteractionType.getValue();
        this.response = response;
    }

    public String getGameInstanceId() {
    	return gameInstanceId;
    }
    
    public UserInteractionType getUserInteractionType() {
    	return UserInteractionType.fromString(userInteractionType);
    }

    public JsonElement getResponse() {
    	return response;
    }
    
}
