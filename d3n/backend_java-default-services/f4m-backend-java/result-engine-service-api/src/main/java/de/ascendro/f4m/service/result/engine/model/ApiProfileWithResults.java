package de.ascendro.f4m.service.result.engine.model;

import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.profile.model.Profile;

import java.math.BigDecimal;

public class ApiProfileWithResults {

    private String userId;
    private Double handicap;
    private String image;
    private String name;
    private ApiProfileAddress address;
    private Integer place;
    private int correctAnswers;
    private double gamePointsWithBonus;
    private Integer handicapRangeId;
    private Integer handicapFrom;
    private Integer handicapTo;
	private Currency prizeWonCurrency;
	private BigDecimal prizeWonAmount;


	public ApiProfileWithResults(Profile profile, MultiplayerGameResult result) {
		userId = profile.getUserId();
		handicap = profile.getHandicap();
		image = profile.getImage();
		name = profile.getFullNameOrNickname();
		address = new ApiProfileAddress(profile.getAddress());
		place = result.getPlace();
		correctAnswers = result.getCorrectAnswers();
		gamePointsWithBonus = result.getGamePointsWithBonus();
		handicapRangeId = result.getHandicapRangeId();
		handicapFrom = result.getHandicapFrom();
		handicapTo = result.getHandicapTo();
		prizeWonAmount = result.getPrizeWonAmount();
		prizeWonCurrency = result.getPrizeWonCurrency();
	}

	public String getUserId() {
		return userId;
	}

	public Double getHandicap() {
		return handicap;
	}
	
	public String getImage() {
		return image;
	}

	public String getName() {
		return name;
	}

	public ApiProfileAddress getAddress() {
		return address;
	}
	
	public Integer getPlace() {
		return place;
	}

	public int getCorrectAnswers() {
		return correctAnswers;
	}
	
	public double getGamePointsWithBonus() {
		return gamePointsWithBonus;
	}
	
	public Integer getHandicapRangeId() {
		return handicapRangeId;
	}
	
	public Integer getHandicapFrom() {
		return handicapFrom;
	}
	
	public Integer getHandicapTo() {
		return handicapTo;
	}

	public Currency getPrizeWonCurrency(){
		return prizeWonCurrency;
	}

	public BigDecimal getPrizeWonAmount() {
		return prizeWonAmount;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("userId=").append(userId);
		builder.append(", handicap=").append(handicap);
		builder.append(", image=").append(image);
		builder.append(", name=").append(name);
		builder.append(", address=").append(address);
		builder.append(", place=").append(place);
		builder.append(", correctAnswers=").append(correctAnswers);
		builder.append(", gamePointsWithBonus=").append(gamePointsWithBonus);
		builder.append(", handicapRangeId=").append(handicapRangeId);
		builder.append(", handicapFrom=").append(handicapFrom);
		builder.append(", handicapTo=").append(handicapTo);
		builder.append(", prizeWonCurrency=").append(prizeWonCurrency);
		builder.append(", prizeWonAmount=").append(prizeWonAmount);
		builder.append("]");
		return builder.toString();
	}
    
}
