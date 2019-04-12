package de.ascendro.f4m.server.analytics.model;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.achievement.model.AchievementRule;
import de.ascendro.f4m.server.analytics.exception.F4MAnalyticsFatalErrorException;
import de.ascendro.f4m.server.analytics.model.base.GameBaseEvent;
import de.ascendro.f4m.service.payment.model.Currency;

public class RewardEvent extends GameBaseEvent {

	public static final String VOUCHER_ID_PROPERTY = "voucherId";
	public static final String VOUCHER_WON_PROPERTY = "voucherWon";
	public static final String SUPER_PRIZE_WON_PROPERTY = "superPrizeWon";
	public static final String BONUS_POINTS_WON_PROPERTY = "bonusPointsWon";
	public static final String CREDIT_WON_PROPERTY = "creditWon";
	public static final String MONEY_WON_PROPERTY = "moneyWon";
	public static final String PAID_WINNING_COMPONENTS_PLAYED = "paidWinningComponentsPlayed";
	public static final String FREE_WINNING_COMPONENTS_PLAYED = "freeWinningComponentsPlayed";
	public static final String TOMBOLA_ID_PROPERTY = "tombolaId";
	public static final String GAME_INSTANCE_ID = "gameInstanceId";
	
	private static Map<AchievementRule, String> achievementRulesToProperties;
	static {
		achievementRulesToProperties = new HashMap<>();
		achievementRulesToProperties.put(AchievementRule.RULE_GAME_WON_BONUS, BONUS_POINTS_WON_PROPERTY);
		achievementRulesToProperties.put(AchievementRule.RULE_GAME_WON_CREDITS, CREDIT_WON_PROPERTY);
		achievementRulesToProperties.put(AchievementRule.RULE_GAME_WON_MONEY, MONEY_WON_PROPERTY);
		achievementRulesToProperties.put(AchievementRule.RULE_GAME_WON_VOUCHERS, VOUCHER_WON_PROPERTY);
	}

	public RewardEvent() {
		//default constructor
	}

	public RewardEvent(JsonObject rewardJsonObject) {
		super(rewardJsonObject);
	}

	@Override
	public boolean isAppIdRequired() {
		return false;
	}

	@Override
	public boolean isSessionIpRequired() {
		return false;
	}

	public void setGameInstanceId(String gameInstanceId) {
		setProperty(GAME_INSTANCE_ID, gameInstanceId);
	}

	public String getGameInstanceId() {
		return getPropertyAsString(GAME_INSTANCE_ID);
	}

	public void setVoucherId(Long voucherId) {
		setProperty(VOUCHER_ID_PROPERTY, voucherId);
	}

	public Long getVoucherId() {
		return getPropertyAsLong(VOUCHER_ID_PROPERTY);
	}

	public void setVoucherWon(Boolean voucherWon) {
		setProperty(VOUCHER_WON_PROPERTY, voucherWon);
	}

	public Boolean isVoucherWon() {
		return getPropertyAsBoolean(VOUCHER_WON_PROPERTY);
	}

	public void setSuperPrizeWon(Boolean superPrizeWon) {
		setProperty(SUPER_PRIZE_WON_PROPERTY, superPrizeWon);
	}

	public Boolean isSuperPrizeWon() {
		return getPropertyAsBoolean(SUPER_PRIZE_WON_PROPERTY);
	}

	public void setBonusPointsWon(Long bonusPointsWon) {
		setProperty(BONUS_POINTS_WON_PROPERTY, bonusPointsWon);
	}

	public Long getBonusPointsWon() {
		return getPropertyAsLong(BONUS_POINTS_WON_PROPERTY);
	}

	public void setCreditWon(Long creditWon) {
		setProperty(CREDIT_WON_PROPERTY, creditWon);
	}

	public Long getCreditWon() {
		return getPropertyAsLong(CREDIT_WON_PROPERTY);
	}

	public void setMoneyWon(BigDecimal moneyWon) {
		setProperty(MONEY_WON_PROPERTY, moneyWon);
	}

	public BigDecimal getMoneyWon() {
		return getPropertyAsBigDecimal(MONEY_WON_PROPERTY);
	}

	public void setPaidWinningComponentsPlayed(Boolean paidWinningComponentsPlayed) {
		setProperty(PAID_WINNING_COMPONENTS_PLAYED, paidWinningComponentsPlayed);
	}

	public Boolean isPaidWinningComponentsPlayed() {
		return getPropertyAsBoolean(PAID_WINNING_COMPONENTS_PLAYED);
	}

	public void setFreeWinningComponentsPlayed(Boolean freeWinningComponentsPlayed) {
		setProperty(FREE_WINNING_COMPONENTS_PLAYED, freeWinningComponentsPlayed);
	}

	public Boolean isFreeWinningComponentsPlayed() {
		return getPropertyAsBoolean(FREE_WINNING_COMPONENTS_PLAYED);
	}

	public void setTombolaId(Long tombolaId) {
		setProperty(TOMBOLA_ID_PROPERTY, tombolaId);
	}

	public Long getTombolaId() {
		return getPropertyAsLong(TOMBOLA_ID_PROPERTY);
	}

	public void setTombolaId(String tombolaId) {
		try {
			setTombolaId(Long.valueOf(tombolaId));
		} catch (NumberFormatException ex) {
			throw new F4MAnalyticsFatalErrorException("Invalid tombola id");
		}
	}

	public void setPrizeWon(BigDecimal amount, Currency currency) {
		if (Currency.CREDIT.equals(currency)) {
			setCreditWon(amount.longValue());
		} else if (Currency.BONUS.equals(currency)) {
			setBonusPointsWon(amount.longValue());
		} else if (Currency.MONEY.equals(currency)) {
			setMoneyWon(amount);
		}
	}

	public void setVoucherId(String voucherId) {
		try {
			setVoucherId(Long.valueOf(voucherId));
		} catch (NumberFormatException ex) {
			throw new F4MAnalyticsFatalErrorException("Invalid voucher id");
		}
	}

	@Override
	public Integer getAchievementIncrementingCounter(AchievementRule rule) {
		String propertyName = achievementRulesToProperties.get(rule);
		return getPropertyAsInt(propertyName);
	}
}
