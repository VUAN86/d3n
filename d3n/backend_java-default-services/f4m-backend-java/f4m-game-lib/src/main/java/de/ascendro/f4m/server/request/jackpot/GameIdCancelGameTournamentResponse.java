package de.ascendro.f4m.server.request.jackpot;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GameIdCancelGameTournamentResponse implements JsonMessageContent {
    private String gameId;

    public GameIdCancelGameTournamentResponse(String gameId) {
        this.gameId = gameId;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }
}
