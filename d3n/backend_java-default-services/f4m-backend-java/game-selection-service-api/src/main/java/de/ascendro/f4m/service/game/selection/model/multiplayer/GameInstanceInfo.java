package de.ascendro.f4m.service.game.selection.model.multiplayer;

public class GameInstanceInfo {
    private String gameInstanceId;
    // status contains values from GameState enum, but introduces a circular dependency if we change the type to be GameState
    private String status;

    public GameInstanceInfo(String gameInstanceId, String status) {
        this.gameInstanceId = gameInstanceId;
        this.status = status;
    }

    public String getGameInstanceId() {
        return gameInstanceId;
    }

    public void setGameInstanceId(String gameInstanceId) {
        this.gameInstanceId = gameInstanceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
