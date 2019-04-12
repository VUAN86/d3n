package de.ascendro.f4m.service.game.selection.model.multiplayer;

import java.math.BigDecimal;
import java.util.Arrays;

import com.google.gson.annotations.SerializedName;

import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.payment.model.Currency;

public class PublicGameFilter {

	private String appId;
	private GameType gameType;
	private String[] playingRegions;
	private String[] poolIds;
	private Integer numberOfQuestions;
	@SerializedName(value = "entryFeeAmountFrom")
	private BigDecimal entryFeeFrom;
	@SerializedName(value = "entryFeeAmountTo")
	private BigDecimal entryFeeTo;
	private Currency entryFeeCurrency;
	private String notByUserId;
	private String searchKeywords;

	public PublicGameFilter(String appId) {
		this.appId = appId;
	}

	public boolean isWithoutEntryFee() {
		return entryFeeFrom.compareTo(BigDecimal.ZERO) == 0 && entryFeeTo.compareTo(BigDecimal.ZERO) == 0;
	}

	public void setDefaultGameTypeIfNotSet() {
		if (gameType == null) {
			gameType = GameType.DUEL;
		}
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public GameType getGameType() {
		return gameType;
	}

	public void setGameType(GameType gameType) {
		this.gameType = gameType;
	}

	public String[] getPlayingRegions() {
		return playingRegions;
	}

	public void setPlayingRegions(String[] playingRegions) {
		this.playingRegions = playingRegions;
	}

	public String[] getPoolIds() {
		return poolIds;
	}

	public void setPoolIds(String[] poolIds) {
		this.poolIds = poolIds;
	}

	public Integer getNumberOfQuestions() {
		return numberOfQuestions;
	}

	public void setNumberOfQuestions(Integer numberOfQuestions) {
		this.numberOfQuestions = numberOfQuestions;
	}

	public BigDecimal getEntryFeeFrom() {
		return entryFeeFrom;
	}

	public void setEntryFeeFrom(BigDecimal entryFeeFrom) {
		this.entryFeeFrom = entryFeeFrom;
	}

	public BigDecimal getEntryFeeTo() {
		return entryFeeTo;
	}

	public void setEntryFeeTo(BigDecimal entryFeeTo) {
		this.entryFeeTo = entryFeeTo;
	}

	public Currency getEntryFeeCurrency() {
		return entryFeeCurrency;
	}

	public void setEntryFeeCurrency(Currency entryFeeCurrency) {
		this.entryFeeCurrency = entryFeeCurrency;
	}

	public String getNotByUserId() {
		return notByUserId;
	}

	public void setNotByUserId(String notByUserId) {
		this.notByUserId = notByUserId;
	}

	public String getSearchKeywords() {
		return searchKeywords;
	}

	public void setSearchKeywords(String searchKeywords) {
		this.searchKeywords = searchKeywords;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PublicGameFilter [appId=");
		builder.append(appId);
		builder.append(", gameType=");
		builder.append(gameType);
		builder.append(", playingRegions=");
		builder.append(Arrays.toString(playingRegions));
		builder.append(", poolIds=");
		builder.append(Arrays.toString(poolIds));
		builder.append(", numberOfQuestions=");
		builder.append(numberOfQuestions);
		builder.append(", entryFeeFrom=");
		builder.append(entryFeeFrom);
		builder.append(", entryFeeTo=");
		builder.append(entryFeeTo);
		builder.append(", entryFeeCurrency=");
		builder.append(entryFeeCurrency);
		builder.append(", notByUserId=");
		builder.append(notByUserId);
		builder.append(", searchKeywords=");
		builder.append(searchKeywords);
		builder.append("]");
		return builder.toString();
	}

}
