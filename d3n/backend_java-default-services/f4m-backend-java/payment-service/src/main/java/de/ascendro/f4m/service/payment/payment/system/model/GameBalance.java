package de.ascendro.f4m.service.payment.payment.system.model;

import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.model.internal.GameState;

import java.time.LocalDateTime;
import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "game_balance", schema = "nightly_development")
public class GameBalance extends AbstractBalanceEntity {

    @Column(name = "gameId", nullable = true)
    private int gameId;

    @Column(name = "mgiId", nullable = false, length = 255)
    private String mgiId;

    @Column(name = "tenantId", nullable = false)
    private int tenantId;

    @Column(name = "appId", nullable = true)
    private int appId;

    @Enumerated(EnumType.STRING)
    @Column(name = "gameCurrency", columnDefinition = "enum('BONUS','CREDIT','MONEY')")
    private Currency currency;

    @Column(name = "startDate", columnDefinition = "CURRENT_TIMESTAMP")
    private LocalDateTime startDate = LocalDateTime.now();

    @Column(name = "endDate", nullable = true)
    private LocalDateTime endDate;

    @Column(name = "prizes", nullable = true, length = -1)
    private String prizes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "enum('OPEN','CLOSED')")
    private GameState gameState = GameState.OPEN;

    @Column(name = "description")
    private String description;

    @Version
    @Column(name ="version")
    private Integer version;

    public GameBalance() {
    }

    public GameBalance(int gameId, String mgiId, int tenantId, int appId, Currency currency) {
        this.gameId = gameId;
        this.mgiId = mgiId;
        this.tenantId = tenantId;
        this.appId = appId;
        this.currency = currency;
    }

    public GameBalance gameId(int gameId) {
        this.gameId = gameId;
        return this;
    }

    public GameBalance mgiId(String mgiId) {
        this.mgiId = mgiId;
        return this;
    }

    public GameBalance tenantId(int tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public GameBalance appId(int appId) {
        this.appId = appId;
        return this;
    }

    public GameBalance currency(Currency currency) {
        this.currency = currency;
        return this;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public String getMgiId() {
        return mgiId;
    }

    public void setMgiId(String mgiId) {
        this.mgiId = mgiId;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public int getAppId() {
        return appId;
    }

    public void setAppId(int appId) {
        this.appId = appId;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }


    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getPrizes() {
        return prizes;
    }

    public void setPrizes(String prizes) {
        this.prizes = prizes;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        GameBalance that = (GameBalance) o;
        return gameId == that.gameId &&
                tenantId == that.tenantId &&
                appId == that.appId &&
                mgiId.equals(that.mgiId) &&
                currency == that.currency &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gameId, mgiId, tenantId, appId, currency, version);
    }

    @Override
    public String toString() {
        return "GameBalance{" +
                "gameId=" + gameId +
                ", mgiId='" + mgiId + '\'' +
                ", tenantId=" + tenantId +
                ", appId=" + appId +
                ", currency=" + currency +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", prizes='" + prizes + '\'' +
                ", gameState=" + gameState +
                ", description='" + description + '\'' +
                '}';
    }
}
