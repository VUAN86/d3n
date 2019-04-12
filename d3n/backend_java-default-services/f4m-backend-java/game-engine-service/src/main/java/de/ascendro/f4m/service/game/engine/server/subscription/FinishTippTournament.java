package de.ascendro.f4m.service.game.engine.server.subscription;

import java.time.ZonedDateTime;

public class FinishTippTournament {

    private String gameId;
    private String tenantId;
    private String appId;
    private ZonedDateTime repetition;
    private ZonedDateTime startGameDateTime;

    public FinishTippTournament(String gameId, String tenantId, String appId, ZonedDateTime repetition, ZonedDateTime startGameDateTime) {
        this.gameId = gameId;
        this.tenantId = tenantId;
        this.appId = appId;
        this.repetition = repetition;
        this.startGameDateTime = startGameDateTime;
    }

    public FinishTippTournament(String gameId, String tenantId, String appId, ZonedDateTime repetition) {
        this.gameId = gameId;
        this.tenantId = tenantId;
        this.appId = appId;
        this.repetition = repetition;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public ZonedDateTime getRepetition() {
        return repetition;
    }

    public void setRepetition(ZonedDateTime repetition) {
        this.repetition = repetition;
    }

    public ZonedDateTime getStartGameDateTime() {
        return startGameDateTime;
    }

    public void setStartGameDateTime(ZonedDateTime startGameDateTime) {
        this.startGameDateTime = startGameDateTime;
    }

    @Override
    public String toString() {
        return "FinishTippTournament{" +
                "gameId='" + gameId + '\'' + ", " +
                "tenantId='" + tenantId + '\'' + ", " +
                "appId='" + appId + '\'' + ", " +
                "repetition=" + repetition + ", " +
                "startGameDateTime=" + startGameDateTime +
                '}';
    }
}
