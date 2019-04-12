package de.ascendro.f4m.server.analytics.model;

import java.math.BigDecimal;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.achievement.model.AchievementRule;
import de.ascendro.f4m.server.analytics.model.base.GameBaseEvent;
import de.ascendro.f4m.server.result.GameOutcome;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.payment.model.Currency;


public class MultiplayerGameEndEvent extends GameBaseEvent {
    public static final String GAME_INSTANCE_ID = "gameInstanceId";
    public static final String OPPONENT_ID = "opponentId";
    public static final String PLACEMENT = "placement";
    public static final String JACKPOT = "jackpot";
    public static final String JACKPOT_CURRENCY = "jackpotCurrency";
    public static final String GAME_WON = "gameWon";
    public static final String GAME_LOST = "gameLost";
    public static final String GAME_DRAW = "gameDraw";

    public static final String WON_DUEL_GAME = "wonDuelGames";
    public static final String LOST_DUEL_GAME = "lostDuelGames";
    public static final String WON_TOURNAMENT_GAME = "wonTournamentGames";
    public static final String LOST_TOURNAMENT_GAME = "lostTournamentGames";

    public MultiplayerGameEndEvent() {
        //default constructor
    }

    public MultiplayerGameEndEvent(JsonObject multiplayerGameEndJsonObject) {
        super(multiplayerGameEndJsonObject);
    }

    public void setGameInstanceId(String gameInstanceId) {
        setProperty(GAME_INSTANCE_ID, gameInstanceId);
    }

    public String getGameInstanceId() {
        return getPropertyAsString(GAME_INSTANCE_ID);
    }

    public void setOpponentId(String opponentId) {
        setProperty(OPPONENT_ID, opponentId);
    }

    public String getOpponentId() {
        return getPropertyAsString(OPPONENT_ID);
    }

    public void setPlacement(Long placement) {
        setProperty(PLACEMENT, placement);
    }

    public Long getPlacement() {
        return getPropertyAsLong(PLACEMENT);
    }

    public void setJackpot(BigDecimal jackpot) {
        setProperty(JACKPOT, jackpot);
    }

    public BigDecimal getJackpot() {
        return getPropertyAsBigDecimal(JACKPOT);
    }

    public void setJackpotCurrency(String jackpotCurrency) {
        setProperty(JACKPOT_CURRENCY, jackpotCurrency);
    }

    public String getJackpotCurrency() {
        return getPropertyAsString(JACKPOT_CURRENCY);
    }

    public void setGameWon(Boolean gameWon) {
        setProperty(GAME_WON, gameWon);
    }

    public Boolean isGameWon() {
        return getPropertyAsBoolean(GAME_WON);
    }

    public void setGameLost(Boolean gameLost) {
        setProperty(GAME_LOST, gameLost);
    }

    public Boolean isGameLost() {
        return getPropertyAsBoolean(GAME_LOST);
    }

    public void setGameDraw(Boolean gameDraw) {
        setProperty(GAME_DRAW, gameDraw);
    }

    public Boolean isGameDraw() {
        return getPropertyAsBoolean(GAME_DRAW);
    }

    public void setDuelGameWon(Boolean wonDuelGame){
        setProperty(WON_DUEL_GAME, wonDuelGame);
    }

    public Boolean isDuelGameWon(){
        return getPropertyAsBoolean(WON_DUEL_GAME);
    }

    public void setDuelGameLost(Boolean lostDuelGame){
        setProperty(LOST_DUEL_GAME, lostDuelGame);
    }

    public Boolean isDuelGameLost(){
        return getPropertyAsBoolean(LOST_DUEL_GAME);
    }

    public void setTournamentGameWon(Boolean wonTournamentGame){
        setProperty(WON_TOURNAMENT_GAME, wonTournamentGame);
    }

    public Boolean isTournamentGameWon(){
        return getPropertyAsBoolean(WON_TOURNAMENT_GAME);
    }

    public void setTournamentGameLost(Boolean lostTournamentGame){
        setProperty(LOST_TOURNAMENT_GAME, lostTournamentGame);
    }

    public Boolean isTournamentGameLost(){
        return getPropertyAsBoolean(LOST_TOURNAMENT_GAME);
    }


    public void setGameOutcome(GameOutcome gameOutcome, GameType gameType) {
        if (gameOutcome.equals(GameOutcome.WINNER)) {
            setGameWon(gameType);
        } else if (gameOutcome.equals(GameOutcome.LOSER)) {
            setGameLost(gameType);
        } else if (gameOutcome.equals(GameOutcome.TIE)) {
            setGameDraw(true);
        }
    }

    private void setGameWon(GameType gameType) {
        setGameWon(true);
        if (gameType.isDuel()) {
            setDuelGameWon(true);
        } else if (gameType.isTournament()) {
            setTournamentGameWon(true);
        }
    }

    private void setGameLost(GameType gameType) {
        setGameLost(true);
        if (gameType.isDuel()) {
            setDuelGameLost(true);
        } else if (gameType.isTournament()) {
            setTournamentGameLost(true);
        }
    }

    public boolean hasCurrency(Currency currency) {
        return currency.name().equals(this.getJackpotCurrency());
    }

    @Override
    public Integer getAchievementIncrementingCounter(AchievementRule rule) {
        Integer result;
        if(rule.equals(AchievementRule.RULE_GAME_WON_GAMES)) {
            result = 1;
        } else {
            result = super.getAchievementIncrementingCounter(rule);
        }

        return result;
    }
}
