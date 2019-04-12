package de.ascendro.f4m.service.profile.model;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

public class ProfileStats  extends JsonObjectWrapper {
    public static final String GAMES_WON_PROPERTY = "gamesWon";
    public static final String GAMES_LOST_PROPERTY = "gamesLost";
    public static final String TOTAL_GAMES_PROPERTY = "totalGames";
    public static final String POINTS_WON_PROPERTY = "pointsWon";
    public static final String CREDIT_WON_PROPERTY = "creditWon";
    public static final String MONEY_WON_PROPERTY = "moneyWon";
    public static final String MONTHLY_INVITES_PROPERTY = "monthlyInvites";
    public static final String CURRENT_MONTH_PROPERTY = "currentMonth";

    public static final String TOTAL_QUICK_QUIZ_GAMES_PROPERTY = "totalQuickQuizGamesCount";
    public static final String WON_DUEL_GAMES_PROPERTY = "wonDuelGamesCount";
    public static final String LOST_DUEL_GAMES_PROPERTY = "lostDuelGamesCount";
    public static final String TOTAL_DUEL_GAMES_PROPERTY = "totalDuelGamesCount";
    public static final String WON_TOURNAMENT_GAMES_PROPERTY = "wonTournamentGamesCount";
    public static final String LOST_TOURNAMENT_GAMES_PROPERTY = "lostTournamentGamesCount";
    public static final String TOTAL_TOURNAMENT_GAMES_PROPERTY = "totalTournamentGamesCount";

    public ProfileStats() {
        // Initialize empty object
    }

    public ProfileStats(JsonObject profileJsonObject) {
        this.jsonObject = profileJsonObject;
    }

    public void setMonthlyInvites(Integer monthlyInvites){
        setProperty(MONTHLY_INVITES_PROPERTY, monthlyInvites);
    }

    public Integer getMonthlyInvites(){
        return getPropertyAsInt(MONTHLY_INVITES_PROPERTY);
    }

    public void setCurrentMonth(ZonedDateTime currentMonth){
        setProperty(CURRENT_MONTH_PROPERTY, currentMonth);
    }

    public ZonedDateTime getCurrentMonth(){
        return getPropertyAsZonedDateTime(CURRENT_MONTH_PROPERTY);
    }

    public void setGamesWon(Integer gamesWon){
        setProperty(GAMES_WON_PROPERTY, gamesWon);
    }

    public Integer getGamesWon(){
        return getPropertyAsInt(GAMES_WON_PROPERTY);
    }

    public void setGamesLost(Integer gamesLost){
        setProperty(GAMES_LOST_PROPERTY, gamesLost);
    }

    public Integer getGamesLost(){
        return getPropertyAsInt(GAMES_LOST_PROPERTY);
    }

    public void setTotalGames(Integer totalGames){
        setProperty(TOTAL_GAMES_PROPERTY, totalGames);
    }

    public Integer getTotalGames(){
        return getPropertyAsInt(TOTAL_GAMES_PROPERTY);
    }

    public void setCreditWon(Integer creditWon){
        setProperty(CREDIT_WON_PROPERTY, creditWon);
    }

    public Integer getCreditWon(){
        return getPropertyAsInt(CREDIT_WON_PROPERTY);
    }

    public void setPointsWon(Integer pointsWon){
        setProperty(POINTS_WON_PROPERTY, pointsWon);
    }

    public Integer getPointsWon(){
        return getPropertyAsInt(POINTS_WON_PROPERTY);
    }

    public void setMoneyWon(Double moneyWon){
        setProperty(MONEY_WON_PROPERTY, moneyWon);
    }

    public Double getMoneyWon(){
        return getPropertyAsDouble(MONEY_WON_PROPERTY);
    }

    public void setTotalQuickQuizGamesCount(Integer totalQuickQuizGamesCount){
        setProperty(TOTAL_QUICK_QUIZ_GAMES_PROPERTY, totalQuickQuizGamesCount);
    }

    public Integer getTotalQuickQuizGamesCount(){
        return getPropertyAsInt(TOTAL_QUICK_QUIZ_GAMES_PROPERTY);
    }

