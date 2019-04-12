package de.ascendro.f4m.service.game.selection.model.multiplayer;

import java.math.BigDecimal;

import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.payment.model.Currency;

public class PublicGameFilterBuilder {

	private String appId;
	private GameType gameType;
	private String[] playingRegions;
	private String[] poolIds;
	private Integer numberOfQuestions;
	private BigDecimal entryFeeFrom;
	private BigDecimal entryFeeTo;
	private Currency entryFeeCurrency;
	private String notByUserId;
	private String searchKeywords;

	private PublicGameFilterBuilder(String appId) {
		this.appId = appId;
	}

	public static PublicGameFilterBuilder create(String appId) {
		return new PublicGameFilterBuilder(appId);
	}

	public PublicGameFilter build() {
		PublicGameFilter filter = new PublicGameFilter(appId);
		filter.setGameType(gameType);
		filter.setPlayingRegions(playingRegions);
		filter.setPoolIds(poolIds);
		filter.setNumberOfQuestions(numberOfQuestions);
		filter.setEntryFeeFrom(entryFeeFrom);
		filter.setEntryFeeTo(entryFeeTo);
		filter.setEntryFeeCurrency(entryFeeCurrency);
		filter.setNotByUserId(notByUserId);
		filter.setSearchKeywords(searchKeywords);
		return filter;
	}
	
	public PublicGameFilterBuilder withAppId(String appId) {
		this.appId = appId;
		return this;
	}

	public PublicGameFilterBuilder withGameType(GameType gameType) {
		this.gameType = gameType;
		return this;
	}

	public PublicGameFilterBuilder withPlayingRegions(String... playingRegions) {
		this.playingRegions = playingRegions;
		return this;
	}

	public PublicGameFilterBuilder withPoolIds(String... poolIds) {
		this.poolIds = poolIds;
		return this;
	}

	public PublicGameFilterBuilder withNumberOfQuestions(Integer numberOfQuestions) {
		this.numberOfQuestions = numberOfQuestions;
		return this;
	}

	public PublicGameFilterBuilder withEntryFee(BigDecimal entryFeeFrom, BigDecimal entryFeeTo) {
		this.entryFeeFrom = entryFeeFrom;
		this.entryFeeTo = entryFeeTo;
		return this;
	}

	public PublicGameFilterBuilder withEntryFeeCurrency(Currency entryFeeCurrency) {
		this.entryFeeCurrency = entryFeeCurrency;
		return this;
	}

	public PublicGameFilterBuilder withNotByUserId(String notByUserId) {
		this.notByUserId = notByUserId;
		return this;
	}
	
	public PublicGameFilterBuilder withSearchKeywords(String searchKeywords) {
		this.searchKeywords = searchKeywords;
		return this;
	}

}
