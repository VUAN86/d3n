package de.ascendro.f4m.service.game.selection.model.dashboard;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import de.ascendro.f4m.service.game.selection.model.game.GameType;

public class SpecialGameInfo {

	private String gameId;
	private GameType type = GameType.QUIZ24;
	private String title;
	private ZonedDateTime dateTime;
	
	private String bannerMediaId;
	
	private Long userRemainingPlayCount;
	
	private BigDecimal entryFeeAmount;
	private String entryFeeCurrency;
	private Integer entryFeeBatchSize;

	public SpecialGameInfo() {
		// initialize empty object
	}

	public String getGameId() {
		return gameId;
	}

	public void setGameId(String gameId) {
		this.gameId = gameId;
	}

	public GameType getType() {
		return type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public ZonedDateTime getDateTime() {
		return dateTime;
	}

	public void setDateTime(ZonedDateTime dateTime) {
		this.dateTime = dateTime;
	}

	public String getBannerMediaId() {
		return bannerMediaId;
	}

	public void setBannerMediaId(String bannerMediaId) {
		this.bannerMediaId = bannerMediaId;
	}

	public Integer getEntryFeeBatchSize() {
		return entryFeeBatchSize;
	}

	public void setEntryFeeBatchSize(Integer entryFeeBatchSize) {
		this.entryFeeBatchSize = entryFeeBatchSize;
	}

	public Long getUserRemainingPlayCount() {
		return userRemainingPlayCount;
	}

	public void setUserRemainingPlayCount(Long userRemainingPlayCount) {
		this.userRemainingPlayCount = userRemainingPlayCount;
	}

	public BigDecimal getEntryFeeAmount() {
		return entryFeeAmount;
	}

	public void setEntryFeeAmount(BigDecimal entryFeeAmount) {
		this.entryFeeAmount = entryFeeAmount;
	}

	public String getEntryFeeCurrency() {
		return entryFeeCurrency;
	}

	public void setEntryFeeCurrency(String entryFeeCurrency) {
		this.entryFeeCurrency = entryFeeCurrency;
	}

	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SpecialGameInfo [gameId=");
		builder.append(gameId);
		builder.append(", type=");
		builder.append(type);
		builder.append(", title=");
		builder.append(title);
		builder.append(", dateTime=");
		builder.append(dateTime);
		builder.append(", bannerMediaId=");
		builder.append(bannerMediaId);
		builder.append(", userRemainingPlayCount=");
		builder.append(userRemainingPlayCount);
		builder.append(", entryFeeAmount=");
		builder.append(entryFeeAmount);
		builder.append(", entryFeeCurrency=");
		builder.append(entryFeeCurrency);
		builder.append(", entryFeeBatchSize=");
		builder.append(entryFeeBatchSize);
		builder.append("]");
		return builder.toString();
	}

}