    public void setWonDuelGamesCount(Integer wonDuelGamesCount){
        setProperty(WON_DUEL_GAMES_PROPERTY, wonDuelGamesCount);
    }

    public Integer getWonDuelGamesCount(){
        return getPropertyAsInt(WON_DUEL_GAMES_PROPERTY);
    }

    public void setLostDuelGamesCount(Integer lostDuelGamesCount){
        setProperty(LOST_DUEL_GAMES_PROPERTY, lostDuelGamesCount);
    }

    public Integer getLostDuelGamesCount(){
        return getPropertyAsInt(LOST_DUEL_GAMES_PROPERTY);
    }

    public void setTotalDuelGamesCount(Integer totalDuelGamesCount){
        setProperty(TOTAL_DUEL_GAMES_PROPERTY, totalDuelGamesCount);
    }

    public Integer getTotalDuelGamesCount(){
        return getPropertyAsInt(TOTAL_DUEL_GAMES_PROPERTY);
    }

    public void setWonTournamentGamesCount(Integer wonTournamentGamesCount){
        setProperty(WON_TOURNAMENT_GAMES_PROPERTY, wonTournamentGamesCount);
    }

    public Integer geWonTournamentGamesCount(){
        return getPropertyAsInt(WON_TOURNAMENT_GAMES_PROPERTY);
    }

    public void setLostTournamentGamesCount(Integer lostTournamentGamesCount){
        setProperty(LOST_TOURNAMENT_GAMES_PROPERTY, lostTournamentGamesCount);
    }

    public Integer geLostTournamentGamesCount(){
        return getPropertyAsInt(LOST_TOURNAMENT_GAMES_PROPERTY);
    }

    public void setTotalTournamentGamesCount(Integer totalTournamentGamesCount){
        setProperty(TOTAL_TOURNAMENT_GAMES_PROPERTY, totalTournamentGamesCount);
    }

    public Integer geTotalTournamentGamesCount(){
        return getPropertyAsInt(TOTAL_TOURNAMENT_GAMES_PROPERTY);
    }

    public void add(ProfileStats profileStats) {
        setCreditWon(getCreditWon() + profileStats.getCreditWon());
        setPointsWon(getPointsWon() + profileStats.getPointsWon());
        setMoneyWon(getMoneyWon() + profileStats.getMoneyWon());
        setGamesWon(getGamesWon() + profileStats.getGamesWon());
        setGamesLost(getGamesLost() + profileStats.getGamesLost());
        setTotalGames(getTotalGames() + profileStats.getTotalGames());

        setTotalQuickQuizGamesCount(getTotalQuickQuizGamesCount() + profileStats.getTotalQuickQuizGamesCount());
        setWonDuelGamesCount(getWonDuelGamesCount() + profileStats.getWonDuelGamesCount());
        setLostDuelGamesCount(getLostDuelGamesCount() + profileStats.getLostDuelGamesCount());
        setTotalDuelGamesCount(getTotalDuelGamesCount() + profileStats.getTotalDuelGamesCount());
        setWonTournamentGamesCount(geWonTournamentGamesCount() + profileStats.geWonTournamentGamesCount());
        setLostTournamentGamesCount(geLostTournamentGamesCount() + profileStats.geLostTournamentGamesCount());
        setTotalTournamentGamesCount(geTotalTournamentGamesCount() + profileStats.geTotalTournamentGamesCount());

        //check month change and monthly invites
        if (profileStats.getMonthlyInvites()>0) {
            if (getCurrentMonth() == null || getCurrentMonth().get(ChronoField.PROLEPTIC_MONTH) < profileStats.getCurrentMonth().get(ChronoField.PROLEPTIC_MONTH)) {
                setCurrentMonth(profileStats.getCurrentMonth());
                setMonthlyInvites(profileStats.getMonthlyInvites());
            } else if (getCurrentMonth().get(ChronoField.PROLEPTIC_MONTH) == profileStats.getCurrentMonth().get(ChronoField.PROLEPTIC_MONTH)) {
                setMonthlyInvites(getMonthlyInvites() + profileStats.getMonthlyInvites());
            }
        }
    }
}
