package de.ascendro.f4m.service.game.engine.model.cancelTournament;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class CancelTournamentGameResponse implements JsonMessageContent {
    private String gameId;
    private boolean isCancelTournamentGame;

    public CancelTournamentGameResponse(String gameId, boolean isCancelTournamentGame) {
        this.gameId = gameId;
        this.isCancelTournamentGame = isCancelTournamentGame;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public boolean isCancelTournamentGame() {
        return isCancelTournamentGame;
    }

    public void setCancelTournamentGame(boolean cancelTournamentGame) {
        this.isCancelTournamentGame = cancelTournamentGame;
    }

    @Override
    public String toString() {
        return "CancelTournamentGameResponse{" +
                "gameId='" + gameId + '\'' +
                ", isCancelTournamentGame=" + isCancelTournamentGame +
                '}';
    }
}
