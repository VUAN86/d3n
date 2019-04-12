package de.ascendro.f4m.service.game.engine.model.history;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class PlayerGameHistoryRequest extends FilterCriteria implements JsonMessageContent {

    private String userId;

    public PlayerGameHistoryRequest() {
        // Initialize empty request
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "PlayerGameHistoryRequest{" +
                "userId='" + userId + '\'' +
                "} " + super.toString();
    }

}
