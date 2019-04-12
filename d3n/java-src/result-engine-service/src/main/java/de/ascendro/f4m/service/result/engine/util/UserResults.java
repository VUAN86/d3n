package de.ascendro.f4m.service.result.engine.util;

import java.math.BigDecimal;

import de.ascendro.f4m.server.result.GameOutcome;
import de.ascendro.f4m.server.result.Results;
import de.ascendro.f4m.service.payment.model.Currency;

public class UserResults {

	private double userHandicap;
	private boolean gameFinished;
	private int correctAnswerCount;
	private double gamePointsWithBonus;
	private int place;
	private String userId;
	private String appId;
	private String tenantId;
	private String clientIp;
	private String gameInstanceId;
	private BigDecimal entryFeePaid;
	private Currency entryFeeCurrency;
	private BigDecimal jackpotWinning;
	private Currency jackpotWinningCurrency;
	private String duelOpponentGameInstanceId;
	private GameOutcome gameOutcome;
	private boolean additionalPaymentForWinning = false;

	public UserResults(Results results) {
		userHandicap = results.getUserHandicap();
		gameFinished = results.isGameFinished();
		correctAnswerCount = results.getCorrectAnswerCount();
		gamePointsWithBonus = results.getGamePointsWithBonus();
		userId = results.getUserId();
		gameInstanceId = results.getGameInstanceId();
		entryFeePaid = results.getEntryFeePaid();
		entryFeeCurrency = results.getEntryFeeCurrency();
		appId = results.getAppId();
		tenantId = results.getTenantId();
		clientIp = results.getClientIp();
	}
	
	public double getUserHandicap() {
		return userHandicap;
	}
	
	public boolean isGameFinished() {
		return gameFinished;
	}
	
	public int getCorrectAnswerCount() {
		return correctAnswerCount;
	}

	public double getGamePointsWithBonus() {
		return gamePointsWithBonus;
	}
	
	public void setPlace(int place) {
		this.place = place;
	}
	
	public int getPlace() {
		return place;
	}

	public String getUserId() {
		return userId;
	}
	
	public String getAppId() {
		return appId;
	}
	
	public String getTenantId() {
		return tenantId;
	}
	
	public String getClientIp() {
		return clientIp;
	}
	
	public String getGameInstanceId() {
		return gameInstanceId;
	}
	
	public BigDecimal getEntryFeePaid() {
		return entryFeePaid;
	}
	
	public Currency getEntryFeeCurrency() {
		return entryFeeCurrency;
	}
	
	public BigDecimal getJackpotWinning() {
		return jackpotWinning;
	}
	
	public Currency getJackpotWinningCurrency() {
		return jackpotWinningCurrency;
	}
	
	public void setJackpotWinning(BigDecimal jackpotWinning, Currency currency) {
		this.jackpotWinning = jackpotWinning;
		this.jackpotWinningCurrency = currency;
	}

	public String getDuelOpponentGameInstanceId() {
		return duelOpponentGameInstanceId;
	}
	
	public void setDuelOpponentGameInstanceId(String duelOpponentGameInstanceId) {
		this.duelOpponentGameInstanceId = duelOpponentGameInstanceId;
	}

	public GameOutcome getGameOutcome() {
		return gameOutcome;
	}
	
	public void setGameOutcome(GameOutcome gameOutcome) {
		this.gameOutcome = gameOutcome;
	}

	public boolean isAdditionalPaymentForWinning() {
		return additionalPaymentForWinning;
	}

	public void setAdditionalPaymentForWinning(boolean additionalPaymentForWinning) {
		this.additionalPaymentForWinning = additionalPaymentForWinning;
	}

	@Override
	public String toString() {
		return "UserResults{" +
				"userHandicap=" + userHandicap +
				", gameFinished=" + gameFinished +
				", correctAnswerCount=" + correctAnswerCount +
				", gamePointsWithBonus=" + gamePointsWithBonus +
				", place=" + place +
				", userId='" + userId + '\'' +
				", appId='" + appId + '\'' +
				", tenantId='" + tenantId + '\'' +
				", clientIp='" + clientIp + '\'' +
				", gameInstanceId='" + gameInstanceId + '\'' +
				", entryFeePaid=" + entryFeePaid +
				", entryFeeCurrency=" + entryFeeCurrency +
				", jackpotWinning=" + jackpotWinning +
				", jackpotWinningCurrency=" + jackpotWinningCurrency +
				", duelOpponentGameInstanceId='" + duelOpponentGameInstanceId + '\'' +
				", gameOutcome=" + gameOutcome +
				'}';
	}
}
