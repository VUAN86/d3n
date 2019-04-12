package de.ascendro.f4m.service.winning.model;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

public class SuperPrize extends JsonObjectWrapper {

	public static final String PROPERTY_SUPER_PRIZE_ID = "superPrizeId";
	public static final String PROPERTY_WINNING = "winning";
	public static final String PROPERTY_INSURANCE_URL = "insuranceUrl";
	public static final String PROPERTY_RANDOM_RANGE_FROM = "randomRangeFrom";
	public static final String PROPERTY_RANDOM_RANGE_TO = "randomRangeTo";
	
	public SuperPrize(String superPrizeId, int winning, String insuranceUrl, int randomRangeFrom, int randomRangeTo) {
		setSuperPrizeId(superPrizeId);
		setWinning(winning);
		setInsuranceUrl(insuranceUrl);
		setRandomRangeFrom(randomRangeFrom);
		setRandomRangeTo(randomRangeTo);
	}

	public SuperPrize(JsonObject winningJsonObject) {
		super(winningJsonObject);
	}

	public String getSuperPrizeId() {
		return getPropertyAsString(PROPERTY_SUPER_PRIZE_ID);
	}
	
	public void setSuperPrizeId(String superPrizeId) {
		setProperty(PROPERTY_SUPER_PRIZE_ID, superPrizeId);
	}
	
	public int getWinning() {
		return getPropertyAsInt(PROPERTY_WINNING);
	}
	
	public void setWinning(int winning) {
		setProperty(PROPERTY_WINNING, winning);
	}
	
	public String getInsuranceUrl() {
		return getPropertyAsString(PROPERTY_INSURANCE_URL);
	}

	public String getInsuranceUrl(String userId, int random) {
		return getInsuranceUrl()
				.replaceAll("\\{userId\\}", userId)
				.replaceAll("\\{random\\}", String.valueOf(random));
	}
	
	public void setInsuranceUrl(String insuranceUrl) {
		setProperty(PROPERTY_INSURANCE_URL, insuranceUrl);
	}

	public int getRandomRangeFrom() {
		return getPropertyAsInt(PROPERTY_RANDOM_RANGE_FROM);
	}
	
	public void setRandomRangeFrom(int randomRangeFrom) {
		setProperty(PROPERTY_RANDOM_RANGE_FROM, randomRangeFrom);
	}

	public int getRandomRangeTo() {
		return getPropertyAsInt(PROPERTY_RANDOM_RANGE_TO);
	}
	
	public void setRandomRangeTo(int randomRangeTo) {
		setProperty(PROPERTY_RANDOM_RANGE_TO, randomRangeTo);
	}

}
