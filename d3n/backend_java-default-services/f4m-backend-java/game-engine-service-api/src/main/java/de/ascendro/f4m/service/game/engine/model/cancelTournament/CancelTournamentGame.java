package de.ascendro.f4m.service.game.engine.model.cancelTournament;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class CancelTournamentGame implements JsonMessageContent{

    public CancelTournamentGame() {
    }

    public CancelTournamentGame(String gameId) {
        this.gameId = gameId;
    }

    private String gameId;

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    @Override
    public String toString() {
        return "CancelTournamentGame{" +
                "gameId='" + gameId + '\'' +
                '}';
    }
}
