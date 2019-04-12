package de.ascendro.f4m.server.analytics.model;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.model.base.GameBaseEvent;

public class GameEndEvent extends GameBaseEvent {

    public GameEndEvent() {
    }

    public GameEndEvent(JsonObject gameEndJsonObject) {
        super(gameEndJsonObject);
    }

    @Override
	protected boolean isUserInfoRequired() {
		return false;
	}

    @Override
    public boolean isAppIdRequired() {
        return true;
    }
}
