package de.ascendro.f4m.service.result.engine.model;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;
import de.ascendro.f4m.service.payment.model.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class MultiplayerGameResult extends JsonObjectWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiplayerGameResult.class);

    public static final String PROPERTY_MULTIPLAYER_GAME_INSTANCE_ID = "mgiId";
    public static final String PROPERTY_USER_ID = "userId";
    public static final String PROPERTY_HANDICAP_RANGE_ID = "handicapRangeId";
    public static final String PROPERTY_HANDICAP_FROM = "handicapFrom";
    public static final String PROPERTY_HANDICAP_TO = "handicapTo";
    public static final String PROPERTY_PLACE = "place";
    public static final String PROPERTY_CORRECT_ANSWERS = "correctAnswers";
    public static final String PROPERTY_GAME_POINTS_WITH_BONUS = "gamePointsWithBonus";
    public static final String PROPERTY_PRIZE_WON_CURRENCY = "prizeWonCurrency";
    public static final String PROPERTY_PRIZE_WON_AMOUNT = "prizeWonAmount";

    public MultiplayerGameResult() {
    	super();
    }

    public MultiplayerGameResult(String multiplayerGameInstanceId, String userId, int handicapRangeId, int handicapFrom,
    		int handicapTo, Integer place, int correctAnswers, double gamePointsWithBonus,BigDecimal prizeWonAmount,Currency prizeWonCurrency) {
        setMultiplayerGameInstanceId(multiplayerGameInstanceId);
        setUserId(userId);
        setHandicapRangeId(handicapRangeId);
        setHandicapFrom(handicapFrom);
        setHandicapTo(handicapTo);
        setPlace(place);
        setCorrectAnswers(correctAnswers);
        setGamePointsWithBonus(gamePointsWithBonus);
        setPrizeWonAmount(prizeWonAmount);
        setPrizeWonCurrency(prizeWonCurrency);
    }

    public MultiplayerGameResult(JsonObject mgr) {
    	super(mgr);
    }

	public String getId() {
    	return getMultiplayerGameResultId(getUserId(), getMultiplayerGameInstanceId());
    }

	public static String getMultiplayerGameResultId(String userId, String multiplayerGameInstanceId) {
		return new StringBuilder(userId).append("_").append(multiplayerGameInstanceId).toString();
	}
	
	public String getMultiplayerGameInstanceId() {
    	return getPropertyAsString(PROPERTY_MULTIPLAYER_GAME_INSTANCE_ID);
    }
    
    public void setMultiplayerGameInstanceId(String multiplayerGameInstanceId) {
    	setProperty(PROPERTY_MULTIPLAYER_GAME_INSTANCE_ID, multiplayerGameInstanceId);
    }
    
	public String getUserId() {
    	return getPropertyAsString(PROPERTY_USER_ID);
    }
    
    public void setUserId(String userId) {
    	setProperty(PROPERTY_USER_ID, userId);
    }
    
    public Integer getHandicapRangeId() {
    	return getPropertyAsInteger(PROPERTY_HANDICAP_RANGE_ID);
    }
    
    public void setHandicapRangeId(int handicapRangeId) {
    	setProperty(PROPERTY_HANDICAP_RANGE_ID, handicapRangeId);
    }

    public Integer getHandicapFrom() {
    	return getPropertyAsInteger(PROPERTY_HANDICAP_FROM);
    }
    
    public void setHandicapFrom(int handicapFrom) {
    	setProperty(PROPERTY_HANDICAP_FROM, handicapFrom);
    }

    public Integer getHandicapTo() {
    	return getPropertyAsInteger(PROPERTY_HANDICAP_TO);
    }
    
    public void setHandicapTo(int handicapTo) {
    	setProperty(PROPERTY_HANDICAP_TO, handicapTo);
    }

    public Integer getPlace() {
    	return getPropertyAsInteger(PROPERTY_PLACE);
    }
    
    public void setPlace(Integer place) {
    	setProperty(PROPERTY_PLACE, place);
    }
    
    public int getCorrectAnswers() {
    	return getPropertyAsInt(PROPERTY_CORRECT_ANSWERS);
    }
    
    public void setCorrectAnswers(int correctAnswers) {
    	setProperty(PROPERTY_CORRECT_ANSWERS, correctAnswers);
    }
    
    public double getGamePointsWithBonus() {
    	return getPropertyAsDouble(PROPERTY_GAME_POINTS_WITH_BONUS);
    }
    
    public void setGamePointsWithBonus(double gamePointsWithBonus) {
    	setProperty(PROPERTY_GAME_POINTS_WITH_BONUS, gamePointsWithBonus);
    }

    public void setPrizeWonCurrency(Currency prizeWonCurrency) {
        setProperty(PROPERTY_PRIZE_WON_CURRENCY, prizeWonCurrency == null ? "" : prizeWonCurrency.toString());
    }

    public Currency getPrizeWonCurrency() {
        String currencyString = getPropertyAsString(PROPERTY_PRIZE_WON_CURRENCY);
        if(Currency.BONUS.toString().equals(currencyString)) return Currency.BONUS;
        else if(Currency.MONEY.toString().equals(currencyString)) return Currency.MONEY;
        else if(Currency.CREDIT.toString().equals(currencyString)) return Currency.CREDIT;
        else return Currency.CREDIT;
    }

    public void setPrizeWonAmount(BigDecimal prizeWonAmount){
        setProperty(PROPERTY_PRIZE_WON_AMOUNT, prizeWonAmount);
    }

    public BigDecimal getPrizeWonAmount(){
        return getPropertyAsBigDecimal(PROPERTY_PRIZE_WON_AMOUNT);
    }
}
