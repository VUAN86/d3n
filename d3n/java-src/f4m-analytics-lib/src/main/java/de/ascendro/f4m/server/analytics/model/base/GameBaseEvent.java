package de.ascendro.f4m.server.analytics.model.base;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.exception.F4MAnalyticsFatalErrorException;

public class GameBaseEvent extends BaseEvent {
    public static final String GAME_ID_PROPERTY = "gameId";

    public GameBaseEvent() {}

    public GameBaseEvent(JsonObject baseGameJsonObject) {
        super(baseGameJsonObject);
    }

    public void setGameId(Long gameId) {
        setProperty(GAME_ID_PROPERTY, gameId);
    }

    public void setGameId(String gameId) {
        try {
            setGameId(Long.valueOf(gameId));
        } catch (NumberFormatException ex) {
            throw new F4MAnalyticsFatalErrorException("Invalid game id");
        }
    }

    public Long getGameId() {
        return getPropertyAsLong(GAME_ID_PROPERTY);
    }
}
